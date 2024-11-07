package com.example.hearhere.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Sound {
    @Id
    @Column(name = "sound_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long soundId;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "tag", length = 100)
    private String tag;

    @Column(name = "intensity", length = 50)
    private String intensity;

    @Column(name = "length", length = 10)
    private String length;
}
