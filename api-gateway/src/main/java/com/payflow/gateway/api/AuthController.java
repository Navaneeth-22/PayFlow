package com.payflow.gateway.api;

import com.payflow.gateway.config.RouteRegistry;
import com.payflow.gateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtUtil jwtUtil;
    private final RouteRegistry routeRegistry;

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> generateToken(
            @RequestParam String userId) {
        String token = jwtUtil.generateToken(userId);
        log.info("Test token generated for userId: {}", userId);
        return ResponseEntity.ok(Map.of(
                "token",  token,
                "userId", userId,
                "type",   "Bearer"
        ));
    }

    @GetMapping("/routes")
    public ResponseEntity<Object> routes() {
        return ResponseEntity.ok(
                Map.of("routes", routeRegistry.getAllRoutes())
        );
    }
}