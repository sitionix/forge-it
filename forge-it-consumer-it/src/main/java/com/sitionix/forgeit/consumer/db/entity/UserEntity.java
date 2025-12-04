package com.sitionix.forgeit.consumer.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@Entity
@Table(name = "USERS")
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USERNAME")
    private String username;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "STATUS_ID", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private UserStatusEntity status;
}
