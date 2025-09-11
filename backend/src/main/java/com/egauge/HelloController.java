package com.egauge;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        return "¡Hola! Tu backend Spring Boot está funcionando 🚀";
    }
}
