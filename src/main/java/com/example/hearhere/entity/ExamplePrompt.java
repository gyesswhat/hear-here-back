package com.example.hearhere.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "example_prompt")
public class ExamplePrompt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prompt_id")
    private Integer promptId;

    @Column(name = "prompt")
    private String prompt;
}
