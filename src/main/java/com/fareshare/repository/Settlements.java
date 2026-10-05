package com.fareshare.repository;

import com.fareshare.model.Model;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Settlements extends JpaRepository<Model.Settlement, UUID> {
    List<Model.Settlement> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
    Optional<Model.Settlement> findByGroupIdAndRequestKey(UUID groupId, String requestKey);
}
