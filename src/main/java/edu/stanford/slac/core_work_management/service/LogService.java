package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.NewLogEntry;
import org.springframework.web.multipart.MultipartFile;

public interface LogService {
    String createNewLogEntry(String workId, NewLogEntry entry, MultipartFile[] files);

    /**
     * Create a new log entry for an activity
     *
     * @param workId The id of the work plan or job execution
     * @param activityId The id of the activity
     * @param entry  The log entry
     * @param files  The files to be attached to the log entry
     */
    String createNewLogEntry(String workId, String activityId, NewLogEntry entry, MultipartFile[] files);
}
