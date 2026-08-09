package com.quantedge.backend.mapper;

import com.quantedge.backend.dto.response.ChatHistoryResponse;
import com.quantedge.backend.entity.ChatHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatHistoryMapper {
    ChatHistoryResponse toDto(ChatHistory chatHistory);
}
