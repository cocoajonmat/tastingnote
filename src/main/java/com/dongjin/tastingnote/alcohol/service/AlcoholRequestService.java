package com.dongjin.tastingnote.alcohol.service;

import com.dongjin.tastingnote.alcohol.dto.AlcoholAliasCreateRequest;
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
        if (isBlank(req.getName()) && isBlank(req.getNameKo())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateNoDuplicateName(user, req.getName(), req.getNameKo());

        AlcoholRequest alcoholRequest = AlcoholRequest.builder()
                .requestedBy(user)
                .type(AlcoholRequestType.NEW)
                .name(req.getName())
                .nameKo(req.getNameKo())
                .aliases(req.getAliases() != null ? req.getAliases() : List.of())
                .reason(req.getReason())
                .category(req.getCategory())
                .build();

        alcoholRequestRepository.save(alcoholRequest);
    }

    @Transactional
    public void requestAlias(Long userId, Long alcoholId, AlcoholAliasCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Alcohol target = alcoholRepository.findById(alcoholId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALCOHOL_NOT_FOUND));

        String alias = req.getAlias();
        if (alcoholAliasRepository.existsByAliasIgnoreCase(alias)
                || alcoholRepository.existsByNameIgnoreCase(alias)
                || alcoholRepository.existsByNameKoIgnoreCase(alias)) {
            throw new BusinessException(ErrorCode.ALCOHOL_ALREADY_EXISTS);
        }

        if (alcoholRequestRepository.existsByAliasIgnoreCaseAndStatus(alias, AlcoholRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ALCOHOL_REQUEST);
        }

        if (alcoholRequestRepository.existsByRequestedByAndTargetAlcoholIdAndStatus(user, alcoholId, AlcoholRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ALCOHOL_REQUEST);
        }

        AlcoholRequest alcoholRequest = AlcoholRequest.builder()
                .requestedBy(user)
                .type(AlcoholRequestType.ALIAS)
                .targetAlcohol(target)
                .aliases(List.of(alias))
                .reason(req.getReason())
                .build();

        alcoholRequestRepository.save(alcoholRequest);
    }

    @Transactional(readOnly = true)
    public List<AlcoholRequestResponse> getRequests(AlcoholRequestStatus status, AlcoholRequestType type) {
        List<AlcoholRequest> requests = type != null
                ? alcoholRequestRepository.findAllByStatusAndTypeOrderByCreatedAtDesc(status, type)
                : alcoholRequestRepository.findAllByStatusOrderByCreatedAtDesc(status);

        return requests.stream()
                .map(r -> {
                    String searchKey = !isBlank(r.getName()) ? r.getName() : r.getNameKo();
                    List<AlcoholResponse> similar = searchKey != null
                            ? alcoholRepository.searchByKeyword(searchKey).stream().map(AlcoholResponse::from).toList()
                            : List.of();
                    return AlcoholRequestResponse.from(r, similar);
                })
                .toList();
    }

    @Transactional
    public void approve(Long requestId) {
        AlcoholRequest req = findPendingRequestOfType(requestId, AlcoholRequestType.NEW);

        Alcohol alcohol = Alcohol.builder()
                .name(req.getName())
                .nameKo(req.getNameKo())
                .category(req.getCategory())
                .build();
        alcoholRepository.save(alcohol);

        saveNewAlcoholAliases(alcohol, req);
        req.approve();
    }

    @Transactional
    public void approveAlias(Long requestId) {
        AlcoholRequest req = findPendingRequestOfType(requestId, AlcoholRequestType.ALIAS);
        Alcohol target = req.getTargetAlcohol();

        String alias = req.getAliases().get(0);
        if (!alcoholAliasRepository.existsByAliasIgnoreCase(alias)) {
            alcoholAliasRepository.save(AlcoholAlias.builder().alcohol(target).alias(alias).build());
        }
        req.approve();
    }

    @Transactional
    public void reject(Long requestId, String rejectReason) {
        AlcoholRequest req = findPendingRequest(requestId);
        req.reject(rejectReason);
    }

    private void saveNewAlcoholAliases(Alcohol alcohol, AlcoholRequest req) {
        List<String> candidates = new ArrayList<>();
        if (!isBlank(req.getName())) candidates.add(req.getName());
        if (!isBlank(req.getNameKo())) candidates.add(req.getNameKo());
        candidates.addAll(req.getAliases());

        List<AlcoholAlias> toSave = candidates.stream()
                .filter(a -> !alcoholAliasRepository.existsByAliasIgnoreCase(a))
                .map(a -> AlcoholAlias.builder().alcohol(alcohol).alias(a).build())
                .toList();
        alcoholAliasRepository.saveAll(toSave);
    }

    private void validateNoDuplicateName(User user, String name, String nameKo) {
        if (!isBlank(name)) {
            if (alcoholRepository.existsByNameIgnoreCase(name)
                    || alcoholRepository.existsByNameKoIgnoreCase(name)
                    || alcoholAliasRepository.existsByAliasIgnoreCase(name)) {
                throw new BusinessException(ErrorCode.ALCOHOL_ALREADY_EXISTS);
            }
            if (alcoholRequestRepository.existsByRequestedByAndNameIgnoreCase(user, name)
                    || alcoholRequestRepository.existsByNameIgnoreCaseAndStatus(name, AlcoholRequestStatus.PENDING)
                    || alcoholRequestRepository.existsByNameKoIgnoreCaseAndStatus(name, AlcoholRequestStatus.PENDING)) {
                throw new BusinessException(ErrorCode.DUPLICATE_ALCOHOL_REQUEST);
            }
        }
        if (!isBlank(nameKo)) {
            if (alcoholRepository.existsByNameKoIgnoreCase(nameKo)
                    || alcoholRepository.existsByNameIgnoreCase(nameKo)
                    || alcoholAliasRepository.existsByAliasIgnoreCase(nameKo)) {
                throw new BusinessException(ErrorCode.ALCOHOL_ALREADY_EXISTS);
            }
            if (alcoholRequestRepository.existsByRequestedByAndNameKoIgnoreCase(user, nameKo)
                    || alcoholRequestRepository.existsByNameKoIgnoreCaseAndStatus(nameKo, AlcoholRequestStatus.PENDING)
                    || alcoholRequestRepository.existsByNameIgnoreCaseAndStatus(nameKo, AlcoholRequestStatus.PENDING)) {
                throw new BusinessException(ErrorCode.DUPLICATE_ALCOHOL_REQUEST);
            }
        }
    }

    private AlcoholRequest findPendingRequest(Long requestId) {
        AlcoholRequest req = alcoholRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALCOHOL_REQUEST_NOT_FOUND));
        if (req.getStatus() != AlcoholRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED);
        }
        return req;
    }

    private AlcoholRequest findPendingRequestOfType(Long requestId, AlcoholRequestType expectedType) {
        AlcoholRequest req = findPendingRequest(requestId);
        if (req.getType() != expectedType) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return req;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}