package edu.stanford.slac.core_work_management.service;

import edu.stanford.slac.core_work_management.api.v1.dto.NewLogEntry;
import org.springframework.web.multipart.MultipartFile;

public interface LogService {
    String createNewLogEntry(String workId, NewLogEntry entry, MultipartFile[] files);
}
