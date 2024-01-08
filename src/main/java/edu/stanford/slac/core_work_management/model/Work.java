package edu.stanford.slac.core_work_management.model;

import org.springframework.data.annotation.*;

import java.time.LocalDateTime;

public class Work {
    @Id
    private String id;
    private String name;
    private String description;
    @CreatedDate
    private LocalDateTime createdDate;
    @CreatedBy
    private String createdBy;
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
    @LastModifiedBy
    private String lastModifiedBy;
    @Version
    private Long version;
}
