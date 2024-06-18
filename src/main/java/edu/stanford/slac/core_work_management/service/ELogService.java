package edu.stanford.slac.core_work_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.auth.JWTHelper;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.core_work_management.api.v1.dto.NewLogEntry;
import edu.stanford.slac.core_work_management.api.v1.mapper.LogEntryMapper;
import edu.stanford.slac.core_work_management.elog_api.dto.EntryImportDTO;
import edu.stanford.slac.core_work_management.elog_api.dto.ImportEntryDTO;
import edu.stanford.slac.core_work_management.exception.ActivityNotFound;
import edu.stanford.slac.core_work_management.exception.WorkNotFound;
import edu.stanford.slac.core_work_management.model.LogEntry;
import edu.stanford.slac.core_work_management.repository.ActivityRepository;
import edu.stanford.slac.core_work_management.repository.AttachmentRepository;
import edu.stanford.slac.core_work_management.repository.LogEntryRepository;
import edu.stanford.slac.core_work_management.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;
import static edu.stanford.slac.core_work_management.config.AuthorizationStringConfig.WORK_AUTHORIZATION_TEMPLATE;

@Log4j2
@Service
@Profile("elog-support")
@RequiredArgsConstructor
public class ELogService {
    private final AuthService authService;
    private final WorkRepository workRepository;
    private final ActivityRepository activityRepository;
    private final LogEntryMapper logEntryMapper;
    private final ShopGroupService shopGroupService;
    private final LogEntryRepository logEntryRepository;
    private final AttachmentRepository attachmentRepository;
    private final KafkaTemplate<String, ImportEntryDTO> importEntryDTOKafkaTemplate;
    private final JWTHelper jwtHelper;
    private final ObjectMapper objectMapper;
    @Value("${edu.stanford.slac.core-work-management.elog-import-topic}")
    private String importEntryTopic;

    /**
     * Create a new log entry for a work plan or job execution
     *
     * @param workId The id of the work plan or job execution
     * @param entry  The log entry
     * @param files  The files to be attached to the log entry
     */
    @Transactional
    public String createNewLogEntry(String workId, NewLogEntry entry, MultipartFile[] files) {
        var foundWork = workRepository.findById(workId).orElseThrow(()-> WorkNotFound.notFoundById().workId(workId).errorCode(-1).build());
        log.info("Creating new log entry for work {}", foundWork.getWorkNumber());
        List<String> attachmentIds = new ArrayList<>();
        //store attachments
        if (files != null) {
            log.info("[logging work number {}] Storing {} attachments", foundWork.getWorkNumber(), files.length);
            for (MultipartFile file : files) {
                attachmentIds.add(
                        wrapCatch(
                                () -> attachmentRepository.addAttachment(
                                        workId,
                                        entry.title(),
                                        file.getInputStream(),
                                        file.getOriginalFilename(),
                                        file.getContentType()
                                ),
                                -1
                        )
                );
            }
        }
        log.info("[logging work number {}] Storing log entry", foundWork.getWorkNumber());
        // store log entry
        LogEntry savedLogEntry = logEntryRepository.save(
                logEntryMapper.toModel(entry, workId, attachmentIds)
        );

        // create dto for submit entry to elog system
        // fetch all authorized on work
        log.info("[logging work number {}] Fetching all authorization",foundWork.getWorkNumber());
        List<AuthorizationDTO> allWorkAuthorization = authService.findByResourceIs(WORK_AUTHORIZATION_TEMPLATE.formatted(workId));

        // check if within allWorkAuthorization there is some ower with the postfix 'shopgroup.cws.slac.stanford.edu'
        // in this case i need to fetch the shop gorup and return an array of user id
        List<String> userIdForAuthorization = allWorkAuthorization.stream().map(
                auth -> {
                    if (auth.owner().endsWith("@shopgroup.cws.slac.stanford.edu")) {
                        log.info("[logging work number {}] Fetching all user for shop group: {}", foundWork.getWorkNumber(), auth.owner());
                        var shopGroup = shopGroupService.findById(auth.owner().split("@")[0]);
                        return shopGroup.users().stream().map(member -> member.user().mail()).toList();
                    }
                    return List.of(auth.owner());
                }
        ).flatMap(List::stream).distinct().toList();
        log.info("[logging work number {}] Authorize elog reading to: {}", foundWork.getWorkNumber(), userIdForAuthorization);
        // create DTO
        log.info("[logging work number {}] Creating DTO for elog system", foundWork.getWorkNumber());
        ImportEntryDTO dto = new ImportEntryDTO()
                .entry
                        (
                                new EntryImportDTO()
                                        .title(entry.title())
                                        .text(entry.text())
                                        .loggedAt(entry.eventAt())
                                        .logbooks(List.of("cwm-logbook"))
                                        .originId("cwm:work:%s".formatted(foundWork.getWorkNumber()))

                        )
                .readerUserIds(userIdForAuthorization);
        //create kafka record
        log.info("[logging work number {}] Sending log to elog system", foundWork.getWorkNumber());
        ProducerRecord<String, ImportEntryDTO> message = new ProducerRecord<>(
                importEntryTopic,
                dto
        );
        message.headers().add("Authorization", jwtHelper.generateServiceToken().getBytes());
        // submit log to kafka topic to send to elog system
        CompletableFuture<SendResult<String, ImportEntryDTO>> future = importEntryDTOKafkaTemplate.send(message);
        future.thenAccept(result -> {
            // Success handling
            log.info(
                    "Log sent to elog system using topic {} at partition {} with offset {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        }).handle((result, throwable) -> {
            if (throwable != null) {
                final String errorMessage = "Failed to send message to topic %s : %s".formatted(importEntryTopic, throwable.getMessage());
                // Log the exception
                log.error(errorMessage);
                throw ControllerLogicException
                        .builder()
                        .errorCode(-2)
                        .errorMessage(errorMessage)
                        .errorDomain("LogService::createNewLogEntry")
                        .build();
            }
            return result;
        });

        log.info("[logging work number {}] Log entry stored and sent to elog system", foundWork.getWorkNumber());
        return savedLogEntry.getId();
    }

    /**
     * Create a new log entry for an activity
     *
     * @param workId The id of the work plan or job execution
     * @param activityId The id of the activity
     * @param entry  The log entry
     * @param files  The files to be attached to the log entry
     */
    public String createNewLogEntry(String workId, String activityId, NewLogEntry entry, MultipartFile[] files) {
        var foundActivity = activityRepository.findById(activityId).orElseThrow(()-> ActivityNotFound.notFoundById().activityId(activityId).errorCode(-1).build());

        if(foundActivity.getWorkId().compareTo(workId) != 0){
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage("Activity %s does not belong to work %s".formatted(activityId, workId))
                    .errorDomain("LogService::createNewLogEntry")
                    .build();
        }
        log.info("Creating new log entry for activity {}:{}", foundActivity.getWorkNumber(), foundActivity.getActivityNumber());
        List<String> attachmentIds = new ArrayList<>();
        //store attachments
        if (files != null) {
            log.info("[logging activity number {}:{}] Storing {} attachments", foundActivity.getWorkNumber(), foundActivity.getActivityNumber(), files.length);
            for (MultipartFile file : files) {
                attachmentIds.add(
                        wrapCatch(
                                () -> attachmentRepository.addAttachment(
                                        workId,
                                        entry.title(),
                                        file.getInputStream(),
                                        file.getOriginalFilename(),
                                        file.getContentType()
                                ),
                                -1
                        )
                );
            }
        }
        log.info("[logging activity number {}:{}] Storing log entry",foundActivity.getWorkNumber(), foundActivity.getActivityNumber());
        // store log entry
        LogEntry savedLogEntry = logEntryRepository.save(
                logEntryMapper.toModel(entry, workId, attachmentIds)
        );

        // create dto for submit entry to elog system
        // fetch all authorized on work
        log.info("[logging activity number {}:{}] Fetching all authorization", foundActivity.getWorkNumber(), foundActivity.getActivityNumber());
        List<AuthorizationDTO> allWorkAuthorization = authService.findByResourceIs(WORK_AUTHORIZATION_TEMPLATE.formatted(workId));

        // check if within allWorkAuthorization there is some ower with the postfix 'shopgroup.cws.slac.stanford.edu'
        // in this case i need to fetch the shop gorup and return an array of user id
        List<String> userIdForAuthorization = allWorkAuthorization.stream().map(
                auth -> {
                    if (auth.owner().endsWith("@shopgroup.cws.slac.stanford.edu")) {
                        log.info("[logging activity number {}:{}}] Fetching all user for shop group: {}", foundActivity.getWorkNumber(), foundActivity.getActivityNumber(), auth.owner());
                        var shopGroup = shopGroupService.findById(auth.owner().split("@")[0]);
                        return shopGroup.users().stream().map(member -> member.user().mail()).toList();
                    }
                    return List.of(auth.owner());
                }
        ).flatMap(List::stream).distinct().toList();
        log.info("[logging activity number {}:{}] Authorize elog reading to: {}", foundActivity.getWorkNumber(), foundActivity.getActivityNumber(), userIdForAuthorization);
        // create DTO
        log.info("[logging activity number {}:{}] Creating DTO for elog system", foundActivity.getWorkNumber(), foundActivity.getActivityNumber());
        ImportEntryDTO dto = new ImportEntryDTO()
                .entry
                        (
                                new EntryImportDTO()
                                        .title(entry.title())
                                        .text(entry.text())
                                        .loggedAt(entry.eventAt())
                                        .logbooks(List.of("cwm-logbook"))
                                        .originId("cwm:work:%s:activity:%s".formatted(foundActivity.getWorkNumber(), foundActivity.getActivityNumber()))

                        )
                .readerUserIds(userIdForAuthorization);
        //create kafka record
        log.info("[logging activity number {}:{}] Sending log to elog system", foundActivity.getWorkNumber(), foundActivity.getActivityNumber());
        ProducerRecord<String, ImportEntryDTO> message = new ProducerRecord<>(
                importEntryTopic,
                dto
        );
        message.headers().add("Authorization", jwtHelper.generateServiceToken().getBytes());
        // submit log to kafka topic to send to elog system
        CompletableFuture<SendResult<String, ImportEntryDTO>> future = importEntryDTOKafkaTemplate.send(message);
        future.thenAccept(result -> {
            // Success handling
            log.info(
                    "Log sent to elog system using topic {} at partition {} with offset {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        }).handle((result, throwable) -> {
            if (throwable != null) {
                final String errorMessage = "Failed to send message to topic %s : %s".formatted(importEntryTopic, throwable.getMessage());
                // Log the exception
                log.error(errorMessage);
                throw ControllerLogicException
                        .builder()
                        .errorCode(-2)
                        .errorMessage(errorMessage)
                        .errorDomain("LogService::createNewLogEntry")
                        .build();
            }
            return result;
        });

        log.info("[logging activity number {}:{}] Log entry stored and sent to elog system", foundActivity.getWorkNumber(), foundActivity.getActivityNumber());
        return savedLogEntry.getId();
    }
}
