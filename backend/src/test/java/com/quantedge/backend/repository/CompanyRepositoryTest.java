package com.quantedge.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.quantedge.backend.entity.Company;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void findsCompanyBySymbol() {
        Company company = Company.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .sector("Technology")
                .industry("Consumer Electronics")
                .exchange("NASDAQ")
                .build();
        companyRepository.save(company);

        assertThat(companyRepository.findBySymbol("AAPL")).isPresent();
    }

    @Test
    void findsCompaniesBySectorCaseInsensitive() {
        companyRepository.save(Company.builder()
                .symbol("MSFT")
                .name("Microsoft Corporation")
                .sector("Technology")
                .industry("Software - Infrastructure")
                .exchange("NASDAQ")
                .build());

        assertThat(companyRepository.findBySectorIgnoreCase("technology")).hasSize(1);
    }
}
