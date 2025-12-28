package ru.kiyari.ai.bina.service;

import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.*;

@Service
@Slf4j
public class EnhancedVoiceActivationService {

    private final Porcupine porcupine;
    private final ExecutorService executorService;
    private final BlockingQueue<short[]> audioQueue;
    private volatile boolean isRunning = false;

    public EnhancedVoiceActivationService(Porcupine porcupine) {
        this.porcupine = porcupine;
        this.executorService = Executors.newFixedThreadPool(2);
        this.audioQueue = new LinkedBlockingQueue<>(1000);
    }

    public void start() {
        if (isRunning) {
            return;
        }

        isRunning = true;

        // Поток для захвата аудио
        executorService.submit(this::audioCaptureTask);

        // Поток для обработки аудио
        executorService.submit(this::audioProcessingTask);

        log.info("Enhanced voice activation service started");
    }

    public void stop() {
        isRunning = false;
        executorService.shutdown();
        porcupine.delete();
        log.info("Enhanced voice activation service stopped");
    }

    private void audioCaptureTask() {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            int frameSize = 512;
            byte[] buffer = new byte[frameSize * 2]; // 16-bit = 2 bytes per sample

            while (isRunning) {
                int bytesRead = line.read(buffer, 0, buffer.length);

                // Конвертируем byte[] в short[]
                short[] shortBuffer = new short[frameSize];
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);

                for (int i = 0; i < frameSize; i++) {
                    shortBuffer[i] = byteBuffer.getShort();
                }

                // Добавляем в очередь
                audioQueue.offer(shortBuffer, 100, TimeUnit.MILLISECONDS);
            }

            line.stop();
            line.close();

        } catch (Exception e) {
            log.error("Error in audio capture", e);
        }
    }

    private void audioProcessingTask() {
        while (isRunning) {
            try {
                short[] audioFrame = audioQueue.poll(100, TimeUnit.MILLISECONDS);

                if (audioFrame != null) {
                    int keywordIndex = porcupine.process(audioFrame);

                    if (keywordIndex >= 0) {
                        handleKeywordDetection(keywordIndex);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (PorcupineException e) {
                log.error("Error processing audio frame", e);
            }
        }
    }

    private void handleKeywordDetection(int keywordIndex) {
        log.info("Иди нахуй сука", keywordIndex);

        // Ваша логика здесь
        // Например, запуск диалога с ассистентом
        // или отправка события в систему
    }

    @PreDestroy
    public void cleanup() {
        stop();
    }
}
