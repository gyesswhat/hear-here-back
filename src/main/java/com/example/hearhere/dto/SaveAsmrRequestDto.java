package com.example.hearhere.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SaveAsmrRequestDto {
    private String title;
    private String musicUrl;
    private Integer musicVolumn;
    private ArrayList<String> soundUrls;
    private ArrayList<Integer> soundVolumns;
    private ArrayList<ArrayList<Integer>> soundPositions;

    // 입력값 검증 메소드
    public boolean isValid() {
        return title!=null && isValidMusicUrl() && isValidMusicVolumn() && areValidSoundUrls() && areValidSoundVolumns() && areValidSoundPositions();
    }

    // 음악 URL 검증 (유효한 URL인지 확인)
    private boolean isValidMusicUrl() {
        if (musicUrl == null) return true;
        else return isValidUrl(musicUrl);
    }

    // 음악 볼륨 값 검증 (0~100 사이의 값인지 확인)
    private boolean isValidMusicVolumn() {
        if (musicVolumn == null) return true;
        else return (musicVolumn >= 0 && musicVolumn <= 100);
    }

    // 사운드 URL 검증 (유효한 URL 배열인지 확인)
    private boolean areValidSoundUrls() {
        if (soundUrls == null || soundUrls.isEmpty()) {
            return false;
        }

        for (String soundUrl : soundUrls) {
            if (!isValidUrl(soundUrl)) {
                return false;
            }
        }
        return true;
    }

    // URL 형식이 맞는지 검증 (http:// 또는 https:// 없으면 추가)
    private boolean isValidUrl(String url) {
        try {
            new URL(url); // URL 형식이 맞지 않으면 MalformedURLException 발생
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    // 사운드 볼륨 값 검증 (0~100 사이의 숫자인지 확인)
    private boolean areValidSoundVolumns() {
        if (soundVolumns == null || soundVolumns.isEmpty()) {
            return false;
        }

        for (Integer soundVolumn : soundVolumns) {
            // 볼륨 값이 0 ~ 100 사이인지 확인
            if (soundVolumn < 0 || soundVolumn > 100) {
                return false;
            }
        }
        return true;
    }

    // 사운드 위치 값 검증
    private boolean areValidSoundPositions() {
        if (soundPositions == null || soundPositions.isEmpty()) {
            return false;
        }

        for (ArrayList<Integer> positions : soundPositions) {
            if (positions == null || positions.isEmpty()) {
                return false;
            }

            for (Integer time : positions) {
                if (time == null || time < 0) {
                    return false;
                }
            }
        }

        return true;
    }
}
