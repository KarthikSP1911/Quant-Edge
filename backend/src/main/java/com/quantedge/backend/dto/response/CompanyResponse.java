package com.quantedge.backend.dto.response;

import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String symbol,
        String name,
        String sector,
        String industry,
        String description,
        String logoUrl,
        String exchange) {}
