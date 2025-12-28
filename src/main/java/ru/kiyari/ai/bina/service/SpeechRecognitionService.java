package ru.kiyari.ai.bina.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
@Slf4j
public class SpeechRecognitionService {

    private final Model voskModel;
    private final ExecutorService executorService;
    private volatile boolean isRecognizing = false;

    public SpeechRecognitionService(Model voskModel) {
        this.voskModel = voskModel;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void startRecognition(Consumer<String> onResult) {
        if (isRecognizing) {
            return;
        }

        isRecognizing = true;

        executorService.submit(() -> {
            try (Recognizer recognizer = new Recognizer(voskModel, 16000.0f)) {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);

                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                log.info("Speech recognition started");

                byte[] buffer = new byte[4096];
                int bytesRead;

                while (isRecognizing) {
                    bytesRead = microphone.read(buffer, 0, buffer.length);

                    if (bytesRead > 0) {
                        if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                            // Финализированный результат
                            String result = recognizer.getResult();
                            JsonNode jsonNode = new ObjectMapper().readTree(result);
                            String text = jsonNode.get("text").asText();

                            if (!text.isEmpty()) {
                                onResult.accept(text);
                                log.info("Recognized: {}", text);
                            }
                        } else {
                            // Частичный результат
                            String partial = recognizer.getPartialResult();
                            log.debug("Partial: {}", partial);
                        }
                    }
                }

                microphone.stop();
                microphone.close();

            } catch (Exception e) {
                log.error("Error in speech recognition", e);
            }
        });
    }

    public void stopRecognition() {
        isRecognizing = false;
        log.info("Speech recognition stopped");
    }

    public String recognizeAudioFile(File audioFile) throws IOException, UnsupportedAudioFileException {
        try (Recognizer recognizer = new Recognizer(voskModel, 16000.0f);
             InputStream ais = AudioSystem.getAudioInputStream(
                     new AudioFormat(16000, 16, 1, true, false),
                     AudioSystem.getAudioInputStream(audioFile)
             )) {

            int nbytes;
            byte[] b = new byte[4096];

            while ((nbytes = ais.read(b)) >= 0) {
                if (recognizer.acceptWaveForm(b, nbytes)) {
                    // Продолжаем обработку
                }
            }

            // Получаем финальный результат
            String result = recognizer.getFinalResult();
            JsonNode jsonNode = new ObjectMapper().readTree(result);
            return jsonNode.get("text").asText();
        }
    }
}
