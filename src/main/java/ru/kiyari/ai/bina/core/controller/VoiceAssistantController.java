package ru.kiyari.ai.bina.core.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kiyari.ai.bina.service.VoiceAssistantService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceAssistantController {

    private final VoiceAssistantService voiceAssistantService;

    @GetMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        try {
            voiceAssistantService.start();
            return ResponseEntity.ok(createResponse(
                    "started",
                    "Ассистент запущен",
                    voiceAssistantService.isRunning(),
                    voiceAssistantService.isListening()
            ));
        } catch (Exception e) {
            log.error("Ошибка запуска ассистента", e);
            return ResponseEntity.status(500).body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        voiceAssistantService.stop();
        return ResponseEntity.ok(createResponse(
                "stopped",
                "Ассистент остановлен",
                voiceAssistantService.isRunning(),
                voiceAssistantService.isListening()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(createResponse(
                "status",
                "Статус ассистента",
                voiceAssistantService.isRunning(),
                voiceAssistantService.isListening()
        ));
    }

    private Map<String, Object> createResponse(String status, String message,
                                               boolean isRunning, boolean isListening) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        response.put("isRunning", isRunning);
        response.put("isListening", isListening);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", error);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}