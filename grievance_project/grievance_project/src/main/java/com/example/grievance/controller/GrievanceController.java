package com.example.grievance.controller;

import com.example.grievance.entity.*;
import com.example.grievance.service.GrievanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GrievanceController {
    @Autowired private GrievanceService service;

    @GetMapping("/grievances")
    public List<Grievance> all(@RequestParam(required = false) Status status) { return service.list(status); }

    @GetMapping("/grievances/{id}")
    public Grievance one(@PathVariable Long id) { return service.get(id); }

    @PostMapping("/grievances") @ResponseStatus(HttpStatus.CREATED)
    public Grievance create(@Valid @RequestBody Grievance g) { return service.create(g); }

    @PutMapping("/grievances/{id}")
    public Grievance update(@PathVariable Long id, @Valid @RequestBody Grievance g) { return service.update(id, g); }

    @DeleteMapping("/grievances/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @PostMapping("/grievances/{id}/escalate")
    public Grievance escalate(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        body = body == null ? Map.of() : body;
        return service.escalate(id, body.get("reason"), body.get("actionBy"));
    }

    @PostMapping("/grievances/{id}/status")
    public Grievance status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.changeStatus(id, Status.valueOf(body.get("status")), body.get("actionBy"));
    }

    @PostMapping("/grievances/{id}/resolve")
    public Grievance resolve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.resolve(id, body.get("resolution"), body.get("actionBy"));
    }

    @GetMapping("/grievances/{id}/history")
    public List<EscalationLog> history(@PathVariable Long id) { return service.history(id); }

    @GetMapping("/stats")
    public Map<String, Long> stats() { return service.stats(); }
}
