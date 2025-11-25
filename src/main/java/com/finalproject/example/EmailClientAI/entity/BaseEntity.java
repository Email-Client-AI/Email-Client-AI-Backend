package com.finalproject.example.EmailClientAI.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @CreatedBy
    @Column(name = "CreatedBy", nullable = false, updatable = false)
    protected String createdBy;

    @CreatedDate
    @Column(name = "CreatedDate", updatable = false)
    protected Instant createdDate;

    @LastModifiedBy
    @Column(name = "ModifiedBy")
    protected String modifiedBy;

    @LastModifiedDate
    @Column(name = "ModifiedDate")
    protected Instant modifiedDate;

    @Column(name = "DeletedBy")
    protected String deletedBy;

    @Column(name = "DeletedDate")
    protected Instant deletedDate;
}
