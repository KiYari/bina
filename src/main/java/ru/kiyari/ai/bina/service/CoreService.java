package ru.kiyari.ai.bina.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.internal.OpenAiClient;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kiyari.ai.bina.config.ModelSettings;

import java.util.List;

@Service
@AllArgsConstructor
public class CoreService {
    private final OpenAiChatModel model;
    private final OpenAiClient client;

    List<ChatMessage> messages;

    @PostConstruct
    void init() {
        messages.add(new SystemMessage(ModelSettings.SYSTEM_ROLE));
    }

    public String healthCheck() {
        messages.add(new UserMessage("Привет! Кто сильнее гей лев или лев с фимозом?"));
        ChatResponse modelResponse = model.chat(messages);

        return modelResponse.aiMessage().text();
    }

}
