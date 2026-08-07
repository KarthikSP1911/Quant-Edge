package com.quantedge.backend.repository;

import com.quantedge.backend.entity.OAuthCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthCodeRepository extends JpaRepository<OAuthCode, java.util.UUID> {

    Optional<OAuthCode> findByCode(String code);
}
