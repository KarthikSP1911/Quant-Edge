package com.quantedge.backend.resolver;

import com.quantedge.backend.dto.response.TimeMachineResultResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.TimeMachineService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class TimeMachineResolver {

    private final TimeMachineService timeMachineService;

    public TimeMachineResolver(TimeMachineService timeMachineService) {
        this.timeMachineService = timeMachineService;
    }

    @QueryMapping
    public TimeMachineResultResponse portfolioTimeMachine(
            @AuthenticationPrincipal User user, @Argument String asOfDate) {
        return timeMachineService.getTimeMachine(user, asOfDate);
    }
}
