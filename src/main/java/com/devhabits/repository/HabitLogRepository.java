package com.devhabits.repository;

import com.devhabits.model.entity.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, UUID> {

    // Trouver tous les logs d'un habit
    List<HabitLog> findByHabitIdOrderByCompletedAtDesc(UUID habitId);

    // Trouver les logs d'un habit entre deux dates
    List<HabitLog> findByHabitIdAndCompletedAtBetweenOrderByCompletedAtDesc(
        UUID habitId,
        LocalDateTime start,
        LocalDateTime end
    );

    // Vérifier si un habit a été complété aujourd'hui
    @Query("SELECT COUNT(hl) > 0 FROM HabitLog hl WHERE hl.habitId = :habitId " +
    	       "AND hl.completedAt >= :startOfDay AND hl.completedAt < :endOfDay")
    	boolean existsForToday(
    	    @Param("habitId") UUID habitId,
    	    @Param("startOfDay") LocalDateTime startOfDay,
    	    @Param("endOfDay") LocalDateTime endOfDay
    	);

    // Compter les completions d'un habit
    long countByHabitId(UUID habitId);

    // Trouver le dernier log d'un habit
    Optional<HabitLog> findFirstByHabitIdOrderByCompletedAtDesc(UUID habitId);
}