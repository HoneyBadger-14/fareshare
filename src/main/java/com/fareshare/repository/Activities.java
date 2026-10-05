package com.fareshare.repository;

import com.fareshare.model.Model;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Activities extends JpaRepository<Model.Activity, UUID> {
    List<Model.Activity> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
