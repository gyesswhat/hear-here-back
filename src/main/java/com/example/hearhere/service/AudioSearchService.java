package com.example.hearhere.service;

import com.example.hearhere.dto.SoundDetailDto;
import com.example.hearhere.entity.Sound;
import com.example.hearhere.repository.SoundRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AudioSearchService {
    @Autowired
    private SoundRepository soundRepository;
    @Value("${s3.basic.url}")
    private String s3Url;

    public ArrayList<SoundDetailDto> searchSoundByPrompt(List<Map<String, String>> audioPromptList) {
        log.info("audioPromptList\n" + audioPromptList.toString());

        ArrayList<SoundDetailDto> soundDetailList = new ArrayList<>();

        for (Map<String, String> prompt : audioPromptList) {
            // 1. 값 가져오기
            String category = prompt.get("category");
            Object tagObject = prompt.get("tag");
            List<String> tags;

            if (tagObject instanceof List) {
                tags = (List<String>) tagObject;
            } else {
                tags = new ArrayList<>();
            }
            log.info(tags.toString());

            // 2. 검색을 위해 정리
            String tag1 = tags.size() > 0 ? tags.get(0) : null;
            String tag2 = tags.size() > 1 ? tags.get(1) : null;
            String tag3 = tags.size() > 2 ? tags.get(2) : null;

            // 3. 검색
            List<Sound> sounds = soundRepository.findByCategoryAndAnyTag(category, tag1, tag2, tag3);

            // 4. DTO로 변환
            for (Sound sound : sounds) {
                SoundDetailDto soundDetailDto = new SoundDetailDto(
                        sound.getSoundId(),
                        s3Url + sound.getName() + ".wav",
                        sound.getLength()
                );
                // 중복 방지
                boolean exists = soundDetailList.stream()
                        .anyMatch(existingDetail -> existingDetail.getUrl().equals(soundDetailDto.getUrl()));

                if (!exists) {
                    soundDetailList.add(soundDetailDto);
                }
            }
        }

        return soundDetailList;
    }
}
