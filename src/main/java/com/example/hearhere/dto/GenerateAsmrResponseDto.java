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
public class GenerateAsmrResponseDto {
    private String asmrTitle;
    private String musicURL;
    private ArrayList<String> soundUrls;
}
