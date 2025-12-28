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

        // Вариант 1: Прямой путь к файлу
        File modelFile = new File(modelPath);
        log.info("Absolute path from File: {}", modelFile.getAbsolutePath());

        // Вариант 2: Путь относительно classpath
        Resource resource = new ClassPathResource(modelPath);
        log.info("ClassPathResource exists: {}", resource.exists());
        log.info("ClassPathResource URL: {}", resource.getURL());

        // Вариант 3: Путь относительно текущей директории
        Path currentDirPath = Paths.get("").toAbsolutePath();
        log.info("Current directory: {}", currentDirPath);

        // Проверяем несколько возможных расположений
        String[] possibleLocations = {
                modelPath, // как указано
                "src/main/resources/" + modelPath, // в исходниках
                "target/classes/" + modelPath, // в собранном проекте
                currentDirPath.resolve(modelPath).toString(), // относительно текущей директории
                System.getProperty("user.dir") + "/" + modelPath // абсолютный путь
        };

        for (String location : possibleLocations) {
            log.info("Checking location: {}", location);
            File locationFile = new File(location);
            if (locationFile.exists()) {
                log.info("Found model at: {}", locationFile.getAbsolutePath());

                // Выводим содержимое директории для отладки
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

        // Если ничего не найдено, создаем инструкцию
        throw new IOException("""
            ❌ Vosk model not found!
            
            Требуемый путь: %s
            
            Проверьте:
            1. Распакована ли модель в папку?
               - Исходный ZIP: vosk-model-ru-0.42.zip
               - После распаковки должна быть папка с содержимым
              
            2. Правильная структура папки модели:
               models/vosk/
               ├── am/
               │   ├── final.mdl
               │   └── ...
               ├── conf/
               │   ├── mfcc.conf
               │   └── ...
               ├── graph/
               │   ├── HCLr.fst
               │   └── ...
               └── ivector/
                   └── ...
            
            3. Расположение модели:
               - Разработка: src/main/resources/models/vosk/
               - Сборка: target/classes/models/vosk/
            
            4. Скачайте модель если отсутствует:
               wget https://alphacephei.com/vosk/models/vosk-model-ru-0.42.zip
               unzip vosk-model-ru-0.42.zip -d src/main/resources/models/
               переименуйте: vosk-model-ru-0.42 -> vosk
            """.formatted(modelPath));
    }

    @Bean
    public Recognizer voskRecognizer(Model model) throws IOException {
        return new Recognizer(model, 16000.0f);
    }
}
