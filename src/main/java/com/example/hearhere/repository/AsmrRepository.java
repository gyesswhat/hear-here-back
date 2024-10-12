package com.example.hearhere.repository;

import com.example.hearhere.entity.Asmr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface AsmrRepository extends JpaRepository<Asmr, Long> {
    @Query(nativeQuery = true, value = "SELECT * FROM asmr WHERE asmr.user_id=:userId")
    ArrayList<Asmr> findAllByUserId(@Param("userId") String userId);
}
