package com.example.grievance.repository;

import com.example.grievance.entity.Grievance;
import com.example.grievance.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface GrievanceRepository extends JpaRepository<Grievance, Long> {
    List<Grievance> findByStatusOrderByCreatedAtDesc(Status status);
    List<Grievance> findAllByOrderByCreatedAtDesc();
    long countByStatus(Status status);
    long countByStatusNotInAndDueDateBefore(Collection<Status> statuses, LocalDateTime now);
    List<Grievance> findByStatusNotInAndDueDateBeforeAndEscalationLevelLessThan(
            Collection<Status> statuses, LocalDateTime now, int level);
}
