package ru.kiyari.ai.bina.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class AudioService {


    private static final int SAMPLE_RATE = 16000;
    private static final int FRAME_LENGTH = 512;
    private static final int BUFFER_SIZE = 4096;

    private TargetDataLine microphone;
    private SourceDataLine speaker;
    private final AtomicBoolean isMicrophoneInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);

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

    public void playWavAudio(byte[] audioData) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        if (isPlaying.get()) {
            log.warn("Audio is already playing");
            return;
        }

        isPlaying.set(true);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
             AudioInputStream audioStream = AudioSystem.getAudioInputStream(bais)) {

            AudioFormat format = audioStream.getFormat();
            log.info("Audio format: {}, channels: {}, sample rate: {}, sample size: {} bits",
                    format.getEncoding(), format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits());

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                log.error("Line not supported for format: {}", format);
                throw new LineUnavailableException("Audio format not supported");
            }

            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(format);
            speaker.start();

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while (isPlaying.get() && (bytesRead = audioStream.read(buffer)) != -1) {
                speaker.write(buffer, 0, bytesRead);
            }

            speaker.drain();
            speaker.stop();

        } finally {
            if (speaker != null) {
                speaker.close();
                speaker = null;
            }
            isPlaying.set(false);
        }
    }

    public void playWavAudio(InputStream audioStream) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        if (isPlaying.get()) {
            log.warn("Audio is already playing");
            return;
        }

        isPlaying.set(true);

        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioStream)) {

            AudioFormat format = audioInputStream.getFormat();
            log.info("Audio format: {}, channels: {}, sample rate: {}, sample size: {} bits",
                    format.getEncoding(), format.getChannels(),
                    format.getSampleRate(), format.getSampleSizeInBits());

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                log.error("Line not supported for format: {}", format);
                throw new LineUnavailableException("Audio format not supported");
            }

            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(format);
            speaker.start();

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while (isPlaying.get() && (bytesRead = audioInputStream.read(buffer)) != -1) {
                speaker.write(buffer, 0, bytesRead);
            }

            speaker.drain();
            speaker.stop();

        } finally {
            if (speaker != null) {
                speaker.close();
                speaker = null;
            }
            isPlaying.set(false);
        }
    }

    public void stopPlayback() {
        isPlaying.set(false);
        if (speaker != null) {
            speaker.stop();
            speaker.close();
            speaker = null;
        }
        log.info("Playback stopped");
    }

    public boolean isPlaying() {
        return isPlaying.get();
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