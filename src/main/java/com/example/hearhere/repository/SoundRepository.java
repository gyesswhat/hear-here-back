package com.example.hearhere.repository;

import com.example.hearhere.entity.Sound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoundRepository extends JpaRepository<Sound, Long> {
    @Query(value = "SELECT * FROM sound s " +
            "WHERE s.category = :category " +
            "AND (" +
            "      s.tag LIKE CONCAT('%', :tag1, '%') " +
            "   OR s.tag LIKE CONCAT('%', :tag2, '%') " +
            "   OR s.tag LIKE CONCAT('%', :tag3, '%')" +
            ")", nativeQuery = true)
    List<Sound> findByCategoryAndAnyTag(
            @Param("category") String category,
            @Param("tag1") String tag1,
            @Param("tag2") String tag2,
            @Param("tag3") String tag3
    );
}
