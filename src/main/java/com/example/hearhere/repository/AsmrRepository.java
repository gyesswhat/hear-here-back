package com.example.hearhere.repository;

import com.example.hearhere.entity.Asmr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Repository
public interface AsmrRepository extends JpaRepository<Asmr, Long> {
    @Query(nativeQuery = true, value = "SELECT * FROM asmr WHERE asmr.user_id=:userId")
    ArrayList<Asmr> findAllByUserId(@Param("userId") String userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM asmr WHERE user_id IS NULL AND generated_time <= :cutoffTime", nativeQuery = true)
    void deleteOldRecordsWithoutUserId(@Param("cutoffTime") String cutoffTime);
}
