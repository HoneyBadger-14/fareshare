package com.fareshare.repository;

import com.fareshare.model.Model;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Members extends JpaRepository<Model.Member, UUID> {
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
    List<Model.Member> findByGroupId(UUID groupId);
    List<Model.Member> findByUserId(UUID userId);
}
