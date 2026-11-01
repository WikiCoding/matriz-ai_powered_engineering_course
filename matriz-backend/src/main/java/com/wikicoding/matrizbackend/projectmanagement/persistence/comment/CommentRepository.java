package com.wikicoding.matrizbackend.projectmanagement.persistence.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Sort;

public interface CommentRepository extends JpaRepository<CommentDataModel, UUID> {
    List<CommentDataModel> findByTaskId(UUID taskId, Sort sort);
}
