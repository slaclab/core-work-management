package edu.stanford.slac.core_work_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.InputStream;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class StorageObject implements AutoCloseable {
    String filename;
    String contentType;
    InputStream file;

    @Override
    public void close() throws Exception {
        if (file != null) {
            file.close();
        }
    }
}
