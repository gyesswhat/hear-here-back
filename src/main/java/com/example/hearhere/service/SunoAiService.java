package com.example.hearhere.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class SunoAiService {
    private final String baseUrl = "https://suno-oah3pcnzx-gyesswhats-projects.vercel.app";
    private final RestTemplate restTemplate = new RestTemplate();

    // 음악 생성 요청
    public ResponseEntity<List> generateAudioByPrompt(Map<String, Object> payload) {
        String url = baseUrl + "/api/generate";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        // 응답을 비동기적으로 처리
        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, List.class);
    }

    // 오디오 정보 가져오기
    public List<Map<String, Object>> getAudioInfo(String audioId) {
        String url = baseUrl + "/api/get?ids=" + audioId;
        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, null, List.class);
        return (List<Map<String, Object>>) response.getBody();
    }


}
