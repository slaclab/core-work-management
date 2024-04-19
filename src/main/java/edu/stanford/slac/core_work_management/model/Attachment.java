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
public class Attachment {
    String filename;
    String contentType;
    InputStream file;
}
