package com.chatji.chatji.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final Map<String, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String userId) {
        SseEmitter emitter = new SseEmitter(60 * 1000L * 60); 
        userEmitters.put(userId, emitter);

        emitter.onCompletion(() -> userEmitters.remove(userId));
        emitter.onTimeout(() -> userEmitters.remove(userId));
        emitter.onError((e) -> userEmitters.remove(userId));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected for: " + userId));
        } catch (IOException e) {
            userEmitters.remove(userId);
        }

        return emitter;
    }

    /**
     * v30.1: 실시간 연결 유지를 위한 하트비트 (30초마다 전송)
     */
    @Scheduled(fixedDelay = 30000)
    public void sendHeartbeat() {
        userEmitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (IOException e) {
                userEmitters.remove(id);
            }
        });
    }

    public static void broadcast(String name, Object data) {
        userEmitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(name).data(data));
            } catch (IOException e) {
                userEmitters.remove(id);
            }
        });
    }

    public static void sendToUser(String userId, String name, Object data) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(name).data(data));
            } catch (IOException e) {
                userEmitters.remove(userId);
            }
        }
    }
}
