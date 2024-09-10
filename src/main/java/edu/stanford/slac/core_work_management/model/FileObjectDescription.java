package edu.stanford.slac.core_work_management.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;

@Getter
@Setter
@Builder
public class FileObjectDescription implements AutoCloseable{
    private InputStream is;
    private String fileName;
    private String contentType;

    @Override
    public void close() throws IOException {
        if (is != null) {
            is.close();
        }
    }
}
