package com.fareshare.repository;

import com.fareshare.model.Model;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Shares extends JpaRepository<Model.Share, UUID> {
    List<Model.Share> findByExpenseId(UUID expenseId);
    void deleteByExpenseId(UUID expenseId);
}
