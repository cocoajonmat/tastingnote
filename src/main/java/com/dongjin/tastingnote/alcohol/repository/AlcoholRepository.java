package com.dongjin.tastingnote.alcohol.repository;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlcoholRepository extends JpaRepository<Alcohol, Long> {
}