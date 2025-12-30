package ru.kiyari.ai.bina.core.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kiyari.ai.bina.client.AudioClient;
import ru.kiyari.ai.bina.service.AudioService;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audio")
@Slf4j
public class AudioController {

    private final AudioClient audioClient;
    private final AudioService audioService;

    @GetMapping("/play")
    public ResponseEntity<String> playAudio() {
        try {
            byte[] audioData = audioClient.getAudio("Привет, это тест синтеза речи, кстати я еблан и пидарас");

            new Thread(() -> {
                try {
                    audioService.playWavAudio(audioData);
                    log.info("Audio playback completed for ID:");
                } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                    log.error("Error playing audio: {}", e.getMessage(), e);
                }
            }).start();

            return ResponseEntity.ok("Audio playback started for ID: ");

        } catch (Exception e) {
            log.error("Error processing audio request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopPlayback() {
        audioService.stopPlayback();
        return ResponseEntity.ok("Playback stopped");
    }

    @GetMapping("/status")
    public ResponseEntity<Boolean> getPlaybackStatus() {
        return ResponseEntity.ok(audioService.isPlaying());
    }
}
