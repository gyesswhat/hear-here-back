package com.example.hearhere.repository;

import com.example.hearhere.entity.ExamplePrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface ExamplePromptRepository extends JpaRepository<ExamplePrompt, Integer> {
    @Query(nativeQuery = true, value = "SELECT prompt FROM example_prompt ORDER BY RAND() LIMIT 3;")
    List<String> get3RandomPrompts();
}
