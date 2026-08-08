package com.quantedge.backend.graphql;

import com.quantedge.backend.entity.User;
import com.quantedge.backend.repository.ResearchNoteRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ResearchNoteResolver {

    private final ResearchNoteRepository researchNoteRepository;

    @QueryMapping
    public List<ResearchNoteResponse> researchNotes(@AuthenticationPrincipal User user, @Argument String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            return researchNoteRepository.findByUserAndCompanySymbolOrderByCreatedAtDesc(user, symbol).stream()
                    .map(note -> new ResearchNoteResponse(
                            note.getId().toString(),
                            note.getCompany(),
                            note.getTitle(),
                            note.getContent(),
                            note.getGeneratedBy(),
                            note.getCreatedAt().toString()))
                    .collect(Collectors.toList());
        }
        return researchNoteRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(note -> new ResearchNoteResponse(
                        note.getId().toString(),
                        note.getCompany(),
                        note.getTitle(),
                        note.getContent(),
                        note.getGeneratedBy(),
                        note.getCreatedAt().toString()))
                .collect(Collectors.toList());
    }

    public record ResearchNoteResponse(
            String id,
            com.quantedge.backend.entity.Company company,
            String title,
            String content,
            String generatedBy,
            String createdAt) {}
}
