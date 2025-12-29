package ru.kiyari.ai.bina.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class AudioService {


    private static final int SAMPLE_RATE = 16000;
    private static final int FRAME_LENGTH = 512;

    private TargetDataLine microphone;
    private final AtomicBoolean isMicrophoneInitialized = new AtomicBoolean(false);

    public void initializeMicrophone() throws LineUnavailableException {
        if (isMicrophoneInitialized.get()) {
            return;
        }

        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            isMicrophoneInitialized.set(true);
            log.info("Microphone initialized successfully");
        } catch (LineUnavailableException e) {
            log.error("Failed to initialize microphone: {}", e.getMessage());
            isMicrophoneInitialized.set(false);
            throw e;
        }
    }

    public byte[] readAudioFrame(int bufferSize) {
        if (!isMicrophoneInitialized.get() || microphone == null) {
            log.warn("Microphone not initialized");
            return new byte[0];
        }

        try {
            byte[] buffer = new byte[bufferSize];
            int bytesRead = microphone.read(buffer, 0, buffer.length);

            if (bytesRead <= 0) {
                return new byte[0];
            }

            // Обрезаем массив до фактически прочитанных байт
            byte[] result = new byte[bytesRead];
            System.arraycopy(buffer, 0, result, 0, bytesRead);
            return result;
        } catch (Exception e) {
            log.error("Error reading audio frame: {}", e.getMessage());
            return new byte[0];
        }
    }

    public short[] readAudioFrameAsShort(int frameSize) {
        byte[] bytes = readAudioFrame(frameSize * 2);
        if (bytes.length == 0) {
            return new short[0];
        }
        return convertToShortArray(bytes, Math.min(frameSize, bytes.length / 2));
    }

    public short[] convertToShortArray(byte[] byteArray, int frameSize) {
        short[] shortArray = new short[frameSize];
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < frameSize && byteBuffer.hasRemaining(); i++) {
            shortArray[i] = byteBuffer.getShort();
        }

        return shortArray;
    }

    public void closeMicrophone() {
        if (microphone != null) {
            try {
                microphone.stop();
                microphone.close();
            } catch (Exception e) {
                log.warn("Error closing microphone: {}", e.getMessage());
            }
            microphone = null;
        }
        isMicrophoneInitialized.set(false);
        log.info("Microphone closed");
    }

    public boolean isMicrophoneOpen() {
        return microphone != null && microphone.isOpen() && isMicrophoneInitialized.get();
    }

    public static int getSampleRate() {
        return SAMPLE_RATE;
    }

    public static int getFrameLength() {
        return FRAME_LENGTH;
    }
}