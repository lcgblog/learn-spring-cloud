package com.shophub.notification.repository;

import com.shophub.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知数据访问接口
 * 
 * @author ShopHub Team
 * @since Week 8
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 根据用户ID查找通知
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 根据用户ID和状态查找通知
     */
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Notification.NotificationStatus status);

    /**
     * 根据用户ID查找未读通知
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * 统计用户未读通知数量
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL")
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 根据通知类型查找通知
     */
    List<Notification> findByTypeOrderByCreatedAtDesc(Notification.NotificationType type);

    /**
     * 根据事件类型查找通知
     */
    List<Notification> findByEventTypeOrderByCreatedAtDesc(String eventType);

    /**
     * 查找指定时间范围内的通知
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startTime AND :endTime ORDER BY n.createdAt DESC")
    List<Notification> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 查找待发送的通知
     */
    List<Notification> findByStatusOrderByCreatedAtAsc(Notification.NotificationStatus status);

    /**
     * 删除指定时间之前的已读通知
     */
    @Query("DELETE FROM Notification n WHERE n.readAt IS NOT NULL AND n.createdAt < :cutoffTime")
    void deleteReadNotificationsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 查找用户最近的通知
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findRecentByUserId(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    /**
     * 统计各种状态的通知数量
     */
    @Query("SELECT n.status, COUNT(n) FROM Notification n GROUP BY n.status")
    List<Object[]> countByStatus();

    /**
     * 统计各种类型的通知数量
     */
    @Query("SELECT n.type, COUNT(n) FROM Notification n GROUP BY n.type")
    List<Object[]> countByType();
}