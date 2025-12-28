package ru.kiyari.ai.bina.service;

import ai.picovoice.porcupine.Porcupine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnhancedVoiceActivationService {

    private final Porcupine porcupine;
    private final Model voskModel;
    private final ExecutorService executorService;

    // Состояние сервиса
    private final AtomicBoolean isWakeWordDetectionActive = new AtomicBoolean(false);
    private final AtomicBoolean isSpeechRecognitionActive = new AtomicBoolean(false);
    private final AtomicBoolean isProcessingCommand = new AtomicBoolean(false);

    // Очередь для аудио данных
    private final BlockingQueue<short[]> audioQueue = new LinkedBlockingQueue<>();

    // Аудио линия для захвата
    private TargetDataLine microphone;

    // Константы
    private static final int SAMPLE_RATE = 16000;
    private static final int FRAME_LENGTH = 512; // Для Porcupine
    private static final int VOSK_BUFFER_SIZE = 4096; // Для Vosk

    private Model vosk;

    @Value("${assistant.wake-word.sensitivity:0.5}")
    private float wakeWordSensitivity;

    @Value("${assistant.command-timeout-seconds:5}")
    private int commandTimeoutSeconds;

    private Consumer<String> commandHandler = this::defaultCommandHandler;

    /**
     * Запуск голосового ассистента
     */
    public void start() {
        if (isWakeWordDetectionActive.get()) {
            log.warn("Voice assistant is already running");
            return;
        }

        isWakeWordDetectionActive.set(true);

        // Запускаем обнаружение wake word
        executorService.submit(this::wakeWordDetectionLoop);

        log.info("Voice Assistant started. Say 'Hey Bina' to activate");
    }

    /**
     * Остановка голосового ассистента
     */
    public void stop() {
        isWakeWordDetectionActive.set(false);
        isSpeechRecognitionActive.set(false);
        isProcessingCommand.set(false);

        closeMicrophone();
        audioQueue.clear();

        log.info("Voice Assistant stopped");
    }

    /**
     * Установка обработчика команд
     */
    public void setCommandHandler(Consumer<String> handler) {
        this.commandHandler = handler;
    }

    /**
     * Основной цикл обнаружения wake word
     */
    private void wakeWordDetectionLoop() {
        try {
            initializeMicrophone();

            byte[] buffer = new byte[FRAME_LENGTH * 2];

            while (isWakeWordDetectionActive.get()) {
                // Если идет распознавание команды, пропускаем wake word detection
                if (isSpeechRecognitionActive.get() || isProcessingCommand.get()) {
                    Thread.sleep(100);
                    continue;
                }

                int bytesRead = microphone.read(buffer, 0, buffer.length);

                if (bytesRead > 0) {
                    // Конвертируем в short[] для Porcupine
                    short[] shortBuffer = convertToShortArray(buffer, FRAME_LENGTH);

                    // Проверяем wake word
                    int keywordIndex = porcupine.process(shortBuffer);

                    if (keywordIndex >= 0) {
                        log.info("🎤 Wake word detected! Keyword index: {}", keywordIndex);
                        onWakeWordDetected();
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error in wake word detection", e);
        } finally {
            closeMicrophone();
        }
    }

    /**
     * Обработка обнаружения wake word
     */
    private void onWakeWordDetected() {
        if (isSpeechRecognitionActive.get() || isProcessingCommand.get()) {
            return;
        }

        log.info("🎵 Listening for command...");

        // Запускаем распознавание команды
        executorService.submit(this::recognizeCommand);
    }

    /**
     * Распознавание голосовой команды
     */
    private void recognizeCommand() {
        if (voskModel == null) {
            log.error("Vosk model not loaded, cannot recognize speech");
            return;
        }

        isSpeechRecognitionActive.set(true);

        try (Recognizer recognizer = new Recognizer(voskModel, SAMPLE_RATE)) {
            log.info("Starting speech recognition...");

            // Сигнал о начале прослушивания (можно добавить звуковой сигнал)
            playActivationSound();

            ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
            long startTime = System.currentTimeMillis();
            boolean commandReceived = false;

            // Собираем аудио в течение timeout секунд
            while (System.currentTimeMillis() - startTime < commandTimeoutSeconds * 1000L
                    && isSpeechRecognitionActive.get()) {

                byte[] buffer = new byte[VOSK_BUFFER_SIZE];
                int bytesRead = microphone.read(buffer, 0, buffer.length);

                if (bytesRead > 0) {
                    // Сохраняем для распознавания
                    audioBuffer.write(buffer, 0, bytesRead);

                    // Отправляем в Vosk
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        String result = recognizer.getResult();
                        JsonNode jsonNode = new ObjectMapper().readTree(result);
                        String text = jsonNode.get("text").asText();

                        if (!text.trim().isEmpty()) {
                            log.info("Recognized: {}", text);
                            processRecognizedCommand(text);
                            commandReceived = true;
                            break;
                        }
                    } else {
                        // Частичный результат (опционально)
                        String partial = recognizer.getPartialResult();
                        if (!partial.contains("\"partial\" : \"\"")) {
                            log.debug("Partial recognition: {}", partial);
                        }
                    }
                }
            }

            // Если команда не получена, пробуем получить финальный результат
            if (!commandReceived) {
                String finalResult = recognizer.getFinalResult();
                JsonNode jsonNode = new ObjectMapper().readTree(finalResult);
                String text = jsonNode.get("text").asText();

                if (!text.trim().isEmpty()) {
                    log.info("Final recognition: {}", text);
                    processRecognizedCommand(text);
                } else {
                    log.info("No command recognized");
                    // Можно воспроизвести "Не понял" звук
                }
            }

        } catch (Exception e) {
            log.error("Error in speech recognition", e);
        } finally {
            isSpeechRecognitionActive.set(false);
            log.info("Speech recognition completed");
        }
    }

    /**
     * Обработка распознанной команды
     */
    private void processRecognizedCommand(String command) {
        isProcessingCommand.set(true);

        try {
            log.info("📝 Processing command: '{}'", command);

            // Вызываем обработчик команды
            commandHandler.accept(command);

            // Добавляем небольшую задержку перед возвратом к wake word detection
            Thread.sleep(1000);

        } catch (Exception e) {
            log.error("Error processing command: {}", command, e);
        } finally {
            isProcessingCommand.set(false);
        }
    }

    /**
     * Обработчик команд по умолчанию
     */
    private void defaultCommandHandler(String command) {
        String lowerCommand = command.toLowerCase();

        // Примеры обработки команд
        if (lowerCommand.contains("привет") || lowerCommand.contains("здравствуй")) {
            log.info("👋 Привет! Чем могу помочь?");
            // textToSpeech.speak("Привет! Чем могу помочь?");

        } else if (lowerCommand.contains("пока") || lowerCommand.contains("до свидания")) {
            log.info("👋 До свидания!");
            // textToSpeech.speak("До свидания!");

        } else if (lowerCommand.contains("время")) {
            String currentTime = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            );
            log.info("⏰ Сейчас {} часов", currentTime);
            // textToSpeech.speak("Сейчас " + currentTime + " часов");

        } else if (lowerCommand.contains("погода")) {
            log.info("🌤 Запрос погоды получен");
            // weatherService.getWeather();

        } else if (lowerCommand.contains("новости")) {
            log.info("📰 Запрос новостей получен");
            // newsService.getLatestNews();

        } else if (lowerCommand.contains("расскажи шутку")) {
            log.info("😄 Запрос шутки получен");
            // jokeService.tellJoke();

        } else {
            log.info("🤔 Не понял команду: {}", command);
            // textToSpeech.speak("Извините, я не понял команду");
        }
    }

    /**
     * Воспроизведение звука активации (опционально)
     */
    private void playActivationSound() {
        // Можно добавить короткий звуковой сигнал
        // Например, использовать Java Sound API или внешнюю библиотеку
        log.debug("Playing activation sound");
    }

    /**
     * Инициализация микрофона
     */
    private void initializeMicrophone() throws LineUnavailableException {
        if (microphone != null && microphone.isOpen()) {
            return;
        }

        AudioFormat format = new AudioFormat(
                SAMPLE_RATE,
                16,     // бит на сэмпл
                1,      // моно
                true,   // signed
                false   // little-endian
        );

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();

        log.info("Microphone initialized with format: {}", format);
    }

    /**
     * Закрытие микрофона
     */
    private void closeMicrophone() {
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
            log.info("Microphone closed");
        }
    }

    /**
     * Конвертация byte[] в short[]
     */
    private short[] convertToShortArray(byte[] byteArray, int frameSize) {
        short[] shortArray = new short[frameSize];
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < frameSize && byteBuffer.hasRemaining(); i++) {
            shortArray[i] = byteBuffer.getShort();
        }

        return shortArray;
    }

    /**
     * Проверка состояния сервиса
     */
    public boolean isRunning() {
        return isWakeWordDetectionActive.get();
    }

    /**
     * Проверка, идет ли распознавание команды
     */
    public boolean isListeningForCommand() {
        return isSpeechRecognitionActive.get();
    }

    @PreDestroy
    public void cleanup() {
        stop();
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (voskModel != null) {
            voskModel.close();
        }

        porcupine.delete();

        log.info("Voice Assistant Service cleaned up");
    }
}