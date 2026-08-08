package com.quantedge.backend.repository;

import com.quantedge.backend.entity.ResearchNote;
import com.quantedge.backend.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResearchNoteRepository extends JpaRepository<ResearchNote, UUID> {
    List<ResearchNote> findByUserOrderByCreatedAtDesc(User user);

    List<ResearchNote> findByUserAndCompanySymbolOrderByCreatedAtDesc(User user, String symbol);
}
