package com.devhabits.repository;

import com.devhabits.model.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitRepository extends JpaRepository<Habit, UUID> {

    // Trouver tous les habits d'un user
    List<Habit> findByUserIdAndIsActiveTrue(UUID userId);

    // Trouver tous les habits (actifs et archivés)
    List<Habit> findByUserId(UUID userId);

    // Trouver un habit spécifique d'un user
    Optional<Habit> findByIdAndUserId(UUID id, UUID userId);

    // Compter les habits actifs d'un user
    long countByUserIdAndIsActiveTrue(UUID userId);

    // Trouver les habits par catégorie
    List<Habit> findByUserIdAndCategoryAndIsActiveTrue(
        UUID userId, 
        String category
    );
}