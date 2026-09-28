package com.example.grievance.service;

import com.example.grievance.entity.*;
import com.example.grievance.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class GrievanceService {
    public static final int MAX_LEVEL = 3;
    private static final List<Status> CLOSED_STATES = List.of(Status.RESOLVED, Status.CLOSED);

    @Autowired private GrievanceRepository grievanceRepo;
    @Autowired private DepartmentRepository departmentRepo;
    @Autowired private EscalationLogRepository logRepo;

    // ---------- Department CRUD ----------
    public Department createDepartment(Department d) { d.setId(null); return departmentRepo.save(d); }
    @Transactional(readOnly = true)
    public List<Department> listDepartments() { return departmentRepo.findAll(); }
    @Transactional(readOnly = true)
    public Department getDepartment(Long id) {
        return departmentRepo.findById(id).orElseThrow(() -> new NoSuchElementException("Department not found: " + id));
    }
    public Department updateDepartment(Long id, Department in) {
        Department d = getDepartment(id);
        d.setName(in.getName());
        d.setHeadOfficer(in.getHeadOfficer());
        d.setEmail(in.getEmail());
        return departmentRepo.save(d);
    }
    public void deleteDepartment(Long id) {
        departmentRepo.delete(getDepartment(id));
        departmentRepo.flush();
    }

    // ---------- Grievance CRUD ----------
    public Grievance create(Grievance g) {
        g.setId(null);
        g.setDepartment(resolveDepartment(g.getDepartment()));
        g.setStatus(Status.SUBMITTED);
        g.setEscalationLevel(1);
        g.setResolution(null);
        LocalDateTime now = LocalDateTime.now();
        g.setCreatedAt(now);
        g.setUpdatedAt(now);
        g.setDueDate(now.plusHours(g.getPriority().getSlaHours()));
        Grievance saved = grievanceRepo.save(g);
        log(saved.getId(), 0, 1, "SUBMITTED", "Grievance registered", "Citizen");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Grievance> list(Status status) {
        return status == null ? grievanceRepo.findAllByOrderByCreatedAtDesc()
                              : grievanceRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public Grievance get(Long id) {
        return grievanceRepo.findById(id).orElseThrow(() -> new NoSuchElementException("Grievance not found: " + id));
    }

    public Grievance update(Long id, Grievance in) {
        Grievance g = get(id);
        g.setTitle(in.getTitle());
        g.setDescription(in.getDescription());
        g.setCitizenName(in.getCitizenName());
        g.setCitizenEmail(in.getCitizenEmail());
        g.setCitizenPhone(in.getCitizenPhone());
        g.setCategory(in.getCategory());
        g.setDepartment(resolveDepartment(in.getDepartment()));
        if (g.getPriority() != in.getPriority() && !CLOSED_STATES.contains(g.getStatus())) {
            g.setDueDate(g.getCreatedAt().plusHours(in.getPriority().getSlaHours()));
        }
        g.setPriority(in.getPriority());
        g.setUpdatedAt(LocalDateTime.now());
        return grievanceRepo.save(g);
    }

    public void delete(Long id) {
        Grievance g = get(id);
        logRepo.deleteByGrievanceId(id);
        grievanceRepo.delete(g);
    }

    // ---------- Workflow ----------
    public Grievance changeStatus(Long id, Status newStatus, String actionBy) {
        Grievance g = get(id);
        if (newStatus == Status.ESCALATED) throw new IllegalArgumentException("Use the escalate action to escalate");
        if (newStatus == Status.RESOLVED) throw new IllegalArgumentException("Use the resolve action to resolve");
        g.setStatus(newStatus);
        g.setUpdatedAt(LocalDateTime.now());
        grievanceRepo.save(g);
        log(id, g.getEscalationLevel(), g.getEscalationLevel(), "STATUS_" + newStatus, "Status changed to " + newStatus, actionBy);
        return g;
    }

    public Grievance resolve(Long id, String resolution, String actionBy) {
        if (resolution == null || resolution.isBlank()) throw new IllegalArgumentException("Resolution note is required");
        Grievance g = get(id);
        g.setStatus(Status.RESOLVED);
        g.setResolution(resolution);
        g.setUpdatedAt(LocalDateTime.now());
        grievanceRepo.save(g);
        log(id, g.getEscalationLevel(), g.getEscalationLevel(), "RESOLVED", resolution, actionBy);
        return g;
    }

    public Grievance escalate(Long id, String reason, String actionBy) {
        Grievance g = get(id);
        if (CLOSED_STATES.contains(g.getStatus()))
            throw new IllegalStateException("Resolved/closed grievances cannot be escalated");
        if (g.getEscalationLevel() >= MAX_LEVEL)
            throw new IllegalStateException("Already at the highest escalation level");
        doEscalate(g, reason == null || reason.isBlank() ? "Manual escalation" : reason, actionBy);
        return g;
    }

    /** Auto-escalates every open grievance whose SLA deadline has passed. */
    @Scheduled(fixedRateString = "${grievance.escalation.check-ms:60000}")
    public void autoEscalateOverdue() {
        List<Grievance> overdue = grievanceRepo.findByStatusNotInAndDueDateBeforeAndEscalationLevelLessThan(
                CLOSED_STATES, LocalDateTime.now(), MAX_LEVEL);
        for (Grievance g : overdue) doEscalate(g, "SLA deadline breached - auto escalated", "SYSTEM");
    }

    private void doEscalate(Grievance g, String reason, String actionBy) {
        int from = g.getEscalationLevel();
        g.setEscalationLevel(from + 1);
        g.setStatus(Status.ESCALATED);
        g.setDueDate(LocalDateTime.now().plusHours(24)); // fresh SLA window at the next level
        g.setUpdatedAt(LocalDateTime.now());
        grievanceRepo.save(g);
        log(g.getId(), from, from + 1, "ESCALATED", reason, actionBy);
    }

    @Transactional(readOnly = true)
    public List<EscalationLog> history(Long id) {
        get(id);
        return logRepo.findByGrievanceIdOrderByTimestampAsc(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("TOTAL", grievanceRepo.count());
        for (Status s : Status.values()) m.put(s.name(), grievanceRepo.countByStatus(s));
        m.put("OVERDUE", grievanceRepo.countByStatusNotInAndDueDateBefore(CLOSED_STATES, LocalDateTime.now()));
        return m;
    }

    // ---------- helpers ----------
    private Department resolveDepartment(Department d) {
        if (d == null || d.getId() == null) return null;
        return getDepartment(d.getId());
    }

    private void log(Long gid, int from, int to, String action, String reason, String by) {
        EscalationLog l = new EscalationLog();
        l.setGrievanceId(gid);
        l.setFromLevel(from);
        l.setToLevel(to);
        l.setAction(action);
        l.setReason(reason);
        l.setActionBy(by == null || by.isBlank() ? "Officer" : by);
        l.setTimestamp(LocalDateTime.now());
        logRepo.save(l);
    }
}
