package com.dongjin.tastingnote.user.service;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.alcohol.repository.AlcoholRepository;
import com.dongjin.tastingnote.event.entity.UserEvent;
import com.dongjin.tastingnote.event.entity.UserEventType;
import com.dongjin.tastingnote.event.repository.UserEventRepository;
import com.dongjin.tastingnote.note.entity.Note;
import com.dongjin.tastingnote.note.repository.NoteRepository;
import com.dongjin.tastingnote.user.dto.UserProfileData;
import com.dongjin.tastingnote.user.dto.UserProfileData.TopAlcohol;
import com.dongjin.tastingnote.user.entity.UserProfile;
import com.dongjin.tastingnote.user.repository.UserProfileRepository;
import com.dongjin.tastingnote.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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

        UserProfileData data = new UserProfileData(
                publishedNotes.size(),
                computeCategoryScores(events),
                computeTopRatedAlcohols(publishedNotes),
                extractRecentSearchKeywords(events),
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
        List<UserEvent> alcoholEvents = events.stream()
                .filter(e -> e.getEventType() == UserEventType.VIEW_ALCOHOL
                        || e.getEventType() == UserEventType.NOTE_CREATED
                        || e.getEventType() == UserEventType.NOTE_RATED)
                .toList();

        Set<Long> alcoholIds = alcoholEvents.stream()
                .map(e -> MetadataParser.parse(e.getMetadata(), objectMapper).getLong("alcoholId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> categoryMap = alcoholRepository.findAllById(alcoholIds).stream()
                .collect(Collectors.toMap(Alcohol::getId, a -> a.getCategory().name()));

        Map<String, Double> scores = new HashMap<>();
        for (UserEvent event : alcoholEvents) {
            MetadataParser meta = MetadataParser.parse(event.getMetadata(), objectMapper);
            Long alcoholId = meta.getLong("alcoholId");
            String category = alcoholId != null ? categoryMap.get(alcoholId) : null;
            if (category == null) continue;

            double score = switch (event.getEventType()) {
                case VIEW_ALCOHOL -> VIEW_SCORE;
                case NOTE_CREATED -> NOTE_CREATED_SCORE;
                case NOTE_RATED -> Objects.requireNonNullElse(meta.getDouble("rating"), 0.0);
                default -> 0.0;
            };
            scores.merge(category, score, Double::sum);
        }
        return scores;
    }

    private List<TopAlcohol> computeTopRatedAlcohols(List<Note> notes) {
        return notes.stream()
                .filter(n -> n.getAlcohol() != null)
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
                .map(e -> MetadataParser.parse(e.getMetadata(), objectMapper).getString("keyword"))
                .filter(k -> k != null && !k.isBlank())
                .distinct()
                .limit(MAX_SEARCH_KEYWORDS)
                .toList();
    }

    private static class MetadataParser {
        private final Map<?, ?> map;

        private MetadataParser(Map<?, ?> map) { this.map = map; }

        static MetadataParser parse(String metadata, ObjectMapper mapper) {
            if (metadata == null) return new MetadataParser(Map.of());
            try {
                return new MetadataParser(mapper.readValue(metadata, Map.class));
            } catch (Exception e) {
                return new MetadataParser(Map.of());
            }
        }

        Long getLong(String key) {
            Object val = map.get(key);
            return val instanceof Number n ? n.longValue() : null;
        }

        Double getDouble(String key) {
            Object val = map.get(key);
            return val instanceof Number n ? n.doubleValue() : null;
        }

        String getString(String key) {
            Object val = map.get(key);
            return val != null ? val.toString() : null;
        }
    }

}
