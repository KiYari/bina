package ru.kiyari.ai.bina.config;

import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Configuration
public class PicovoiceConfig {

    @Value("${picovoice.api-key}")
    private String apiKey;

    @Bean
    public Porcupine porcupine() throws PorcupineException, IOException {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("heybina.ppn");

        if (inputStream == null) {
            throw new FileNotFoundException("Keyword file not found: heybina.ppn");
        }

        File tempFile = File.createTempFile("keyword_", ".ppn");

        Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        tempFile.deleteOnExit();

        String filePath = tempFile.getAbsolutePath();

        return new Porcupine.Builder()
                .setAccessKey(apiKey)
                .setKeywordPaths(new String[]{filePath})
                .build();
    }
}