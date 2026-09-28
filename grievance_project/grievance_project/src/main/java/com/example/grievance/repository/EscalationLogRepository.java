package com.example.grievance.repository;

import com.example.grievance.entity.EscalationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EscalationLogRepository extends JpaRepository<EscalationLog, Long> {
    List<EscalationLog> findByGrievanceIdOrderByTimestampAsc(Long grievanceId);
    void deleteByGrievanceId(Long grievanceId);
}
