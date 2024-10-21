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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
        if (responseDto != null) return ResponseEntity.status(HttpStatus.OK).body(responseDto);
        else {
            String jsonResponse = "{\"error\": \"Internal server error\", \"message\": \"" + "랜덤 프롬프트 생성에 실패했습니다." + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(headers).body(jsonResponse);
        }
    }

    @PostMapping("/asmr/generate")
    public ResponseEntity<?> generateASMR(@RequestBody GenerateAsmrRequestDto dto) throws InterruptedException {
        // 0. 값 검증
        if (!dto.isValid()) {
            String jsonResponse = "{\"error\": \"Bad Request\", \"message\": \"" + "유저 프롬프트가 없거나 isMusicIncluded가 0 또는 1이 아닙니다." + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(jsonResponse);
        }

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
                responseDto.setMusicUrl(audioInfo.get(0).get("audio_url").toString());
            } else {
                String jsonResponse = "{\"error\": \"Intenal server error\", \"message\": \"" + "음악 생성에 실패했습니다." + "\"}";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(headers).body(jsonResponse);
            }

        }
        // 4. 소리 요소 찾기
        List<String> audioPromptList = (List<String>) finalPrompt.get("audio");
        ArrayList<String> audioUrls = audioSearchService.searchSoundByPrompt(audioPromptList);
        responseDto.setSoundUrls(audioUrls);

        // 5. 제목 짓기
        Map<String, Object> generatedTitle = chatGptService.generateTitle(dto.getUserPrompt());
        responseDto.setTitle((String) generatedTitle.get("title"));

        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @PostMapping("/asmr/save")
    public ResponseEntity<?> saveAsmr(@RequestHeader("Authorization") String authorizationHeader,
                                      @RequestBody SaveAsmrRequestDto requestDto) {
        // 1. 토큰 사용해서 유저 uuid 찾기
        String accessToken = jwtUtil.getTokenFromHeader(authorizationHeader);
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        // 2. DTO 값 검증
        if (!requestDto.isValid()) {
            String jsonResponse = "{\"error\": \"Bad Request\", \"message\": \"" + "URL 형식이 잘못되었거나 볼륨 값이 잘못되었습니다." + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(jsonResponse);
        }
        // 3. ASMR 저장
        Asmr created = new Asmr(null, userId, requestDto.getTitle(), requestDto.getMusicUrl(), requestDto.getMusicVolumn(), requestDto.getSoundUrls().toString(), requestDto.getSoundVolumns().toString(), requestDto.getSoundPositions().toString());
        Asmr saved = asmrRepository.save(created);
        // 4. 값 리턴
        if (saved != null) return ResponseEntity.status(HttpStatus.OK).body(new SaveAsmrResponseDto(saved.getAsmrId()));
        else {
            String jsonResponse = "{\"error\": \"Internal server error\", \"message\": \"" + "ASMR 저장에 실패했습니다." + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(headers).body(jsonResponse);
        }
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
        for (int i = 0; i < searchedList.size(); i++) {
            Asmr searched = searchedList.get(i);
            responses.add(new RetrieveAsmrDto(
                    searched.getAsmrId(),
                    searched.getTitle(),
                    searched.getMusicUrl(),
                    searched.getMusicVolumn(),
                    ArrayParser.parseStringToArrayList(searched.getSoundUrls()),
                    ArrayParser.parseStringToIntegerArrayList(searched.getSoundVolumns()),
                    ArrayParser.parseStringToNestedIntegerArrayList(searched.getSoundPositions())
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
        if (searched == null || !searched.getUserId().toString().equals(userId)) {
            String jsonResponse = "{\"error\": \"Bad Request\", \"message\": \"" + "asmrId가 잘못되었습니다." + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(jsonResponse);
        }
        // 3. DTO에 넣어서 리턴
        RetrieveAsmrDto response = new RetrieveAsmrDto(
                searched.getAsmrId(),
                searched.getTitle(),
                searched.getMusicUrl(),
                searched.getMusicVolumn(),
                ArrayParser.parseStringToArrayList(searched.getSoundUrls()),
                ArrayParser.parseStringToIntegerArrayList(searched.getSoundVolumns()),
                ArrayParser.parseStringToNestedIntegerArrayList(searched.getSoundPositions())
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/asmr/my-asmr/{asmrId}/update/sound")
    public ResponseEntity<?> updateMyAsmrSound(@PathVariable("asmrId") Long asmrId,
                                               @RequestHeader("Authorization") String authorizationHeader,
                                               @RequestBody UpdateAsmrSoundDto requestDto) {
        // 0. DTO 값 검증
        if (!requestDto.isValid()) {
            String jsonResponse = "{\"error\": \"Bad Request\", \"message\": \"" + "URL 형식이 잘못되었거나 볼륨 값이 잘못되었습니다." + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(jsonResponse);
        }
        // 1. 토큰 사용해서 유저 id 찾기
        String accessToken = jwtUtil.getTokenFromHeader(authorizationHeader);
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        // 2. 유저 id, asmr id 사용해서 엔티티 찾기
        Asmr searched = asmrRepository.findById(asmrId).orElse(null);
        if (searched == null || !searched.getUserId().toString().equals(userId)) {
            String jsonResponse = "{\"error\": \"Bad Request\", \"message\": \"" + "asmrId가 잘못되었습니다." + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(jsonResponse);
        }
        // 3. 내용 수정
        searched.setMusicUrl(requestDto.getMusicUrl());
        searched.setMusicVolumn(requestDto.getMusicVolumn());
        searched.setSoundUrls(requestDto.getSoundUrls().toString());
        searched.setSoundVolumns(requestDto.getSoundVolumns().toString());
        searched.setSoundPositions(requestDto.getSoundPositions().toString());
        Asmr saved = asmrRepository.save(searched);
        // 4. 리턴
        if (saved == null) {
            String jsonResponse = "{\"error\": \"Internal Server Error\", \"message\": \"" + "수정에 실패했습니다." + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(headers).body(jsonResponse);
        }
        else return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PatchMapping("/asmr/my-asmr/{asmrId}/update/title")
    public ResponseEntity<?> updateMyAsmrTitle(@PathVariable("asmrId") Long asmrId,
                                               @RequestHeader("Authorization") String authorizationHeader,
                                               @RequestBody UpdateAsmrTitleDto updateAsmrTitleDto) {
        // 1. 토큰 사용해서 유저 id 찾기
        String accessToken = jwtUtil.getTokenFromHeader(authorizationHeader);
        String userId = jwtUtil.getUserIdFromToken(accessToken);
        // 2. 유저 id, asmr id 사용해서 엔티티 찾기
        Asmr searched = asmrRepository.findById(asmrId).orElse(null);
        if (searched == null || !searched.getUserId().toString().equals(userId)) {
            String jsonResponse = "{\"error\": \"Bad Request\", \"message\": \"" + "asmrId가 잘못되었습니다." + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(jsonResponse);
        }
        // 3. 수정
        searched.setTitle(updateAsmrTitleDto.getTitle());
        Asmr saved = asmrRepository.save(searched);
        // 4. 리턴
        if (saved == null) {
            String jsonResponse = "{\"error\": \"Internal Server Error\", \"message\": \"" + "수정에 실패했습니다." + "\"}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(headers).body(jsonResponse);
        }
        else return ResponseEntity.status(HttpStatus.OK).build();
    }
}
