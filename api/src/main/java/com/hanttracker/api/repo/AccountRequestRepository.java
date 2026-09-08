package com.hanttracker.api.repo;

import com.hanttracker.api.domain.AccountRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {

    boolean existsByEmailIgnoreCase(String email);

    List<AccountRequest> findAllByOrderByRequestedOnDesc();
}
