package com.szcinda.repository;

import lombok.Data;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@MappedSuperclass
public class BaseEntity implements Serializable {
    @Id
    private String id;

    private LocalDateTime createTime = LocalDateTime.now();

    @Version
    private long version;
}
