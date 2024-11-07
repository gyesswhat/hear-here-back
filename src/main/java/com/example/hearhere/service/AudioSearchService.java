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

        ArrayList<SoundDetailDto> soundDetailList = new ArrayList<>(); // Set을 사용하여 중복 제거

        for (Map<String, String> prompt : audioPromptList) {
            String category = prompt.get("category");
            Object tagObject = prompt.get("tag");
            List<String> tags;

            if (tagObject instanceof List) {
                tags = (List<String>) tagObject;
            } else {
                tags = new ArrayList<>();
            }
            log.info(tags.toString());
            String intensity = prompt.get("intensity");

            List<Sound> sounds = new ArrayList<>();

            for (String tag : tags) {
                sounds.addAll(soundRepository.findByTag(tag));
            }

            for (Sound sound : sounds) {
                SoundDetailDto soundDetailDto = new SoundDetailDto(
                        sound.getSoundId(),
                        s3Url + sound.getName() + ".wav",
                        sound.getLength()
                );
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
