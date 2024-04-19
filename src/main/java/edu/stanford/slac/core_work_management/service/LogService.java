package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.NewLogEntry;
import edu.stanford.slac.core_work_management.api.v1.mapper.LogEntryMapper;
import edu.stanford.slac.core_work_management.model.LogEntry;
import edu.stanford.slac.core_work_management.repository.AttachmentRepository;
import edu.stanford.slac.core_work_management.repository.LogEntryRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Profile("elog-support")
@Service
@AllArgsConstructor
public class LogService {
    LogEntryMapper logEntryMapper;
    LogEntryRepository logEntryRepository;
    AttachmentRepository attachmentRepository;

    /**
     * Create a new log entry
     *
     * @param workId The id of the work plan or job execution
     * @param entry  The log entry
     * @param files  The files to be attached to the log entry
     */
    @Transactional
    public String createNewLogEntry(String workId, NewLogEntry entry, MultipartFile[] files) {
        List<String> attachmentIds = new ArrayList<>();
        //store attachments
        if (files != null) {
            for (MultipartFile file : files) {

                    attachmentIds.add(
                            wrapCatch(
                                    ()->attachmentRepository.addAttachment(
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

        // store log entry
        LogEntry savedLogEntry = logEntryRepository.save(
                logEntryMapper.toModel(entry, workId, attachmentIds)
        );
        return savedLogEntry.getId();
    }
}
