package ru.kiyari.ai.bina.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "audio-service", url = "${tts.url}")
public interface AudioClient {

    @PostMapping(value = "/say", produces = "audio/wav")
    byte[] getAudio(@RequestBody String text);
}
