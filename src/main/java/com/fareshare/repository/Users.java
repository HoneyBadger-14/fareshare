package com.fareshare.repository;

import com.fareshare.model.Model;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Users extends JpaRepository<Model.User, UUID> {
    Optional<Model.User> findByEmail(String email);
}
