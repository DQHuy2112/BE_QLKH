package com.example.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogStatisticsDto {
    private Long totalLogs;
    private Long totalUsers;
    private Map<String, Long> actionCounts; // Action -> Count
    private Map<String, Long> topUsers; // Username -> Count
    private Long todayLogs;
    private Long weekLogs;
    private Long monthLogs;
}

