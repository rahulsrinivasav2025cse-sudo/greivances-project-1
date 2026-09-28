package com.example.grievance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "escalation_logs")
@Getter @Setter
public class EscalationLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long grievanceId;
    private int fromLevel;
    private int toLevel;
    private String action;
    @Column(length = 1000)
    private String reason;
    private String actionBy;
    private LocalDateTime timestamp;
}
