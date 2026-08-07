package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.entity.Watchlist;
import com.quantedge.backend.exception.CompanyNotFoundException;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.WatchlistRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private CompanyRepository companyRepository;

    private WatchlistService watchlistService;

    private final Company apple = Company.builder().symbol("AAPL").build();
    private final User user = User.builder().build();

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistService(watchlistRepository, companyRepository, new CompanyMapper());
    }

    @Test
    void addThrowsWhenSymbolUnknown() {
        when(companyRepository.findBySymbol("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.add(user, "NOPE")).isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void addSavesWhenNotAlreadyWatched() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        when(watchlistRepository.findByUserAndCompany(user, apple)).thenReturn(Optional.empty());

        watchlistService.add(user, "AAPL");

        verify(watchlistRepository).save(any(Watchlist.class));
    }

    @Test
    void addIsIdempotentWhenAlreadyWatched() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        when(watchlistRepository.findByUserAndCompany(user, apple))
                .thenReturn(Optional.of(
                        Watchlist.builder().user(user).company(apple).build()));

        watchlistService.add(user, "AAPL");

        verify(watchlistRepository, never()).save(any());
    }

    @Test
    void removeDeletesWhenPresent() {
        Watchlist watchlist = Watchlist.builder().user(user).company(apple).build();
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        when(watchlistRepository.findByUserAndCompany(user, apple)).thenReturn(Optional.of(watchlist));

        watchlistService.remove(user, "AAPL");

        verify(watchlistRepository).delete(watchlist);
    }

    @Test
    void removeIsIdempotentWhenAbsent() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));
        when(watchlistRepository.findByUserAndCompany(user, apple)).thenReturn(Optional.empty());

        watchlistService.remove(user, "AAPL");

        verify(watchlistRepository, never()).delete(any());
    }

    @Test
    void listReturnsWatchlistItemsInRepositoryOrder() {
        Watchlist watchlist = Watchlist.builder()
                .user(user)
                .company(apple)
                .createdAt(java.time.Instant.parse("2026-08-06T00:00:00Z"))
                .build();
        when(watchlistRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(watchlist));

        List<com.quantedge.backend.dto.response.WatchlistItemResponse> result = watchlistService.list(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).company().symbol()).isEqualTo("AAPL");
    }
}
