package com.dongjin.tastingnote.alcohol.repository;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlcoholRepository extends JpaRepository<Alcohol, Long> {

    @Query("""
            SELECT DISTINCT a FROM Alcohol a
            LEFT JOIN AlcoholAlias aa ON aa.alcohol = a
            WHERE a.name LIKE %:keyword%
               OR a.nameKo LIKE %:keyword%
               OR aa.alias LIKE %:keyword%
            """)
    List<Alcohol> searchByKeyword(@Param("keyword") String keyword);

    List<Alcohol> findAllByCategory(AlcoholCategory category);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameKoIgnoreCase(String nameKo);

    // ── 카테고리별 커서 페이지네이션 ───────────────────────────────────────
    @Query("SELECT a FROM Alcohol a WHERE a.category = :category AND a.id < :cursor ORDER BY a.id DESC")
    List<Alcohol> findByCategoryWithCursor(
            @Param("category") AlcoholCategory category,
            @Param("cursor") Long cursor,
            Pageable pageable);

    // ── 키워드 검색 커서 페이지네이션 ────────────────────────────────────
    @Query("SELECT DISTINCT a FROM Alcohol a LEFT JOIN AlcoholAlias aa ON aa.alcohol = a " +
           "WHERE (a.name LIKE %:keyword% OR a.nameKo LIKE %:keyword% OR aa.alias LIKE %:keyword%) " +
           "AND a.id < :cursor ORDER BY a.id DESC")
    List<Alcohol> searchByKeywordWithCursor(
            @Param("keyword") String keyword,
            @Param("cursor") Long cursor,
            Pageable pageable);
}