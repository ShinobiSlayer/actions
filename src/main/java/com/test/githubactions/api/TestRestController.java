package com.test.githubactions.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestRestController {

    @GetMapping("/test")
    public Map<String, String> get() {
        return Map.of("message", "Hello World8!");
    }
}
