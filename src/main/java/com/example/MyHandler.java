package com.example;

import com.example.util.HandlerInter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MyHandler implements HandlerInter {
    @Override
    public Object handle(String payload, Map<String, Object> headers) {
        return payload.toUpperCase();
    }
}
