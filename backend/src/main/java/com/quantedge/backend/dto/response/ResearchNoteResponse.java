package com.quantedge.backend.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchNoteResponse {
    private UUID id;
    private com.quantedge.backend.entity.Company company;
    private String title;
    private String content;
    private String generatedBy;
    private String createdAt;
}
