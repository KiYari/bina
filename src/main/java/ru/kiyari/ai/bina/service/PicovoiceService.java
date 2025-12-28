package ru.kiyari.ai.bina.service;

import ai.picovoice.porcupine.Porcupine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PicovoiceService {

    private final Porcupine porcupine;

    @Value("${assistant.wake-word.sensitivity:0.5}")
    private float wakeWordSensitivity;

    public boolean detectWakeWord(short[] audioFrame) {
        try {
            int keywordIndex = porcupine.process(audioFrame);
            return keywordIndex >= 0;
        } catch (Exception e) {
            log.error("Wake word detection error", e);
            return false;
        }
    }
}