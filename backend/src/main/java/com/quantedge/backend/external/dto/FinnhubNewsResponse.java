package com.quantedge.backend.external.dto;

public record FinnhubNewsResponse(
        String category,
        long datetime,
        String headline,
        int id,
        String image,
        String related,
        String source,
        String summary,
        String url) {}
