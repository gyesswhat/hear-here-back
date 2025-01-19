package com.example.hearhere.controller;

import com.example.hearhere.common.ApiResponse;
import com.example.hearhere.common.parser.ArrayParser;
import com.example.hearhere.common.status.ErrorStatus;
import com.example.hearhere.common.util.FileUtil;
import com.example.hearhere.dto.*;
import com.example.hearhere.entity.Asmr;
import com.example.hearhere.entity.Sound;
import com.example.hearhere.repository.AsmrRepository;
import com.example.hearhere.repository.ExamplePromptRepository;
import com.example.hearhere.repository.SoundRepository;
import com.example.hearhere.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AsmrController {

    @Autowired
    private TokenService tokenService;
    @Autowired
    private ChatGptService chatGptService;
    @Autowired
    private SunoAiService sunoAiService;
    @Autowired
    private AudioSearchService audioSearchService;
    @Autowired
    private MusicDurationService musicDurationService;

    @Autowired
    private ExamplePromptRepository examplePromptRepository;
    @Autowired
    private AsmrRepository asmrRepository;
    @Autowired
    private SoundRepository soundRepository;

    @Autowired
    private FileUtil fileUtil;

    @Value("${s3.basic.url}")
    private String s3Url;

    @GetMapping("/asmr/randomprompts")
    public ResponseEntity<?> getRandomPrompts() {
        ArrayList<String> randomprompts = (ArrayList<String>) examplePromptRepository.get3RandomPrompts();
        GenerateRandomPromptResponseDto responseDto = new GenerateRandomPromptResponseDto(randomprompts);
        if (responseDto != null) return ResponseEntity.status(HttpStatus.OK).body(responseDto);
        else return ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/asmr/generate")
    public ResponseEntity<?> generateASMR(HttpSession session, @RequestBody GenerateAsmrRequestDto dto) throws Exception {
        // 1. 값 검증
        if (!dto.isValid()) return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST);
        GenerateAsmrResponseDto responseDto = new GenerateAsmrResponseDto();

        // 2. ChatGPT로 프롬프트 작업
        Map<String, Object> finalPrompt = chatGptService.generatePrompt(dto.getUserPrompt());
        log.info(finalPrompt.toString());

        // 3. 음악이 있을 경우
        if (dto.getIsMusicIncluded().equals("1")) {
            // 3-1. 프롬프트 찾기
            List<String> musicPromptList = (List<String>) finalPrompt.get("music");
            String musicPrompt = musicPromptList.stream().collect(Collectors.joining(", "));

            // 3-2. payload 만들기
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("customMode", true);
            payload.put("instrumental", true);
            payload.put("prompt", musicPrompt);
            payload.put("callBackUrl", "https://hearhere-back.site/callback/suno");

            // 3-3. 음악 생성
            try {
                // 3-3-1. Suno API를 호출하여 Task ID 생성
                String taskId = sunoAiService.generateTaskId(payload);
                log.info("Task ID generated: {}", taskId);

                // 3-3-2. Task ID를 기반으로 완료된 데이터를 대기 및 반환
                Map<String, Object> result = sunoAiService.getResultByTaskId(taskId);
                log.info("Received complete result for Task ID {}: {}", taskId, result);

                String musicUrl = sunoAiService.getMusicUrlFromResult(result);
                responseDto.setMusicUrl(musicUrl);
            } catch (Exception e) {
                log.error("Error generating ASMR: {}", e.getMessage(), e);
                return ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
            }
        }

        // 4. 소리 요소 찾기
        Object soundPromptObject = finalPrompt.get("sound");

        // 4-1. 값 검증
        if (soundPromptObject instanceof List<?>) {
            List<?> soundPromptListRaw = (List<?>) soundPromptObject;

            // 각 요소가 Map<String, String> 인지 확인 후 변환
            List<Map<String, String>> soundPromptList = soundPromptListRaw.stream()
                    .filter(element -> element instanceof Map)
                    .map(element -> (Map<String, String>) element)
                    .collect(Collectors.toList());

            ArrayList<SoundDetailDto> soundDetails = audioSearchService.searchSoundByPrompt(soundPromptList);
            responseDto.setSoundDetails(soundDetails);
        } else {
            return ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
        }

        // 5. 제목 짓기
        Map<String, Object> generatedTitle = chatGptService.generateTitle(dto.getUserPrompt());
        responseDto.setTitle((String) generatedTitle.get("title"));

        // 6. 저장
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Asmr generated = new Asmr(
                null,
                null,
                responseDto.getTitle(),
                responseDto.getMusicUrl(),
                null,
                responseDto.getSoundUrls().toString(),
                null,
                null,
                formatter.format(localDateTime)
        );
        Asmr saved = asmrRepository.save(generated);
        responseDto.setAsmrId(saved.getAsmrId());

        // 7. 리턴
        if (responseDto.getAsmrId() == null) return ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
        else return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @PatchMapping("/asmr/save")
    public ResponseEntity<?> saveAsmr(@RequestHeader("Authorization") String authorizationHeader,
                                      @RequestBody SaveAsmrRequestDto requestDto) {
        // 1. 토큰 사용해서 유저 id 찾기
        String userId = tokenService.findUserIdByToken(authorizationHeader);
        // 2. DTO 값 검증
        if (!requestDto.isValid()) return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST);
        // 3. ASMR 저장
        Asmr searched = asmrRepository.findById(requestDto.getAsmrId()).orElse(null);
        if (searched == null) return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST);
        searched.setUserId(userId);
        searched.setTitle(requestDto.getTitle());
        searched.setMusicUrl(requestDto.getMusicUrl());
        searched.setMusicVolumn(requestDto.getMusicVolumn());
        searched.setSoundUrls(requestDto.getSoundUrls().toString());
        searched.setSoundVolumns(requestDto.getSoundVolumns().toString());
        searched.setSoundPositions(requestDto.getSoundPositions().toString());
        Asmr saved = asmrRepository.save(searched);
        // 4. 값 리턴
        if (saved != null) return ResponseEntity.status(HttpStatus.OK).body(new SaveAsmrResponseDto(saved.getAsmrId()));
        else return ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/asmr/my-asmr")
    public ResponseEntity<?> getMyAsmrList(@RequestHeader("Authorization") String authorizationHeader) {
        // 1. 토큰 사용해서 유저 ID 찾기
        String userId = tokenService.findUserIdByToken(authorizationHeader);

        // 2. 유저 ID 사용해서 ASMR 리스트 찾기
        List<Asmr> searchedList = asmrRepository.findAllByUserId(userId);

        // 3. DTO에 넣어서 리턴
        List<RetrieveAsmrListDto> responses = new ArrayList<>();
        for (Asmr searched : searchedList) {
            // RetrieveAsmrDto 생성
            responses.add(new RetrieveAsmrListDto(
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
    public ResponseEntity<?> getMyAsmr(
            @PathVariable("asmrId") Long asmrId,
            @RequestHeader("Authorization") String authorizationHeader) {
        // 1. 토큰 사용해서 유저 ID 찾기
        String userId = tokenService.findUserIdByToken(authorizationHeader);

        // 2. 유저 ID와 asmrId 사용해서 ASMR 엔티티 찾기
        Asmr searched = asmrRepository.findById(asmrId).orElse(null);
        if (searched == null || !searched.getUserId().toString().equals(userId)) {
            return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST);
        }

        // 3. soundUrls를 이용해 soundDetails 생성
        List<String> soundUrls = ArrayParser.parseStringToArrayList(searched.getSoundUrls());
        ArrayList<SoundDetailDto> soundDetails = new ArrayList<>();
        for (String url : soundUrls) {
            // URL에서 name 추출
            String name = extractNameFromUrl(url);

            // name으로 Sound 엔티티 조회
            Sound sound = soundRepository.findByName(name);
            log.info("sound:" + sound);
            if (sound != null) {
                soundDetails.add(new SoundDetailDto(
                        sound.getSoundId(),
                        s3Url + sound.getName() + ".wav", // URL을 name + ".wav" 형식으로 생성
                        sound.getLength()
                ));
            }
        }

        String duration = null;
        File tempFile = null;
        if (searched.getMusicUrl() != null) {
            try {
                tempFile = fileUtil.downloadFile(searched.getMusicUrl());
                log.info("Temp file path: " + tempFile.getAbsolutePath());

                if (!fileUtil.isWavFile(tempFile)) {
                    duration = musicDurationService.getMp3Duration(tempFile.getAbsolutePath());
                }
                else {
                    duration = musicDurationService.getWavDuration(tempFile.getAbsolutePath());
                }
                log.info("Duration: " + duration);
            } catch (UnsupportedOperationException e) {
                log.error("Unsupported file format: ", e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Unsupported file format.");
            } catch (Exception e) {
                log.error("Error occurred: ", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
            }
        }

        // 4. RetrieveAsmrDto 생성
        RetrieveAsmrDto response = new RetrieveAsmrDto(
                searched.getAsmrId(),
                searched.getTitle(),
                searched.getMusicUrl(),
                searched.getMusicVolumn(),
                duration,
                ArrayParser.parseStringToArrayList(searched.getSoundUrls()),
                ArrayParser.parseStringToIntegerArrayList(searched.getSoundVolumns()),
                ArrayParser.parseStringToNestedIntegerArrayList(searched.getSoundPositions()),
                soundDetails // soundDetails 추가
        );

        // 5. 응답 반환
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/asmr/my-asmr/{asmrId}/update/sound")
    public ResponseEntity<?> updateMyAsmrSound(@PathVariable("asmrId") Long asmrId,
                                               @RequestHeader("Authorization") String authorizationHeader,
                                               @RequestBody UpdateAsmrSoundDto requestDto) {
        // 0. DTO 값 검증
        if (!requestDto.isValid()) return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST);
        // 1. 토큰 사용해서 유저 id 찾기
        String userId = tokenService.findUserIdByToken(authorizationHeader);
        // 2. 유저 id, asmr id 사용해서 엔티티 찾기
        Asmr searched = asmrRepository.findById(asmrId).orElse(null);
        if (searched == null || !searched.getUserId().toString().equals(userId)) return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST);
        // 3. 내용 수정
        searched.setMusicUrl(requestDto.getMusicUrl());
        searched.setMusicVolumn(requestDto.getMusicVolumn());
        searched.setSoundUrls(requestDto.getSoundUrls().toString());
        searched.setSoundVolumns(requestDto.getSoundVolumns().toString());
        searched.setSoundPositions(requestDto.getSoundPositions().toString());
        Asmr saved = asmrRepository.save(searched);
        // 4. 리턴
        if (saved == null) return ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
        else return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PatchMapping("/asmr/my-asmr/{asmrId}/update/title")
    public ResponseEntity<?> updateMyAsmrTitle(@PathVariable("asmrId") Long asmrId,
                                               @RequestHeader("Authorization") String authorizationHeader,
                                               @RequestBody UpdateAsmrTitleDto updateAsmrTitleDto) {
        // 1. 토큰 사용해서 유저 id 찾기
        String userId = tokenService.findUserIdByToken(authorizationHeader);
        // 2. 유저 id, asmr id 사용해서 엔티티 찾기
        Asmr searched = asmrRepository.findById(asmrId).orElse(null);
        if (searched == null || !searched.getUserId().toString().equals(userId)) return ApiResponse.onFailure(ErrorStatus._BAD_REQUEST);
        // 3. 수정
        searched.setTitle(updateAsmrTitleDto.getTitle());
        Asmr saved = asmrRepository.save(searched);
        // 4. 리턴
        if (saved == null) return ApiResponse.onFailure(ErrorStatus._INTERNAL_SERVER_ERROR);
        else return ResponseEntity.status(HttpStatus.OK).build();
    }

    private String extractNameFromUrl(String url) {
        log.info(url);
        if (url == null || !url.contains("/")) {
            return null; // URL이 비정상적일 경우 처리
        }
        // URL에서 마지막 "/" 뒤의 파일 이름만 추출
        String[] parts = url.split("/");
        String filename = parts[parts.length - 1]; // 마지막 부분이 파일 이름
        // 확장자(.wav) 제거 후 파일 이름 반환
        return filename.replace(".wav", "");
    }
}
