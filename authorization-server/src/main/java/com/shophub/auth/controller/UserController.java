package com.shophub.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 * 
 * 提供用户信息查询和认证相关的API端点
 * 
 * @author ShopHub Team
 * @since Week 8
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class UserController {

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Authorization Server");
        health.put("timestamp", Instant.now());
        health.put("version", "1.0.0");
        return ResponseEntity.ok(health);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/userinfo")
    public ResponseEntity<Map<String, Object>> getUserInfo(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", authentication.getName());
        userInfo.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        userInfo.put("authenticated", authentication.isAuthenticated());
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            userInfo.put("subject", jwt.getSubject());
            userInfo.put("issuer", jwt.getIssuer());
            userInfo.put("issuedAt", jwt.getIssuedAt());
            userInfo.put("expiresAt", jwt.getExpiresAt());
            userInfo.put("scopes", jwt.getClaimAsStringList("scope"));
        }
        
        return ResponseEntity.ok(userInfo);
    }

    /**
     * 获取用户资料
     */
    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @PathVariable String userId,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        // 简单的用户资料模拟
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", userId);
        profile.put("username", "user" + userId);
        profile.put("email", "user" + userId + "@shophub.com");
        profile.put("firstName", "User");
        profile.put("lastName", userId);
        profile.put("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        profile.put("lastLogin", Instant.now().minusSeconds(3600));
        profile.put("active", true);
        
        return ResponseEntity.ok(profile);
    }

    /**
     * 验证令牌
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            result.put("valid", false);
            result.put("message", "Token is invalid or expired");
            return ResponseEntity.status(401).body(result);
        }

        result.put("valid", true);
        result.put("username", authentication.getName());
        result.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        result.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户权限
     */
    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<Map<String, Object>> getUserPermissions(
            @PathVariable String userId,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> permissions = new HashMap<>();
        permissions.put("userId", userId);
        permissions.put("permissions", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        
        // 基于角色的权限映射
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        permissions.put("canRead", true);
        permissions.put("canWrite", isAdmin);
        permissions.put("canDelete", isAdmin);
        permissions.put("canManageUsers", isAdmin);
        permissions.put("canAccessAdmin", isAdmin);
        
        return ResponseEntity.ok(permissions);
    }
}