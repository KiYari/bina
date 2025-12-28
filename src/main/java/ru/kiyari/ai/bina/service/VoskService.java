package ru.kiyari.ai.bina.service.speech;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoskService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Recognizer recognizer;

    private final Model voskModel;

    public String recognizeAudio(byte[] audioData) {
        if (voskModel == null || audioData == null || audioData.length == 0) {
            log.warn("UNSTABLE_AUDIO");
            return "";
        }

        try {
            if (recognizer.acceptWaveForm(audioData, audioData.length)) {
                String result = recognizer.getResult();
                JsonNode jsonNode = OBJECT_MAPPER.readTree(result);
                return jsonNode.get("text").asText();
            }
        } catch (Exception e) {
            log.error("Ошибка распознавания: {}", e.getMessage());
        }

        return "";
    }
}