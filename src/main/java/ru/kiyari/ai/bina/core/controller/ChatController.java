package ru.kiyari.ai.bina.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/chat/ai")
@RequiredArgsConstructor
public class ChatController {
    private final DeepSeekChatModel model;

    private final Map<String, Boolean> activeStreams = new ConcurrentHashMap<>();

    @GetMapping(value = "/stream-simple", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamConnectionSimple(
            @RequestParam String message,
            @RequestParam String streamId) {

        activeStreams.put(streamId, true);

        Prompt prompt = new Prompt(new UserMessage(message));

        return model.stream(prompt)
                .map(chatResponse -> chatResponse.getResult().getOutput().getText())
                .map(chunk -> chunk.replace(" ", "\u00A0"))
                .takeWhile(chunk -> activeStreams.containsKey(streamId))
                .doOnComplete(() -> activeStreams.remove(streamId))
                .doOnCancel(() -> activeStreams.remove(streamId))
                .doOnError(error -> activeStreams.remove(streamId));
    }

    @PostMapping("/stop-stream")
    public ResponseEntity<Void> stopStream(@RequestParam String streamId) {
        activeStreams.remove(streamId);
        return ResponseEntity.ok().build();
    }
}