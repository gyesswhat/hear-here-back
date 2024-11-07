package com.example.hearhere.repository;

import com.example.hearhere.entity.Sound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoundRepository extends JpaRepository<Sound, Long> {
    @Query(value = "SELECT * FROM sound s WHERE s.tag LIKE CONCAT('%', :tag, '%')",
            nativeQuery = true)
    List<Sound> findByTag(
            @Param("tag") String tag);
}
