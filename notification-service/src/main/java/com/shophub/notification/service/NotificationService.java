package com.shophub.notification.service;

import com.shophub.notification.entity.Notification;
import com.shophub.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 通知服务
 * 
 * @author ShopHub Team
 * @since Week 8
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * 创建通知
     */
    public Notification createNotification(Long userId, String title, String message, 
                                         Notification.NotificationType type, 
                                         String eventType, String eventData) {
        try {
            Notification notification = new Notification(userId, title, message, type);
            notification.setEventType(eventType);
            notification.setEventData(eventData);
            
            Notification saved = notificationRepository.save(notification);
            logger.info("Created notification: {} for user: {}", saved.getId(), userId);
            
            // 异步发送通知
            sendNotificationAsync(saved);
            
            return saved;
        } catch (Exception e) {
            logger.error("Error creating notification for user: {}", userId, e);
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    /**
     * 根据ID获取通知
     */
    @Transactional(readOnly = true)
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    /**
     * 获取用户的所有通知
     */
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取用户的未读通知
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    /**
     * 获取用户未读通知数量
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * 获取用户最近的通知
     */
    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findRecentByUserId(userId, pageable);
    }

    /**
     * 标记通知为已读
     */
    public boolean markAsRead(Long notificationId, Long userId) {
        try {
            Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
            if (optionalNotification.isPresent()) {
                Notification notification = optionalNotification.get();
                
                // 验证通知属于该用户
                if (!notification.getUserId().equals(userId)) {
                    logger.warn("User {} attempted to mark notification {} as read, but it belongs to user {}", 
                               userId, notificationId, notification.getUserId());
                    return false;
                }
                
                if (!notification.isRead()) {
                    notification.markAsRead();
                    notificationRepository.save(notification);
                    logger.info("Marked notification {} as read for user {}", notificationId, userId);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error marking notification {} as read for user {}", notificationId, userId, e);
            return false;
        }
    }

    /**
     * 标记用户所有通知为已读
     */
    public int markAllAsRead(Long userId) {
        try {
            List<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(userId);
            int count = 0;
            
            for (Notification notification : unreadNotifications) {
                notification.markAsRead();
                notificationRepository.save(notification);
                count++;
            }
            
            logger.info("Marked {} notifications as read for user {}", count, userId);
            return count;
        } catch (Exception e) {
            logger.error("Error marking all notifications as read for user {}", userId, e);
            return 0;
        }
    }

    /**
     * 删除通知
     */
    public boolean deleteNotification(Long notificationId, Long userId) {
        try {
            Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
            if (optionalNotification.isPresent()) {
                Notification notification = optionalNotification.get();
                
                // 验证通知属于该用户
                if (!notification.getUserId().equals(userId)) {
                    logger.warn("User {} attempted to delete notification {}, but it belongs to user {}", 
                               userId, notificationId, notification.getUserId());
                    return false;
                }
                
                notificationRepository.delete(notification);
                logger.info("Deleted notification {} for user {}", notificationId, userId);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error deleting notification {} for user {}", notificationId, userId, e);
            return false;
        }
    }

    /**
     * 异步发送通知
     */
    @Async
    public void sendNotificationAsync(Notification notification) {
        try {
            // 模拟发送通知的过程
            Thread.sleep(100); // 模拟网络延迟
            
            // 这里可以集成实际的通知发送服务，如：
            // - 邮件服务
            // - 短信服务
            // - 推送通知服务
            // - WebSocket 实时通知
            
            notification.markAsSent();
            notificationRepository.save(notification);
            
            logger.info("Sent notification {} to user {}", notification.getId(), notification.getUserId());
        } catch (Exception e) {
            logger.error("Error sending notification {}", notification.getId(), e);
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notificationRepository.save(notification);
        }
    }

    /**
     * 获取通知统计信息
     */
    @Transactional(readOnly = true)
    public NotificationStats getNotificationStats() {
        try {
            List<Object[]> statusCounts = notificationRepository.countByStatus();
            List<Object[]> typeCounts = notificationRepository.countByType();
            
            NotificationStats stats = new NotificationStats();
            
            for (Object[] row : statusCounts) {
                Notification.NotificationStatus status = (Notification.NotificationStatus) row[0];
                Long count = (Long) row[1];
                stats.addStatusCount(status, count);
            }
            
            for (Object[] row : typeCounts) {
                Notification.NotificationType type = (Notification.NotificationType) row[0];
                Long count = (Long) row[1];
                stats.addTypeCount(type, count);
            }
            
            return stats;
        } catch (Exception e) {
            logger.error("Error getting notification stats", e);
            return new NotificationStats();
        }
    }

    /**
     * 定期清理已读的旧通知
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupOldNotifications() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30); // 保留30天
            notificationRepository.deleteReadNotificationsOlderThan(cutoffTime);
            logger.info("Cleaned up old notifications older than {}", cutoffTime);
        } catch (Exception e) {
            logger.error("Error cleaning up old notifications", e);
        }
    }

    /**
     * 通知统计信息类
     */
    public static class NotificationStats {
        private java.util.Map<Notification.NotificationStatus, Long> statusCounts = new java.util.HashMap<>();
        private java.util.Map<Notification.NotificationType, Long> typeCounts = new java.util.HashMap<>();

        public void addStatusCount(Notification.NotificationStatus status, Long count) {
            statusCounts.put(status, count);
        }

        public void addTypeCount(Notification.NotificationType type, Long count) {
            typeCounts.put(type, count);
        }

        public java.util.Map<Notification.NotificationStatus, Long> getStatusCounts() {
            return statusCounts;
        }

        public java.util.Map<Notification.NotificationType, Long> getTypeCounts() {
            return typeCounts;
        }
    }
}