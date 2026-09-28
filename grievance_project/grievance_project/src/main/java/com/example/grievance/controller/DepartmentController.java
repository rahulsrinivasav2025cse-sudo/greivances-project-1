package com.example.grievance.controller;

import com.example.grievance.entity.Department;
import com.example.grievance.service.GrievanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    @Autowired private GrievanceService service;

    @GetMapping public List<Department> all() { return service.listDepartments(); }
    @GetMapping("/{id}") public Department one(@PathVariable Long id) { return service.getDepartment(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Department create(@Valid @RequestBody Department d) { return service.createDepartment(d); }
    @PutMapping("/{id}")
    public Department update(@PathVariable Long id, @Valid @RequestBody Department d) { return service.updateDepartment(id, d); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.deleteDepartment(id); }
}
