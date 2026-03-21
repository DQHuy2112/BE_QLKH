package com.example.inventory_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProbeController {
    @GetMapping("/probe")
    public String probe() {
        return "probe-ok";
    }
}

