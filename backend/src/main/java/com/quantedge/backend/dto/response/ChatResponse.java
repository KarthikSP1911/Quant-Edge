package com.quantedge.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {
    private String response;
    // We can also return toolCalls for transparency if we want
}
