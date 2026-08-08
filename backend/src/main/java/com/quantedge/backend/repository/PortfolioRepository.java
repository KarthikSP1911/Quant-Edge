package com.quantedge.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Portfolio;
import com.quantedge.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    Optional<Portfolio> findByUserAndCompany(User user, Company company);

    List<Portfolio> findByUser(User user);
}
