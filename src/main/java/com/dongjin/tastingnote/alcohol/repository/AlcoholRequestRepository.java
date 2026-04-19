package com.dongjin.tastingnote.alcohol.repository;

import com.dongjin.tastingnote.alcohol.entity.AlcoholRequest;
import com.dongjin.tastingnote.alcohol.entity.AlcoholRequestStatus;
import com.dongjin.tastingnote.alcohol.entity.AlcoholRequestType;
import com.dongjin.tastingnote.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlcoholRequestRepository extends JpaRepository<AlcoholRequest, Long> {
    List<AlcoholRequest> findAllByStatusOrderByCreatedAtDesc(AlcoholRequestStatus status);
    List<AlcoholRequest> findAllByStatusAndTypeOrderByCreatedAtDesc(AlcoholRequestStatus status, AlcoholRequestType type);
    boolean existsByRequestedByAndNameIgnoreCase(User requestedBy, String name);
    boolean existsByNameIgnoreCaseAndStatus(String name, AlcoholRequestStatus status);
    boolean existsByRequestedByAndNameKoIgnoreCase(User requestedBy, String nameKo);
    boolean existsByNameKoIgnoreCaseAndStatus(String nameKo, AlcoholRequestStatus status);
    boolean existsByRequestedByAndTargetAlcoholIdAndStatus(User requestedBy, Long targetAlcoholId, AlcoholRequestStatus status);

    @Query("SELECT COUNT(r) > 0 FROM AlcoholRequest r JOIN r.aliases a WHERE LOWER(a) = LOWER(:alias) AND r.status = :status")
    boolean existsByAliasIgnoreCaseAndStatus(@Param("alias") String alias, @Param("status") AlcoholRequestStatus status);
}