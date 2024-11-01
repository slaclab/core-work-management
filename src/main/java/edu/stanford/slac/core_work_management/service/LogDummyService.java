package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.NewLogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Log4j2
@Service
@Profile("!elog-support")
@RequiredArgsConstructor
public class LogDummyService implements LogService{
    /**
     * Create a new log entry for a work plan or job execution
     *
     * @param workId The id of the work plan or job execution
     * @param entry  The log entry
     * @param files  The files to be attached to the log entry
     */
    @Transactional
    public String createNewLogEntry(String workId, NewLogEntry entry, MultipartFile[] files) {
        log.info("LogDummyService.createNewLogEntry: workId={}, entry={}, files={}", workId, entry, files);
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
        log.info("LogDummyService.createNewLogEntry: workId={}, activityId={}, entry={}, files={}", workId, activityId, entry, files);
        return "fake id";
    }
}
