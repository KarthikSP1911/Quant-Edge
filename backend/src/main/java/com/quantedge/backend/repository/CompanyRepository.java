package com.quantedge.backend.repository;

import com.quantedge.backend.entity.Company;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findBySymbol(String symbol);

    List<Company> findBySectorIgnoreCase(String sector);
}
