package com.quantedge.backend.resolver;

import com.quantedge.backend.dto.response.DashboardResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.DashboardService;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class DashboardResolver {

    private final DashboardService dashboardService;

    public DashboardResolver(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @QueryMapping
    public DashboardResponse dashboard(@AuthenticationPrincipal User user) {
        return dashboardService.getDashboard(user);
    }
}
