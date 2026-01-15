package mw.nwra.ewaterpermit.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import mw.nwra.ewaterpermit.model.UserNotification;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationCategory;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationPriority;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationType;

public interface NotificationService {

    Page<UserNotification> getUserNotifications(String userId, Pageable pageable);

    Page<UserNotification> getUserNotifications(String userId, Boolean isRead, Pageable pageable);

    Page<UserNotification> getUserNotificationsByType(String userId, NotificationType type, Pageable pageable);

    Page<UserNotification> getUserNotificationsByCategory(String userId, NotificationCategory category, Pageable pageable);

    Page<UserNotification> getUserNotificationsByPriority(String userId, NotificationPriority priority, Pageable pageable);

    Map<String, Long> getUserNotificationCounts(String userId);

    UserNotification createNotification(UserNotification notification);

    boolean markAsRead(String notificationId, String userId);

    int markMultipleAsRead(List<String> notificationIds, String userId);

    int markAllAsRead(String userId);

    boolean deleteNotification(String notificationId, String userId);

    int deleteMultipleNotifications(List<String> notificationIds, String userId);

    void cleanupExpiredNotifications();

    List<UserNotification> getNewNotificationsSince(String userId, LocalDateTime lastPolled);

    Map<String, Object> pollForUpdates(String userId, LocalDateTime lastPolled);
}