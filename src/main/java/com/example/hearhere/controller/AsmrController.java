package com.example.hearhere.controller;

import com.example.hearhere.common.parser.ArrayParser;
import com.example.hearhere.dto.*;
import com.example.hearhere.entity.Asmr;
import com.example.hearhere.repository.AsmrRepository;
import com.example.hearhere.repository.ExamplePromptRepository;
import com.example.hearhere.security.jwt.JwtUtil;
import com.example.hearhere.service.AudioSearchService;
import com.example.hearhere.service.ChatGptService;
import com.example.hearhere.service.SunoAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AsmrController {

    @Autowired
    private ChatGptService chatGptService;
    @Autowired
    private SunoAiService sunoAiService;
    @Autowired
    private AudioSearchService audioSearchService;
    @Autowired
    private ExamplePromptRepository examplePromptRepository;
    @Autowired
    private AsmrRepository asmrRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/asmr/randomprompts")
    public ResponseEntity<?> getRandomPrompts() {
        ArrayList<String> randomprompts = (ArrayList<String>) examplePromptRepository.get3RandomPrompts();
        GenerateRandomPromptResponseDto responseDto = new GenerateRandomPromptResponseDto(randomprompts);
        return (responseDto!=null)?
                ResponseEntity.status(HttpStatus.OK).body(responseDto):
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("랜덤 프롬프트 생성에 실패했습니다.");
    }

    @PostMapping("/asmr/generate")
    public ResponseEntity<?> generateASMR(@RequestBody GenerateAsmrRequestDto dto) throws InterruptedException {
        // 0. 값 검증
        if (!dto.isValid()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 요청입니다.");

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
    public ResponseEntity<?> saveAsmr(@RequestHeader("Authorization") String authorizationHeader,
                                      @RequestBody SaveAsmrRequestDto requestDto) {
        // 1. 토큰 사용해서 유저 uuid 찾기
        String accessToken = jwtUtil.getTokenFromHeader(authorizationHeader);
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        // 2. DTO 값 검증
        if (!requestDto.isValid()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 요청입니다.");
        // 3. ASMR 저장
        Asmr created = new Asmr(null, userId, requestDto.getMusicUrl(), requestDto.getMusicVolumn(), requestDto.getSoundUrls().toString(), requestDto.getSoundVolumns().toString());
        Asmr saved = asmrRepository.save(created);
        // 4. 값 리턴
        return (saved!=null)?
                ResponseEntity.status(HttpStatus.OK).body(new SaveAsmrResponseDto(saved.getAsmrId())):
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ASMR 저장에 실패했습니다.");
    }

    @GetMapping("/asmr/my-asmr")
    public ResponseEntity<?> getMyAsmrList(@RequestHeader("Authorization") String authorizationHeader) {
        // 1. 토큰 사용해서 유저 uuid 찾기
        String accessToken = jwtUtil.getTokenFromHeader(authorizationHeader);
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        // 2. 유저 id 사용해서 ASMR 리스트 찾기
        ArrayList<Asmr> searchedList = asmrRepository.findAllByUserId(userId);
        // 3. DTO에 넣어서 리턴
        ArrayList<RetrieveAsmrDto> responses = new ArrayList<>();
        for (int i=0; i<searchedList.size(); i++) {
            Asmr searched = searchedList.get(i);
            responses.add(new RetrieveAsmrDto(
                    searched.getAsmrId(),
                    searched.getMusicUrl(),
                    searched.getMusicVolumn(),
                    ArrayParser.parseStringToArrayList(searched.getSoundUrls()),
                    ArrayParser.parseStringToIntegerArrayList(searched.getSoundVolumns())
            ));
        }
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @GetMapping("/asmr/my-asmr/{asmrId}")
    public ResponseEntity<?> getMyAsmr(@PathVariable("asmrId") Long asmrId, @RequestHeader("Authorization") String authorizationHeader) {
        // 1. 토큰 사용해서 유저 id 찾기
        String accessToken = jwtUtil.getTokenFromHeader(authorizationHeader);
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        // 2. 유저 id, asmr id 사용해서 엔티티 찾기
        Asmr searched = asmrRepository.findById(asmrId).orElse(null);
        if (searched==null || !searched.getUserId().toString().equals(userId))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 요청입니다.");
        // 3. DTO에 넣어서 리턴
        RetrieveAsmrDto response = new RetrieveAsmrDto(
                searched.getAsmrId(),
                searched.getMusicUrl(),
                searched.getMusicVolumn(),
                ArrayParser.parseStringToArrayList(searched.getSoundUrls()),
                ArrayParser.parseStringToIntegerArrayList(searched.getSoundVolumns())
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
