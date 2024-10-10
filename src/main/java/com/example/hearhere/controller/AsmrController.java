package com.example.hearhere.controller;

import com.example.hearhere.dto.GenerateAsmrRequestDto;
import com.example.hearhere.dto.GenerateAsmrResponseDto;
import com.example.hearhere.service.AudioSearchService;
import com.example.hearhere.service.ChatGptService;
import com.example.hearhere.service.SunoAiService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class AsmrController {

    @Autowired
    ChatGptService chatGptService;
    @Autowired
    SunoAiService sunoAiService;
    @Autowired
    AudioSearchService audioSearchService;

    @GetMapping("/asmr/randomprompts")
    public ResponseEntity<?> getRandomPrompts() {
        return null;
    }

    @PostMapping("/asmr/generate")
    public ResponseEntity<?> generateASMR(@RequestBody GenerateAsmrRequestDto dto) throws InterruptedException {
        GenerateAsmrResponseDto responseDto = new GenerateAsmrResponseDto();
        // 1. ChatGPT로 프롬프트 작업
        Map<String, Object> finalPrompt = chatGptService.generatePrompt(dto.getUserPrompt());

        // 2. 음악이 있을 경우
        if (dto.getIsMusicIncluded().equals("1")) {

            // 3. SUNO AI로 음악 요소 생성
            List<String> musicPromptList = (List<String>) finalPrompt.get("music");
            String musicPrompt = musicPromptList.stream().collect(Collectors.joining(", "));
            Map<String, Object> payload = Map.of(
                    "prompt", "Create a music piece with the following instruments and styles: " + musicPrompt,
                    "make_instrumental", true,
                    "wait_audio", false  // 비동기 모드로 설정
            );

            // 4. SUNO API로 음악 생성 요청 (비동기)
            ResponseEntity<List> generatedResponse = sunoAiService.generateAudioByPrompt(payload);

            if (generatedResponse.getBody() != null && !generatedResponse.getBody().isEmpty()) {
                Map<String, Object> generated = (Map<String, Object>) generatedResponse.getBody().get(1);
                String audioId = (String) generated.get("id");

                // 5. 오디오 생성 상태 주기적으로 확인 (폴링 방식)
                boolean isGenerated = false;
                List<Map<String, Object>> audioInfo = null;

                while (!isGenerated) {
                    // 5초 대기 후 다시 확인
                    Thread.sleep(5000);
                    audioInfo = sunoAiService.getAudioInfo(audioId);
                    // 오디오 URL이 비어 있지 않으면 생성 완료
                    if (audioInfo != null && audioInfo.get(0).get("status").toString().equals("complete")) {
                        isGenerated = true;
                    }
                }
                responseDto.setMusicURL(audioInfo.get(0).get("audio_url").toString());
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Audio generation failed");
            }

        }
        // 4. 소리 요소 찾기
        List<String> audioPromptList = (List<String>)finalPrompt.get("audio");
        ArrayList<String> audioUrls = audioSearchService.searchSoundByPrompt(audioPromptList);
        responseDto.setSoundUrls(audioUrls);

        // 5. 제목 짓기
        Map<String, Object> generatedTitle = chatGptService.generateTitle(dto.getUserPrompt());
        responseDto.setAsmrTitle((String)generatedTitle.get("title"));

        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }
    @PostMapping("/asmr/save")
    public ResponseEntity<?> saveAsmr(HttpServletRequest request) {
        return null;
    }
}
