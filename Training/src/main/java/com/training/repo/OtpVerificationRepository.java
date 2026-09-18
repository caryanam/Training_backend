package com.training.repo;

import com.training.entity.OtpVerification;
import com.training.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findByIdentifierAndPurpose(String identifier, OtpPurpose purpose);
}
