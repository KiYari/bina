package ru.kiyari.ai.bina.core.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kiyari.ai.bina.service.EnhancedVoiceActivationService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceActivationController {

    private final EnhancedVoiceActivationService voiceService;

    @GetMapping("/start")
    public ResponseEntity<String> startListening() {
        try {
            voiceService.start();
            return ResponseEntity.ok("Voice activation started");
        } catch (Exception e) {
            log.error("Failed to start voice activation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start: " + e.getMessage());
        }
    }

    @GetMapping("/stop")
    public ResponseEntity<String> stopListening() {
        try {
            voiceService.stop();
            return ResponseEntity.ok("Voice activation stopped");
        } catch (Exception e) {
            log.error("Failed to stop voice activation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to stop: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "voice-activation");
        status.put("available", true);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/test-command")
    public ResponseEntity<Map<String, Object>> testCommand(@RequestBody Map<String, String> request) {
        String command = request.get("command");

        if (command == null || command.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Command is required")
            );
        }

        // Имитируем обработку команды
        log.info("Test command received: {}", command);

        Map<String, Object> response = new HashMap<>();
        response.put("command", command);
        response.put("processed", true);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}
