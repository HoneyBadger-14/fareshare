package com.fareshare.repository;

import com.fareshare.model.Model;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Groups extends JpaRepository<Model.Group, UUID> {
    List<Model.Group> findByIdIn(List<UUID> ids);
}
