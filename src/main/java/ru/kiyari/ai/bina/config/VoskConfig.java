package ru.kiyari.ai.bina.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class VoskConfig {

    @Value("${vosk.model-path:models/vosk}")
    private String modelPath;

    @Bean
    public Model voskModel() throws IOException {
        log.info("Attempting to load Vosk model from path: {}", modelPath);

        File modelFile = new File(modelPath);
        log.info("Absolute path from File: {}", modelFile.getAbsolutePath());

        Resource resource = new ClassPathResource(modelPath);
        log.info("ClassPathResource exists: {}", resource.exists());
        log.info("ClassPathResource URL: {}", resource.getURL());

        Path currentDirPath = Paths.get("").toAbsolutePath();
        log.info("Current directory: {}", currentDirPath);

        String[] possibleLocations = {
                modelPath,
                "src/main/resources/" + modelPath,
                "target/classes/" + modelPath,
                currentDirPath.resolve(modelPath).toString(),
                System.getProperty("user.dir") + "/" + modelPath
        };

        for (String location : possibleLocations) {
            log.info("Checking location: {}", location);
            File locationFile = new File(location);
            if (locationFile.exists()) {
                log.info("Found model at: {}", locationFile.getAbsolutePath());

                if (locationFile.isDirectory()) {
                    log.info("Directory contents:");
                    File[] files = locationFile.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            log.info("  - {} (dir: {})", file.getName(), file.isDirectory());
                        }
                    }
                }

                try {
                    Model model = new Model(location);
                    log.info("✅ Vosk model successfully loaded from: {}", location);
                    return model;
                } catch (IOException e) {
                    log.error("Failed to load model from {}: {}", location, e.getMessage());
                }
            } else {
                log.info("Location does not exist: {}", location);
            }
        }

        throw new IOException("❌ Vosk model not found!");
    }

    @Bean
    public Recognizer voskRecognizer(Model model) throws IOException {
        return new Recognizer(model, 16000.0f);
    }
}
