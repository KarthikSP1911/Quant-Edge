package com.quantedge.backend.repository;

import java.util.Optional;

import com.quantedge.backend.entity.OAuthCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthCodeRepository extends JpaRepository<OAuthCode, java.util.UUID> {

    Optional<OAuthCode> findByCode(String code);
}
