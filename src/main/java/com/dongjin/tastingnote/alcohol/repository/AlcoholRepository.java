package com.dongjin.tastingnote.alcohol.repository;

import com.dongjin.tastingnote.alcohol.entity.Alcohol;
import com.dongjin.tastingnote.alcohol.entity.AlcoholCategory;
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
}