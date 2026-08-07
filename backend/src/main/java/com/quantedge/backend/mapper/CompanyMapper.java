package com.quantedge.backend.mapper;

import com.quantedge.backend.dto.response.CompanyResponse;
import com.quantedge.backend.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getSymbol(),
                company.getName(),
                company.getSector(),
                company.getIndustry(),
                company.getDescription(),
                company.getLogoUrl(),
                company.getExchange());
    }
}
