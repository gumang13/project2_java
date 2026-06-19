package com.setting.control;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ping {

    @GetMapping("/ping")
    public Map<String,String> png(){
        return Map.of("message","pong");
    }
}
