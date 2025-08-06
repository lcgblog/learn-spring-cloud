package com.shophub.notification.controller;

import com.shophub.notification.entity.Notification;
import com.shophub.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通知控制器
 * 
 * @author ShopHub Team
 * @since Week 8
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "notification-service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户的所有通知
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Notification>> getUserNotifications(Authentication authentication) {
        try {
            Long userId = getUserIdFromToken(authentication);
            List<Notification> notifications = notificationService.getUserNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("Error getting user notifications", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取当前用户的未读通知
     */
    @GetMapping("/unread")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Notification>> getUnreadNotifications(Authentication authentication) {
        try {
            Long userId = getUserIdFromToken(authentication);
            List<Notification> notifications = notificationService.getUnreadNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("Error getting unread notifications", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取当前用户的未读通知数量
     */
    @GetMapping("/unread/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        try {
            Long userId = getUserIdFromToken(authentication);
            long count = notificationService.getUnreadCount(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("unreadCount", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting unread count", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取当前用户最近的通知
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Notification>> getRecentNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Long userId = getUserIdFromToken(authentication);
            List<Notification> notifications = notificationService.getRecentNotifications(userId, limit);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            logger.error("Error getting recent notifications", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据ID获取通知详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Notification> getNotification(
            @PathVariable Long id, 
            Authentication authentication) {
        try {
            Long userId = getUserIdFromToken(authentication);
            Optional<Notification> notification = notificationService.getNotificationById(id);
            
            if (notification.isPresent()) {
                // 验证通知属于当前用户
                if (notification.get().getUserId().equals(userId)) {
                    return ResponseEntity.ok(notification.get());
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting notification: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long id, 
            Authentication authentication) {
        try {
            Long userId = getUserIdFromToken(authentication);
            boolean success = notificationService.markAsRead(id, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("notificationId", id);
            
            if (success) {
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "通知不存在或无权限");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Error marking notification as read: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        try {
            Long userId = getUserIdFromToken(authentication);
            int count = notificationService.markAllAsRead(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("markedCount", count);
            response.put("message", String.format("已标记 %d 条通知为已读", count));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error marking all notifications as read", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @PathVariable Long id, 
            Authentication authentication) {
        try {
            Long userId = getUserIdFromToken(authentication);
            boolean success = notificationService.deleteNotification(id, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("notificationId", id);
            
            if (success) {
                response.put("message", "通知已删除");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "通知不存在或无权限");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Error deleting notification: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取通知统计信息（管理员权限）
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationService.NotificationStats> getNotificationStats() {
        try {
            NotificationService.NotificationStats stats = notificationService.getNotificationStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting notification stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 测试通知创建（开发环境）
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createTestNotification(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "测试通知") String title,
            @RequestParam(defaultValue = "这是一条测试通知") String message,
            Authentication authentication) {
        try {
            if (userId == null) {
                userId = getUserIdFromToken(authentication);
            }
            
            Notification notification = notificationService.createNotification(
                userId, title, message, 
                Notification.NotificationType.SYSTEM, 
                "TEST_EVENT", "{\"test\": true}"
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("notification", notification);
            response.put("message", "测试通知已创建");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating test notification", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 从JWT令牌中提取用户ID
     */
    private Long getUserIdFromToken(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userIdStr = jwt.getClaimAsString("user_id");
            if (userIdStr != null) {
                return Long.parseLong(userIdStr);
            }
            // 如果没有user_id claim，使用subject作为fallback
            String subject = jwt.getSubject();
            if (subject != null && subject.matches("\\d+")) {
                return Long.parseLong(subject);
            }
        }
        // 默认返回1（用于开发测试）
        return 1L;
    }
}