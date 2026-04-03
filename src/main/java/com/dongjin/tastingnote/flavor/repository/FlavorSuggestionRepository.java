package com.dongjin.tastingnote.flavor.repository;

import com.dongjin.tastingnote.flavor.entity.FlavorSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlavorSuggestionRepository extends JpaRepository<FlavorSuggestion, Long> {
}
