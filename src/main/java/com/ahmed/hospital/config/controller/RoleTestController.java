package com.ahmed.hospital.config.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Development-only smoke test endpoints to verify role-based access control.
 * Hidden from Swagger UI — not part of the public API.
 */
@RestController
@RequestMapping("/api")
@Hidden
public class RoleTestController {

    @GetMapping("/admin/test")
    public String admin() {
        return "Hello Admin";
    }

    @GetMapping("/doctor/test")
    public String doctor() {
        return "Hello Doctor";
    }

    @GetMapping("/receptionist/test")
    public String receptionist() {
        return "Hello Receptionist";
    }

    @GetMapping("/patient/test")
    public String patient() {
        return "Hello Patient";
    }
}