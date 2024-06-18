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
import edu.stanford.slac.core_work_management.model.LogEntry;
import edu.stanford.slac.core_work_management.repository.AttachmentRepository;
import edu.stanford.slac.core_work_management.repository.LogEntryRepository;
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
@Profile("!elog-support")
@RequiredArgsConstructor
public class LogDummyService {


    /**
     * Create a new log entry for a work plan or job execution
     *
     * @param workId The id of the work plan or job execution
     * @param entry  The log entry
     * @param files  The files to be attached to the log entry
     */
    @Transactional
    public String createNewLogEntry(String workId, NewLogEntry entry, MultipartFile[] files) {
        return "fake id";
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
        return "fake id";
    }
}
