package com.libris.stats;

import com.libris.stats.dto.AdminStatsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sits under {@code /api/admin}, so the ADMIN role is already enforced by the filter chain. */
@Tag(name = "Administración", description = "Estadísticas globales de la biblioteca")
@RestController
@RequestMapping("/api/admin")
public class AdminStatsController {

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return statsService.overview();
    }
}
