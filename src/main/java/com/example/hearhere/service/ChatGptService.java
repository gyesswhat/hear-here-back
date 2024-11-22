package com.example.hearhere.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatGptService {
    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper

    public Map<String, Object> generateTitle(String prompt) {
        String systemMessage = "You are a creative assistant responsible for generating short, catchy music titles. Based on the user’s prompt, create a unique and fitting music title that is no more than 20 characters long. The title should be provided in a JSON format as: {\"title\": \"String\"}. You MUST ONLY return final JSON message IN ENGLISH.";
        return generateChat(prompt, systemMessage);
    }

    public Map<String, Object> generatePrompt(String prompt) {
        // 프롬프트 생성
        String systemMessage = "You MUST ONLY return the final JSON message IN ENGLISH. Imagine and expand upon the given user message in a descriptive manner to identify music and sound elements.\n" +
                "\n" +
                "Each sound element should have:\n" +
                "\"category\": Choose between 'city,' 'nature,' or 'object.'\n" +
                "\"tag\": Select up to 3 tags from the provided list that best describe the sound. Each tag should be in ONE WORD.\n" +
                "\"intensity\": Describe as 'strong,' 'weak,' or 'none.'\n" +
                "\n" +
                "For 'music,' only include keywords related to genres, instruments, or background themes. NEVER SPECIFY category, tag, or intensity for 'music' elements. In 'music', YOU SHOULD ONLY CONTAIN PLAIN TEXT\n" +
                "Each key, 'music' and 'sound,' should contain 6 values. Format each element in the JSON to reflect this structured approach.\n" +
                "\n" +
                "You MUST ONLY return the final JSON message IN ENGLISH.";
        return generateChat(prompt, systemMessage);
    }

    public Map<String, Object> generateChat(String prompt, String systemMessage) {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4");
        body.put("messages", new Object[]{
                new HashMap<String, String>() {{
                    put("role", "system");
                    put("content", systemMessage);  // 시스템 메시지
                }},
                new HashMap<String, String>() {{
                    put("role", "user");
                    put("content", prompt);  // 사용자 입력 메시지
                }}
        });

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                // JSON 문자열을 JsonNode로 파싱
                JsonNode rootNode = objectMapper.readTree(response.getBody());

                // choices 배열의 첫 번째 객체에서 message의 content 부분 추출
                String content = rootNode
                        .path("choices")
                        .get(0)
                        .path("message")
                        .path("content")
                        .asText();

                // content를 다시 Map으로 변환
                return objectMapper.readValue(content, Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("JSON 파싱 오류: " + e.getMessage(), e);
            }
        } else {
            throw new RuntimeException("ChatGPT API 호출 실패: " + response.getStatusCode());
        }
    }
}
