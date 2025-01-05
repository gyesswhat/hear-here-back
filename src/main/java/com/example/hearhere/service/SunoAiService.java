package com.example.hearhere.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SunoAiService {
    private final String baseUrl = "https://apibox.erweima.ai/api/v1";
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${suno.api.key}")
    private String sunoApiKey;

    private final Map<String, CompletableFuture<Map<String, Object>>> taskResults = new ConcurrentHashMap<>();

    // 오디오 작업 시작 + 태스크 ID 리턴
    public String generateTaskId(Map<String, Object> payload) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(sunoApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/generate",
                HttpMethod.POST,
                entity,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> responseBody = response.getBody();
            log.info("Suno API response: {}", responseBody);

            if (responseBody != null) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                if (data != null) {
                    String taskId = (String) data.get("taskId");
                    if (taskId != null && !taskId.isEmpty()) {
                        log.info("Task ID retrieved: {}", taskId);
                        taskResults.put(taskId, new CompletableFuture<>());
                        return taskId;
                    }
                }
            }
        }

        log.error("응답에서 taskId 추출 실패: {}", response.getBody());
        throw new Exception("SUNO API 요청에서 taskId 추출에 실패함");
    }

    // future에 결과 들어오면 AsmrController로 전달
    public Map<String, Object> getResultByTaskId(String taskId) throws Exception {
        CompletableFuture<Map<String, Object>> future = taskResults.get(taskId);
        if (future == null) {
            throw new Exception("No task found for ID: " + taskId);
        }

        // CompletableFuture이 작업 마치기를 기다림
        return future.get(); // result가 준비되면 리턴
    }

    // CallbackController에서 작업 완료되었다고 호출하면 future에 결과 넣어줌
    public void completeTask(String taskId, Map<String, Object> result) {
        CompletableFuture<Map<String, Object>> future = taskResults.get(taskId);
        if (future != null) {
            future.complete(result);
        } else {
            log.warn("No CompletableFuture found for task ID: {}", taskId);
        }
    }

    // 완료된 결과에서 musicUrl 추출
    public String getMusicUrlFromResult(Map<String, Object> result) {
        // 1. result 검증
        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("Result map is null or empty");
        }

        // 2. data 부분 추출
        List<Map<String, Object>> dataList = null;
        Object dataObj = result.get("data");
        if (dataObj instanceof List) {
            List<?> rawList = (List<?>) dataObj;
            if (!rawList.isEmpty() && rawList.get(0) instanceof Map) {
                dataList = (List<Map<String, Object>>) rawList;
            }
        }

        // 3. 데이터의 첫번째 파트 추출
        Map<String, Object> firstItem = dataList.get(0);
        if (firstItem == null || firstItem.isEmpty()) {
            throw new IllegalArgumentException("First item in data list is null or empty");
        }

        // 4. 첫번째 부분에서 musicUrl 추출
        String musicUrl = (String) firstItem.get("audio_url");
        if (musicUrl == null || musicUrl.isEmpty()) {
            throw new IllegalArgumentException("Audio URL is null or empty in the first item");
        }

        return musicUrl;
    }
}
