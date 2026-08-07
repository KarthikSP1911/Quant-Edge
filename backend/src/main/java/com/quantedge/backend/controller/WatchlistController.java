package com.quantedge.backend.controller;

import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @PostMapping("/{symbol}")
    public ResponseEntity<Void> add(@AuthenticationPrincipal User user, @PathVariable String symbol) {
        watchlistService.add(user, symbol);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal User user, @PathVariable String symbol) {
        watchlistService.remove(user, symbol);
        return ResponseEntity.noContent().build();
    }
}
