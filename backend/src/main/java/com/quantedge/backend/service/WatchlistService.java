package com.quantedge.backend.service;

import java.util.List;

import com.quantedge.backend.aop.Auditable;
import com.quantedge.backend.dto.response.WatchlistItemResponse;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.entity.Watchlist;
import com.quantedge.backend.exception.CompanyNotFoundException;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Add/remove/list are idempotent: adding an already-watched symbol or removing an absent one is a no-op. */
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    public List<WatchlistItemResponse> list(User user) {
        return watchlistRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(item -> new WatchlistItemResponse(
                        companyMapper.toResponse(item.getCompany()),
                        item.getCreatedAt().toString()))
                .toList();
    }

    @Auditable(action = "WATCHLIST_ADD", entityType = "WATCHLIST", entityId = "#symbol")
    @Transactional
    public void add(User user, String symbol) {
        Company company = findCompany(symbol);
        if (watchlistRepository.findByUserAndCompany(user, company).isEmpty()) {
            watchlistRepository.save(
                    Watchlist.builder().user(user).company(company).build());
        }
    }

    @Auditable(action = "WATCHLIST_REMOVE", entityType = "WATCHLIST", entityId = "#symbol")
    @Transactional
    public void remove(User user, String symbol) {
        Company company = findCompany(symbol);
        watchlistRepository.findByUserAndCompany(user, company).ifPresent(watchlistRepository::delete);
    }

    private Company findCompany(String symbol) {
        return companyRepository
                .findBySymbol(symbol)
                .orElseThrow(() -> new CompanyNotFoundException("Unknown symbol: " + symbol));
    }
}
