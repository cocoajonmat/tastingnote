package com.dongjin.tastingnote.alcohol.service;

import com.dongjin.tastingnote.alcohol.dto.AlcoholRequestCreateRequest;
import com.dongjin.tastingnote.alcohol.dto.AlcoholRequestResponse;
import com.dongjin.tastingnote.alcohol.dto.AlcoholResponse;
import com.dongjin.tastingnote.alcohol.entity.*;
import com.dongjin.tastingnote.alcohol.repository.AlcoholAliasRepository;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRequestRepository;
import com.dongjin.tastingnote.common.exception.BusinessException;
import com.dongjin.tastingnote.common.exception.ErrorCode;
import com.dongjin.tastingnote.user.entity.User;
import com.dongjin.tastingnote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlcoholRequestService {

    private final AlcoholRequestRepository alcoholRequestRepository;
    private final AlcoholRepository alcoholRepository;
    private final AlcoholAliasRepository alcoholAliasRepository;
    private final UserRepository userRepository;

    @Transactional
    public void request(Long userId, AlcoholRequestCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateNoDuplicateName(user, req.getName(), req.getNameKo());

        AlcoholRequest alcoholRequest = AlcoholRequest.builder()
                .requestedBy(user)
                .name(req.getName())
                .nameKo(req.getNameKo())
                .aliases(req.getAliases() != null ? req.getAliases() : List.of())
                .reason(req.getReason())
                .category(req.getCategory())
                .build();

        alcoholRequestRepository.save(alcoholRequest);
    }

    @Transactional(readOnly = true)
    public List<AlcoholRequestResponse> getRequests(AlcoholRequestStatus status) {
        return alcoholRequestRepository.findAllByStatusOrderByCreatedAtDesc(status).stream()
                .map(r -> {
                    List<AlcoholResponse> similar = alcoholRepository.searchByKeyword(r.getName())
                            .stream().map(AlcoholResponse::from).toList();
                    return AlcoholRequestResponse.from(r, similar);
                })
                .toList();
    }

    @Transactional
    public void approve(Long requestId) {
        AlcoholRequest req = findPendingRequest(requestId);

        Alcohol alcohol = Alcohol.builder()
                .name(req.getName())
                .nameKo(req.getNameKo())
                .category(req.getCategory())
                .build();
        alcoholRepository.save(alcohol);

        saveAliases(alcohol, req);
        req.approve();
    }

    @Transactional
    public void merge(Long requestId, Long alcoholId) {
        AlcoholRequest req = findPendingRequest(requestId);

        Alcohol target = alcoholRepository.findById(alcoholId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALCOHOL_NOT_FOUND));

        saveAliases(target, req);
        req.merge(target);
    }

    // name, nameKo, aliases 전부 alias로 저장 (중복 제외)
    private void saveAliases(Alcohol alcohol, AlcoholRequest req) {
        List<String> candidates = new ArrayList<>();
        candidates.add(req.getName());
        if (req.getNameKo() != null && !req.getNameKo().isBlank()) {
            candidates.add(req.getNameKo());
        }
        candidates.addAll(req.getAliases());

        List<AlcoholAlias> toSave = candidates.stream()
                .filter(a -> !alcoholAliasRepository.existsByAliasIgnoreCase(a))
                .map(a -> AlcoholAlias.builder()
                        .alcohol(alcohol)
                        .alias(a)
                        .build())
                .toList();
        alcoholAliasRepository.saveAll(toSave);
    }

    // 이름 중복 검증 헬퍼 (요청 이력 + 기존 술 + 별칭 통합 체크)
    private void validateNoDuplicateName(User user, String name, String nameKo) {
        if (alcoholRequestRepository.existsByRequestedByAndNameIgnoreCase(user, name)
                || alcoholRequestRepository.existsByNameIgnoreCaseAndStatus(name, AlcoholRequestStatus.PENDING)
                || alcoholRepository.existsByNameIgnoreCase(name)
                || alcoholRepository.existsByNameKoIgnoreCase(nameKo)
                || alcoholAliasRepository.existsByAliasIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ALCOHOL_REQUEST);
        }
    }

    @Transactional
    public void reject(Long requestId, String rejectReason) {
        AlcoholRequest req = findPendingRequest(requestId);
        req.reject(rejectReason);
    }

    private AlcoholRequest findPendingRequest(Long requestId) {
        AlcoholRequest req = alcoholRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALCOHOL_REQUEST_NOT_FOUND));

        if (req.getStatus() != AlcoholRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED);
        }
        return req;
    }
}