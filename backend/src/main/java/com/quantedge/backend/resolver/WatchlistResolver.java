package com.quantedge.backend.resolver;

import com.quantedge.backend.dto.response.WatchlistItemResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.WatchlistService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class WatchlistResolver {

    private final WatchlistService watchlistService;

    public WatchlistResolver(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @QueryMapping
    public List<WatchlistItemResponse> watchlist(@AuthenticationPrincipal User user) {
        return watchlistService.list(user);
    }
}
