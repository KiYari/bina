package ru.kiyari.ai.bina.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthCheckController {
    private final DeepSeekChatModel model;

    @GetMapping("/heart")
    public Mono<ResponseEntity<String>> heart() {
        return Mono.just(ResponseEntity.ok("I am working ❤"));
    }

    @GetMapping("/ai/connection")
    public Flux<ChatResponse> connection() {

        return model.stream(new Prompt(
                    new UserMessage("Привет! Расскажи о себе и что ты умеешь?")
                )
        );
    }

}
