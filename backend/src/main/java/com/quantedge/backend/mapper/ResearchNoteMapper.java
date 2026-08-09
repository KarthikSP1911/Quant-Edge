package com.quantedge.backend.mapper;

import com.quantedge.backend.dto.response.ResearchNoteResponse;
import com.quantedge.backend.entity.ResearchNote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ResearchNoteMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "company", source = "company")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "generatedBy", source = "generatedBy")
    @Mapping(target = "createdAt", source = "createdAt")
    ResearchNoteResponse toDto(ResearchNote researchNote);
}
