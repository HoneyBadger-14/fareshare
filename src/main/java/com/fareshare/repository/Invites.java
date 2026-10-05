package com.fareshare.repository;

import com.fareshare.model.Model;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Invites extends JpaRepository<Model.Invite, UUID> {
    Optional<Model.Invite> findByTokenHash(String tokenHash);
}
