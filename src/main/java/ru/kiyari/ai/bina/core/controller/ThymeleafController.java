package ru.kiyari.ai.bina.core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ThymeleafController {
    @GetMapping("/chat")
    public String chatPage(Model model) {
        model.addAttribute("defaultMessage", "Привет!");
        return "ai-chat";
    }
}
