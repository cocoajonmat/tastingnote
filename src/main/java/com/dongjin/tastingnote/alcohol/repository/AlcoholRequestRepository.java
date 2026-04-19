package com.dongjin.tastingnote.alcohol.repository;

import com.dongjin.tastingnote.alcohol.entity.AlcoholRequest;
import com.dongjin.tastingnote.alcohol.entity.AlcoholRequestStatus;
import com.dongjin.tastingnote.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlcoholRequestRepository extends JpaRepository<AlcoholRequest, Long> {
    List<AlcoholRequest> findAllByStatusOrderByCreatedAtDesc(AlcoholRequestStatus status);
    boolean existsByRequestedByAndNameIgnoreCase(User requestedBy, String name);

    boolean existsByNameIgnoreCaseAndStatus(String name, AlcoholRequestStatus status);
}