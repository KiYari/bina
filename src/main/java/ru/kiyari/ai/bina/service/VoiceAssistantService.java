package ru.kiyari.ai.bina.service;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.kiyari.ai.bina.service.speech.VoskService;

import javax.sound.sampled.LineUnavailableException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceAssistantService {

    private final AudioService audioService;
    private final VoskService voskService;
    private final PicovoiceService picovoiceService;
    private final ExecutorService executorService;

    private final AtomicBoolean isActive = new AtomicBoolean(false);
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    @Value("${assistant.command-timeout-seconds:5}")
    private int commandTimeoutSeconds;

    @Setter
    private Consumer<String> commandHandler = this::defaultCommandHandler;

    public void start() {
        if (isActive.get()) {
            log.warn("Ассистент уже запущен");
            return;
        }

        try {
            audioService.initializeMicrophone();
            isActive.set(true);
            executorService.submit(this::wakeWordDetectionLoop);
            log.info("🎙️ Голосовой ассистент запущен. Скажите 'Hey Bina'");
        } catch (LineUnavailableException e) {
            log.error("Не удалось инициализировать микрофон: {}", e.getMessage());
            stop();
        }
    }

    public void stop() {
        log.info("Остановка ассистента...");
        isActive.set(false);
        isListening.set(false);
        isProcessing.set(false);
        audioService.closeMicrophone();
        log.info("✅ Ассистент остановлен");
    }

    public boolean isRunning() {
        return isActive.get();
    }

    public boolean isListening() {
        return isListening.get();
    }

    private void wakeWordDetectionLoop() {
        log.info("🔍 Запуск обнаружения wake word...");

        while (isActive.get()) {
            try {
                if (isListening.get() || isProcessing.get()) {
                    Thread.sleep(50);
                    continue;
                }

                short[] audioFrame = audioService.readAudioFrameAsShort(AudioService.SAMPLE_RATE);

                if (audioFrame.length > 0 && picovoiceService.detectWakeWord(audioFrame)) {
                    log.info("🎯 Wake word обнаружен!");
                    onWakeWordDetected();
                }

                Thread.sleep(10);
            } catch (Exception e) {
                log.error("Ошибка в обнаружении wake word: {}", e.getMessage());
            }
        }

        log.info("⏹️ Цикл обнаружения wake word завершен");
    }

    private void onWakeWordDetected() {
        if (isListening.get() || isProcessing.get()) {
            return;
        }

        executorService.submit(() -> {
            try {
                recognizeCommand();
            } catch (Exception e) {
                log.error("Ошибка при распознавании команды: {}", e.getMessage());
            }
        });
    }

    private void recognizeCommand() {
        isListening.set(true);

        try {
            long startTime = System.currentTimeMillis();
            long timeoutMillis = commandTimeoutSeconds * 1000L;

            audioService.readAudioFrame(4096);

            while (System.currentTimeMillis() - startTime < timeoutMillis
                    && isListening.get() && !isProcessing.get()) {

                byte[] audioData = audioService.readAudioFrame(4096);

                if (audioData.length > 0) {
                    String recognizedText = voskService.recognizeAudio(audioData);

                    if (StringUtils.isNotBlank(recognizedText)) {
                        log.info("📝 Распознано: '{}'", recognizedText);
                        processCommand(recognizedText);
                        break;
                    }
                }

                Thread.sleep(20);
            }

            if (!isProcessing.get()) {
                log.info("⏱️ Время прослушивания истекло");
            }

        } catch (Exception e) {
            log.error("Ошибка при распознавании команды: {}", e.getMessage());
        } finally {
            isListening.set(false);
            log.info("👂 Прослушивание завершено");
        }
    }

    private void processCommand(String command) {
        if (isProcessing.get()) {
            return;
        }

        isProcessing.set(true);

        try {
            log.info("⚙️ Обработка команды: '{}'", command);
            commandHandler.accept(command);

            Thread.sleep(1000);

        } catch (Exception e) {
            log.error("Ошибка обработки команды: {}", e.getMessage());
        } finally {
            isProcessing.set(false);
        }
    }

    private void defaultCommandHandler(String command) {
        String lowerCommand = command.toLowerCase();

        if (lowerCommand.contains("привет") || lowerCommand.contains("здравствуй")) {
            log.info("👋 Привет! Чем могу помочь?");
        } else if (lowerCommand.contains("пока") || lowerCommand.contains("до свидания")) {
            log.info("👋 До свидания!");
        } else if (lowerCommand.contains("время")) {
            String time = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            );
            log.info("⏰ Сейчас {} часов", time);
        } else if (lowerCommand.contains("погода")) {
            log.info("🌤 Запрос погоды получен");
        } else if (lowerCommand.contains("новости")) {
            log.info("📰 Запрос новостей получен");
        } else if (lowerCommand.contains("шутк")) {
            log.info("😄 Запрос шутки получен");
        } else {
            log.info("🤔 Не понял команду: '{}'", command);
        }
    }
}