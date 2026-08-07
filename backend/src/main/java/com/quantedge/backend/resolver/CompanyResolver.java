package com.quantedge.backend.resolver;

import com.quantedge.backend.dto.response.CompanyResponse;
import com.quantedge.backend.mapper.CompanyMapper;
import com.quantedge.backend.repository.CompanyRepository;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class CompanyResolver {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    public CompanyResolver(CompanyRepository companyRepository, CompanyMapper companyMapper) {
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
    }

    @QueryMapping
    public List<CompanyResponse> companies() {
        return companyRepository.findAll().stream()
                .map(companyMapper::toResponse)
                .toList();
    }

    @QueryMapping
    public CompanyResponse company(@Argument String symbol) {
        return companyRepository
                .findBySymbol(symbol)
                .map(companyMapper::toResponse)
                .orElse(null);
    }
}
