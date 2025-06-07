package ru.kiyari.ai.bina.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kiyari.ai.bina.service.CoreService;

@RestController
@RequiredArgsConstructor
public class HealthCheckController {
    private final CoreService coreService;

    @GetMapping("/hc")
    public String healthCheck() {
                return coreService.healthCheck();
    }
}
