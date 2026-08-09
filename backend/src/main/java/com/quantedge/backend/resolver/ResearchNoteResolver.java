package com.quantedge.backend.resolver;

import java.util.List;
import java.util.stream.Collectors;

import com.quantedge.backend.dto.response.ResearchNoteResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.mapper.ResearchNoteMapper;
import com.quantedge.backend.repository.ResearchNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ResearchNoteResolver {

    private final ResearchNoteRepository researchNoteRepository;
    private final ResearchNoteMapper researchNoteMapper;

    @QueryMapping
    public List<ResearchNoteResponse> researchNotes(@AuthenticationPrincipal User user, @Argument String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            return researchNoteRepository.findByUserAndCompanySymbolOrderByCreatedAtDesc(user, symbol).stream()
                    .map(researchNoteMapper::toDto)
                    .collect(Collectors.toList());
        }
        return researchNoteRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(researchNoteMapper::toDto)
                .collect(Collectors.toList());
    }
}
