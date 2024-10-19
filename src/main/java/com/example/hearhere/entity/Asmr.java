package com.example.hearhere.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "asmr")
public class Asmr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asmr_id")
    private Long asmrId;

    @Column(name = "user_id")
    private String userId;

    @Column(name="title")
    private String title;

    @Column(name = "music_url")
    private String musicUrl;

    @Column(name = "music_volumn")
    private Integer musicVolumn;

    @Column(name = "sound_urls", length = 10000)
    private String soundUrls;

    @Column(name = "sound_volumns")
    private String soundVolumns;

    @Column(name = "sound_positions")
    private String soundPositions;

}
