package mw.nwra.ewaterpermit.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mw.nwra.ewaterpermit.model.UserNotification;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationCategory;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationPriority;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationType;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, String> {

    Page<UserNotification> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<UserNotification> findByUserIdAndIsDeletedFalseAndIsReadOrderByCreatedAtDesc(String userId, Boolean isRead, Pageable pageable);

    Page<UserNotification> findByUserIdAndIsDeletedFalseAndTypeOrderByCreatedAtDesc(String userId, NotificationType type, Pageable pageable);

    Page<UserNotification> findByUserIdAndIsDeletedFalseAndCategoryOrderByCreatedAtDesc(String userId, NotificationCategory category, Pageable pageable);

    Page<UserNotification> findByUserIdAndIsDeletedFalseAndPriorityOrderByCreatedAtDesc(String userId, NotificationPriority priority, Pageable pageable);

    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.userId = :userId AND n.isDeleted = false")
    long countByUserIdAndIsDeletedFalse(@Param("userId") String userId);

    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.userId = :userId AND n.isDeleted = false AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.userId = :userId AND n.isDeleted = false AND n.type = :type")
    long countByUserIdAndType(@Param("userId") String userId, @Param("type") NotificationType type);

    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.userId = :userId AND n.isDeleted = false AND n.category = :category")
    long countByUserIdAndCategory(@Param("userId") String userId, @Param("category") NotificationCategory category);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true, n.updatedAt = :updatedAt WHERE n.id = :id AND n.userId = :userId")
    int markAsRead(@Param("id") String id, @Param("userId") String userId, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true, n.updatedAt = :updatedAt WHERE n.id IN :ids AND n.userId = :userId")
    int markMultipleAsRead(@Param("ids") List<String> ids, @Param("userId") String userId, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true, n.updatedAt = :updatedAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") String userId, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isDeleted = true, n.updatedAt = :updatedAt WHERE n.id = :id AND n.userId = :userId")
    int deleteNotification(@Param("id") String id, @Param("userId") String userId, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isDeleted = true, n.updatedAt = :updatedAt WHERE n.id IN :ids AND n.userId = :userId")
    int deleteMultipleNotifications(@Param("ids") List<String> ids, @Param("userId") String userId, @Param("updatedAt") LocalDateTime updatedAt);

    @Query("SELECT n FROM UserNotification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :currentTime AND n.isDeleted = false")
    List<UserNotification> findExpiredNotifications(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND n.isDeleted = false AND n.createdAt > :lastPolled ORDER BY n.createdAt DESC")
    List<UserNotification> findNewNotificationsSince(@Param("userId") String userId, @Param("lastPolled") LocalDateTime lastPolled);

    @Query("SELECT MAX(n.createdAt) FROM UserNotification n WHERE n.userId = :userId AND n.isDeleted = false")
    LocalDateTime findLatestNotificationTime(@Param("userId") String userId);
}