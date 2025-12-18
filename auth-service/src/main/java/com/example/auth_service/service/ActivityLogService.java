package com.example.auth_service.service;

import com.example.auth_service.dto.ActivityLogDto;
import com.example.auth_service.dto.ActivityLogStatisticsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

public interface ActivityLogService {
    Page<ActivityLogDto> searchActivityLogs(Long userId, String action, Date startDate, Date endDate, String ipAddress, String userAgent, String keyword, Pageable pageable);
    ActivityLogDto createActivityLog(ActivityLogDto log);
    Page<ActivityLogDto> getActivityLogsByUserId(Long userId, Pageable pageable);
    ActivityLogDto getActivityLogById(Long id);
    void deleteActivityLog(Long id);
    void deleteActivityLogsBulk(List<Long> ids);
    ActivityLogStatisticsDto getStatistics(Date startDate, Date endDate);
    void cleanupOldLogs(); // scheduled auto-cleanup
}

