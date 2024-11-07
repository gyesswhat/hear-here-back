package com.example.hearhere.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GenerateAsmrResponseDto {
    private Long asmrId;
    private String title;
    private String musicUrl;
    private ArrayList<SoundDetailDto> soundDetails;

    @JsonIgnore
    public List<String> getSoundUrls() {
        return soundDetails.stream()
                .map(SoundDetailDto::getUrl)
                .collect(Collectors.toList());
    }
}
