package com.devhabits.controller;

import com.devhabits.model.dto.response.ApiResponse;
import com.devhabits.model.dto.response.DashboardResponse;
import com.devhabits.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard and statistics APIs")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get user dashboard with statistics")
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        DashboardResponse dashboard = dashboardService.getDashboard(userId);

        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}