package com.wikicoding.matrizbackend.projectmanagement.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** JPA representation of the single-assignee {@code task_assignment} table. */
@Entity
@Table(name = "task_assignment")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class TaskAssignmentDataModel {

    @Id
    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

}
