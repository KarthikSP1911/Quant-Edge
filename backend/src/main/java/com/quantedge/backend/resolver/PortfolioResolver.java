package com.quantedge.backend.resolver;

import com.quantedge.backend.dto.response.PortfolioSummaryResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.DashboardService;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class PortfolioResolver {

    private final DashboardService dashboardService;

    public PortfolioResolver(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @QueryMapping
    public PortfolioSummaryResponse portfolio(@AuthenticationPrincipal User user) {
        return dashboardService.getPortfolioSummary(user);
    }
}
