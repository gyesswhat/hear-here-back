package com.example.hearhere.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GenerateAsmrRequestDto {
    private String userPrompt;
    private String isMusicIncluded;

    public boolean isValid() {
        if (userPrompt == null || userPrompt.isEmpty()) {
            return false;
        }
        if (isMusicIncluded == null || !isMusicIncluded.matches("^[01]$")) {
            return false;
        }
        return true; // 모든 검증 통과 시 true 반환
    }
}
