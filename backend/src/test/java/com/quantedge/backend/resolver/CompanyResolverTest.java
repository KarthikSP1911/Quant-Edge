package com.quantedge.backend.resolver;

import static org.mockito.Mockito.when;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.repository.CompanyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest({CompanyResolver.class, CompanyMapper.class})
class CompanyResolverTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private CompanyRepository companyRepository;

    private Company apple() {
        return Company.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .sector("Technology")
                .industry("Consumer Electronics")
                .exchange("NASDAQ")
                .build();
    }

    @Test
    void companiesReturnsAllSeededCompanies() {
        when(companyRepository.findAll()).thenReturn(List.of(apple()));

        graphQlTester
                .document("{ companies { symbol name sector } }")
                .execute()
                .path("companies")
                .entityList(Object.class)
                .hasSize(1);
    }

    @Test
    void companyReturnsMatchBySymbol() {
        when(companyRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple()));

        graphQlTester
                .document("{ company(symbol: \"AAPL\") { symbol name exchange } }")
                .execute()
                .path("company.symbol")
                .entity(String.class)
                .isEqualTo("AAPL");
    }

    @Test
    void companyReturnsNullWhenSymbolNotFound() {
        when(companyRepository.findBySymbol("NOPE")).thenReturn(Optional.empty());

        graphQlTester
                .document("{ company(symbol: \"NOPE\") { symbol } }")
                .execute()
                .path("company")
                .valueIsNull();
    }
}
