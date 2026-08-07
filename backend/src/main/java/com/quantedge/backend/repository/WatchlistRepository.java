package com.quantedge.backend.repository;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.entity.Watchlist;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {

    List<Watchlist> findByUserOrderByCreatedAtDesc(User user);

    Optional<Watchlist> findByUserAndCompany(User user, Company company);
}
