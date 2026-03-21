package com.example.inventory_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final RequestMappingHandlerMapping handlerMapping;

    public DebugController(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @GetMapping("/mappings")
    public List<String> mappings() {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .map(e -> e.getKey().getPatternsCondition() + " -> " + e.getValue().toString())
                .collect(Collectors.toList());
    }
}

