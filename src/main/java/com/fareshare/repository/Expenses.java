package com.fareshare.repository;

import com.fareshare.model.Model;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Expenses extends JpaRepository<Model.Expense, UUID> {
    List<Model.Expense> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
    Optional<Model.Expense> findByGroupIdAndRequestKey(UUID groupId, String requestKey);
}
