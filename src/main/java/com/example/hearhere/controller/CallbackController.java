package com.example.hearhere.controller;

import com.example.hearhere.service.SunoAiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
@RequiredArgsConstructor
public class CallbackController {

    private final SunoAiService sunoAiService;

    @PostMapping("/callback/suno")
    public ResponseEntity<String> handleCallback(@RequestBody Map<String, Object> callbackData) {
        try {
            log.info("Received callback: {}", callbackData);

            // 1. 콜백 데이터 값 검증
            Map<String, Object> data = (Map<String, Object>) callbackData.get("data");
            if (data == null) {
                log.error("Callback data is null.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid callback data");
            }

            // 2. 콜백 타입으로 값 검증
            // 완료 이전에는 text, 완료 이후에는 complete
            String callbackType = (String) data.get("callbackType");
            String taskId = (String) data.get("task_id");

            log.info("Callback Type: {}, Task ID: {}", callbackType, taskId);

            // 3. 완료되었을 경우 데이터 처리
            if ("complete".equals(callbackType)) {
                sunoAiService.completeTask(taskId, data);
                log.info("완료: Task {} completed with data: {}", taskId, data);
            } else {
                log.info("미완료: Callback received for type: {}", callbackType);
            }
        } catch (Exception e) {
            log.error("콜백 처리에 문제 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Callback processing failed");
        }

        return ResponseEntity.ok("콜백 처리됨"); // 모든 콜백 처리에 작동
    }
}
