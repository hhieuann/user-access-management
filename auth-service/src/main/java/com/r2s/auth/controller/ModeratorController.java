package com.r2s.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/moderator")
@Slf4j
public class ModeratorController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    public Map<String, String> getDashboard() {
        return Map.of("message", "Welcome to Moderator Dashboard");
    }
}