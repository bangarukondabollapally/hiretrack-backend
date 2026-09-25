package com.hiretrack.dashboard;

import com.hiretrack.dashboard.dto.DashboardResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponseDto> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        DashboardResponseDto dashboard = dashboardService.getDashboard(userDetails.getUsername());
        return ResponseEntity.ok(dashboard);
    }
}
