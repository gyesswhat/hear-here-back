package com.example.hearhere.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RetrieveAsmrDto {
    private Long asmrId;
    private String title;
    private String musicUrl;
    private Integer musicVolumn;
    private ArrayList<String> soundUrls;
    private ArrayList<Integer> soundVolumns;
}
