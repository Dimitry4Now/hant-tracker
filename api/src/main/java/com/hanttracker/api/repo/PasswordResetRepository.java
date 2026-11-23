package com.hanttracker.api.repo;

import com.hanttracker.api.domain.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, String> {

    @Transactional
    void deleteByUserId(Long userId);
}
