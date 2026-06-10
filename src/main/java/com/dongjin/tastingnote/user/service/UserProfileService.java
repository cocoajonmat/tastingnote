package com.dongjin.tastingnote.user.service;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import com.dongjin.tastingnote.event.entity.UserEvent;
import com.dongjin.tastingnote.event.entity.UserEventType;
import com.dongjin.tastingnote.event.repository.UserEventRepository;
import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.repository.NoteRepository;
import com.dongjin.tastingnote.user.entity.UserProfile;
import com.dongjin.tastingnote.user.repository.UserProfileRepository;
import com.dongjin.tastingnote.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final double VIEW_SCORE = 1.0;
    private static final double NOTE_CREATED_SCORE = 3.0;
    private static final int MAX_TOP_ALCOHOLS = 5;
    private static final int MAX_SEARCH_KEYWORDS = 10;

    private final UserEventRepository userEventRepository;
    private final AlcoholRepository alcoholRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void buildAllProfiles() {
        userRepository.findAll().forEach(user -> {
            try {
                buildProfile(user.getId());
            } catch (Exception e) {
                log.warn("프로파일 빌드 실패 — userId: {}, error: {}", user.getId(), e.getMessage());
            }
        });
    }

    @Transactional
    public void buildProfile(Long userId) {
        List<UserEvent> events = userEventRepository.findAllByUserId(userId);
        List<Note> publishedNotes = noteRepository.findPublishedNotesByUserId(userId);

        Map<String, Double> categoryScores = computeCategoryScores(events);
        List<TopAlcohol> topRatedAlcohols = computeTopRatedAlcohols(publishedNotes);
        List<String> recentSearchKeywords = extractRecentSearchKeywords(events);
        int totalNotes = publishedNotes.size();
        BigDecimal avgRating = computeAvgRating(publishedNotes);

        UserProfileData data = new UserProfileData(
                totalNotes,
                avgRating,
                categoryScores,
                topRatedAlcohols,
                recentSearchKeywords,
                LocalDateTime.now()
        );

        try {
            String json = objectMapper.writeValueAsString(data);
            UserProfile profile = userProfileRepository.findById(userId)
                    .orElse(UserProfile.of(userId, json));
            profile.update(json);
            userProfileRepository.save(profile);
        } catch (Exception e) {
            log.warn("프로파일 JSON 직렬화 실패 — userId: {}, error: {}", userId, e.getMessage());
        }
    }

    private Map<String, Double> computeCategoryScores(List<UserEvent> events) {
        List<Long> alcoholIds = events.stream()
                .filter(e -> e.getEventType() == UserEventType.VIEW_ALCOHOL
                        || e.getEventType() == UserEventType.NOTE_CREATED
                        || e.getEventType() == UserEventType.NOTE_RATED)
                .map(e -> extractLong(e.getMetadata(), "alcoholId"))
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, String> alcoholCategoryMap = alcoholRepository.findAllById(alcoholIds).stream()
                .collect(Collectors.toMap(Alcohol::getId, a -> a.getCategory().name()));

        Map<String, Double> scores = new HashMap<>();

        for (UserEvent event : events) {
            Long alcoholId = extractLong(event.getMetadata(), "alcoholId");
            if (alcoholId == null) continue;

            String category = alcoholCategoryMap.get(alcoholId);
            if (category == null) continue;

            double score = switch (event.getEventType()) {
                case VIEW_ALCOHOL -> VIEW_SCORE;
                case NOTE_CREATED -> NOTE_CREATED_SCORE;
                case NOTE_RATED -> {
                    Double rating = extractDouble(event.getMetadata(), "rating");
                    yield rating != null ? rating : 0.0;
                }
                default -> 0.0;
            };

            scores.merge(category, score, Double::sum);
        }

        return scores;
    }

    private List<TopAlcohol> computeTopRatedAlcohols(List<Note> notes) {
        return notes.stream()
                .filter(n -> n.getAlcohol() != null)
                .sorted(Comparator.comparing(Note::getRating).reversed())
                .limit(MAX_TOP_ALCOHOLS)
                .map(n -> new TopAlcohol(
                        n.getAlcohol().getId(),
                        n.getAlcohol().getName(),
                        n.getAlcohol().getNameKo(),
                        n.getRating()
                ))
                .toList();
    }

    private List<String> extractRecentSearchKeywords(List<UserEvent> events) {
        return events.stream()
                .filter(e -> e.getEventType() == UserEventType.SEARCH)
                .sorted(Comparator.comparing(UserEvent::getCreatedAt).reversed())
                .map(e -> extractString(e.getMetadata(), "keyword"))
                .filter(k -> k != null && !k.isBlank())
                .distinct()
                .limit(MAX_SEARCH_KEYWORDS)
                .toList();
    }

    private BigDecimal computeAvgRating(List<Note> notes) {
        if (notes.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = notes.stream()
                .map(Note::getRating)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(notes.size()), 1, RoundingMode.HALF_UP);
    }

    private Long extractLong(String metadata, String key) {
        if (metadata == null) return null;
        try {
            Map<?, ?> map = objectMapper.readValue(metadata, Map.class);
            Object val = map.get(key);
            if (val instanceof Number) return ((Number) val).longValue();
        } catch (Exception ignored) {}
        return null;
    }

    private Double extractDouble(String metadata, String key) {
        if (metadata == null) return null;
        try {
            Map<?, ?> map = objectMapper.readValue(metadata, Map.class);
            Object val = map.get(key);
            if (val instanceof Number) return ((Number) val).doubleValue();
        } catch (Exception ignored) {}
        return null;
    }

    private String extractString(String metadata, String key) {
        if (metadata == null) return null;
        try {
            Map<?, ?> map = objectMapper.readValue(metadata, Map.class);
            Object val = map.get(key);
            return val != null ? val.toString() : null;
        } catch (Exception ignored) {}
        return null;
    }

    record UserProfileData(
            int totalNotes,
            BigDecimal avgRating,
            Map<String, Double> categoryScores,
            List<TopAlcohol> topRatedAlcohols,
            List<String> recentSearchKeywords,
            LocalDateTime updatedAt
    ) {}

    record TopAlcohol(
            Long alcoholId,
            String name,
            String nameKo,
            BigDecimal rating
    ) {}
}