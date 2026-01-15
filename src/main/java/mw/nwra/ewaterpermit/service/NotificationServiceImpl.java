package mw.nwra.ewaterpermit.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mw.nwra.ewaterpermit.model.UserNotification;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationCategory;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationPriority;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationType;
import mw.nwra.ewaterpermit.repository.UserNotificationRepository;

@Service(value = "notificationService")
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private UserNotificationRepository notificationRepository;

    @Override
    public Page<UserNotification> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public Page<UserNotification> getUserNotifications(String userId, Boolean isRead, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsDeletedFalseAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable);
    }

    @Override
    public Page<UserNotification> getUserNotificationsByType(String userId, NotificationType type, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsDeletedFalseAndTypeOrderByCreatedAtDesc(userId, type, pageable);
    }

    @Override
    public Page<UserNotification> getUserNotificationsByCategory(String userId, NotificationCategory category, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsDeletedFalseAndCategoryOrderByCreatedAtDesc(userId, category, pageable);
    }

    @Override
    public Page<UserNotification> getUserNotificationsByPriority(String userId, NotificationPriority priority, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsDeletedFalseAndPriorityOrderByCreatedAtDesc(userId, priority, pageable);
    }

    @Override
    public Map<String, Long> getUserNotificationCounts(String userId) {
        Map<String, Long> counts = new HashMap<>();
        
        counts.put("total", notificationRepository.countByUserIdAndIsDeletedFalse(userId));
        counts.put("unread", notificationRepository.countUnreadByUserId(userId));
        
        // Counts by type
        for (NotificationType type : NotificationType.values()) {
            counts.put("type_" + type.name().toLowerCase(), 
                      notificationRepository.countByUserIdAndType(userId, type));
        }
        
        // Counts by category
        for (NotificationCategory category : NotificationCategory.values()) {
            counts.put("category_" + category.name().toLowerCase(), 
                      notificationRepository.countByUserIdAndCategory(userId, category));
        }
        
        return counts;
    }

    @Override
    @Transactional
    public UserNotification createNotification(UserNotification notification) {
        if (notification.getId() == null || notification.getId().isEmpty()) {
            notification.setId(UUID.randomUUID().toString());
        }
        
        if (notification.getIsRead() == null) {
            notification.setIsRead(false);
        }
        
        if (notification.getIsDeleted() == null) {
            notification.setIsDeleted(false);
        }
        
        return notificationRepository.saveAndFlush(notification);
    }

    @Override
    @Transactional
    public boolean markAsRead(String notificationId, String userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
        return updated > 0;
    }

    @Override
    @Transactional
    public int markMultipleAsRead(List<String> notificationIds, String userId) {
        return notificationRepository.markMultipleAsRead(notificationIds, userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public boolean deleteNotification(String notificationId, String userId) {
        int updated = notificationRepository.deleteNotification(notificationId, userId, LocalDateTime.now());
        return updated > 0;
    }

    @Override
    @Transactional
    public int deleteMultipleNotifications(List<String> notificationIds, String userId) {
        return notificationRepository.deleteMultipleNotifications(notificationIds, userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void cleanupExpiredNotifications() {
        List<UserNotification> expiredNotifications = notificationRepository.findExpiredNotifications(LocalDateTime.now());
        for (UserNotification notification : expiredNotifications) {
            notification.setIsDeleted(true);
            notification.setUpdatedAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(expiredNotifications);
    }

    @Override
    public List<UserNotification> getNewNotificationsSince(String userId, LocalDateTime lastPolled) {
        if (lastPolled == null) {
            // If no last polled time, return recent notifications (last 24 hours)
            lastPolled = LocalDateTime.now().minusDays(1);
        }
        return notificationRepository.findNewNotificationsSince(userId, lastPolled);
    }

    @Override
    public Map<String, Object> pollForUpdates(String userId, LocalDateTime lastPolled) {
        Map<String, Object> response = new HashMap<>();
        
        // Get new notifications since last poll
        List<UserNotification> newNotifications = getNewNotificationsSince(userId, lastPolled);
        
        // Get updated counts
        Map<String, Long> counts = getUserNotificationCounts(userId);
        
        // Get latest notification timestamp for next poll
        LocalDateTime latestTime = notificationRepository.findLatestNotificationTime(userId);
        
        response.put("newNotifications", newNotifications);
        response.put("counts", counts);
        response.put("latestTimestamp", latestTime != null ? latestTime : LocalDateTime.now());
        response.put("hasNewNotifications", !newNotifications.isEmpty());
        response.put("pollTime", LocalDateTime.now());
        
        return response;
    }
}