package com.hanttracker.api.repo;

import com.hanttracker.api.domain.EmailCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailCodeRepository extends JpaRepository<EmailCode, Long> {}
