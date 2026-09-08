package com.hanttracker.api.web;

import com.hanttracker.api.dto.DashboardStatsDto;
import com.hanttracker.api.dto.PublicStatsDto;
import com.hanttracker.api.security.CurrentUser;
import com.hanttracker.api.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    /** The only endpoint open to signed-out visitors. */
    @GetMapping("/public")
    public PublicStatsDto publicStats() {
        return stats.publicStats();
    }

    @GetMapping("/dashboard")
    public DashboardStatsDto dashboard() {
        return stats.dashboardFor(CurrentUser.require());
    }
}
