package mw.nwra.ewaterpermit.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mw.nwra.ewaterpermit.exception.EntityNotFoundException;
import mw.nwra.ewaterpermit.exception.ForbiddenException;
import mw.nwra.ewaterpermit.model.SysUserAccount;
import mw.nwra.ewaterpermit.model.UserNotification;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationCategory;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationPriority;
import mw.nwra.ewaterpermit.model.UserNotification.NotificationType;
import mw.nwra.ewaterpermit.service.NotificationService;
import mw.nwra.ewaterpermit.util.AppUtil;
import mw.nwra.ewaterpermit.util.RateLimiter;

@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private RateLimiter rateLimiter;

    @GetMapping("/user")
    public Page<UserNotification> getUserNotifications(
            @RequestHeader(name = "Authorization") String token,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "isRead", required = false) Boolean isRead,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "priority", required = false) String priority) {
        
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("Unauthorized access");
        }
        
        Pageable pageable = PageRequest.of(page, limit);
        
        if (isRead != null) {
            return notificationService.getUserNotifications(user.getId(), isRead, pageable);
        } else if (type != null) {
            try {
                NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());
                return notificationService.getUserNotificationsByType(user.getId(), notificationType, pageable);
            } catch (IllegalArgumentException e) {
                throw new ForbiddenException("Invalid notification type: " + type);
            }
        } else if (category != null) {
            try {
                NotificationCategory notificationCategory = NotificationCategory.valueOf(category.toUpperCase());
                return notificationService.getUserNotificationsByCategory(user.getId(), notificationCategory, pageable);
            } catch (IllegalArgumentException e) {
                throw new ForbiddenException("Invalid notification category: " + category);
            }
        } else if (priority != null) {
            try {
                NotificationPriority notificationPriority = NotificationPriority.valueOf(priority.toUpperCase());
                return notificationService.getUserNotificationsByPriority(user.getId(), notificationPriority, pageable);
            } catch (IllegalArgumentException e) {
                throw new ForbiddenException("Invalid notification priority: " + priority);
            }
        } else {
            return notificationService.getUserNotifications(user.getId(), pageable);
        }
    }

    @GetMapping("/user/counts")
    public Map<String, Long> getUserNotificationCounts(@RequestHeader(name = "Authorization") String token) {
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("Unauthorized access");
        }
        
        return notificationService.getUserNotificationCounts(user.getId());
    }

    @PutMapping("/{id}/mark-read")
    public ResponseEntity<Map<String, String>> markNotificationAsRead(
            @PathVariable String id,
            @RequestHeader(name = "Authorization") String token) {
        
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("Unauthorized access");
        }
        
        boolean updated = notificationService.markAsRead(id, user.getId());
        if (!updated) {
            throw new EntityNotFoundException("Notification not found or already read");
        }
        
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PutMapping("/mark-read-multiple")
    public ResponseEntity<Map<String, String>> markMultipleAsRead(
            @RequestBody Map<String, List<String>> request,
            @RequestHeader(name = "Authorization") String token) {
        
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("Unauthorized access");
        }
        
        List<String> notificationIds = request.get("notificationIds");
        if (notificationIds == null || notificationIds.isEmpty()) {
            throw new ForbiddenException("Notification IDs are required");
        }
        
        int updated = notificationService.markMultipleAsRead(notificationIds, user.getId());
        
        return ResponseEntity.ok(Map.of("message", updated + " notifications marked as read"));
    }

    @PutMapping("/user/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllAsRead(@RequestHeader(name = "Authorization") String token) {
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("Unauthorized access");
        }
        
        int updated = notificationService.markAllAsRead(user.getId());
        
        return ResponseEntity.ok(Map.of("message", updated + " notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable String id,
            @RequestHeader(name = "Authorization") String token) {
        
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("Unauthorized access");
        }
        
        boolean deleted = notificationService.deleteNotification(id, user.getId());
        if (!deleted) {
            throw new EntityNotFoundException("Notification not found");
        }
        
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    @DeleteMapping("/delete-multiple")
    public ResponseEntity<Map<String, String>> deleteMultipleNotifications(
            @RequestBody Map<String, List<String>> request,
            @RequestHeader(name = "Authorization") String token) {
        
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("Unauthorized access");
        }
        
        List<String> notificationIds = request.get("notificationIds");
        if (notificationIds == null || notificationIds.isEmpty()) {
            throw new ForbiddenException("Notification IDs are required");
        }
        
        int deleted = notificationService.deleteMultipleNotifications(notificationIds, user.getId());
        
        return ResponseEntity.ok(Map.of("message", deleted + " notifications deleted"));
    }

    @PostMapping("")
    public UserNotification createNotification(
            @RequestBody Map<String, Object> notificationRequest,
            @RequestHeader(name = "Authorization") String token) {
        
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null || user.getSysUserGroup() == null || 
            !user.getSysUserGroup().getName().equalsIgnoreCase("admin")) {
            throw new ForbiddenException("Admin access required");
        }
        
        UserNotification notification = (UserNotification) AppUtil.objectToClass(UserNotification.class, notificationRequest);
        if (notification == null) {
            throw new ForbiddenException("Could not create the notification");
        }
        
        return notificationService.createNotification(notification);
    }

    @GetMapping("/poll")
    public ResponseEntity<Map<String, Object>> pollForUpdates(
            @RequestHeader(name = "Authorization") String token,
            @RequestParam(value = "lastPolled", required = false) String lastPolledStr) {
        
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("Unauthorized access");
        }
        
        // Check rate limit
        if (!rateLimiter.isAllowed(user.getId())) {
            Map<String, Object> rateLimitResponse = Map.of(
                "error", "Rate limit exceeded",
                "message", "Too many requests. Please wait before polling again.",
                "remainingRequests", rateLimiter.getRemainingRequests(user.getId()),
                "resetInSeconds", rateLimiter.getSecondsUntilReset(user.getId())
            );
            return ResponseEntity.status(429).body(rateLimitResponse);
        }
        
        LocalDateTime lastPolled = null;
        if (lastPolledStr != null && !lastPolledStr.isEmpty()) {
            try {
                lastPolled = LocalDateTime.parse(lastPolledStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new ForbiddenException("Invalid date format. Use ISO format: yyyy-MM-ddTHH:mm:ss");
            }
        }
        
        Map<String, Object> response = notificationService.pollForUpdates(user.getId(), lastPolled);
        
        // Add rate limit info to response headers
        return ResponseEntity.ok()
            .header("X-RateLimit-Remaining", String.valueOf(rateLimiter.getRemainingRequests(user.getId())))
            .header("X-RateLimit-Reset", String.valueOf(rateLimiter.getSecondsUntilReset(user.getId())))
            .body(response);
    }

    @GetMapping("/new")
    public List<UserNotification> getNewNotifications(
            @RequestHeader(name = "Authorization") String token,
            @RequestParam(value = "since", required = false) String sinceStr) {
        
        SysUserAccount user = AppUtil.getLoggedInUser(token);
        if (user == null) {
            throw new ForbiddenException("Unauthorized access");
        }
        
        LocalDateTime since = null;
        if (sinceStr != null && !sinceStr.isEmpty()) {
            try {
                since = LocalDateTime.parse(sinceStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new ForbiddenException("Invalid date format. Use ISO format: yyyy-MM-ddTHH:mm:ss");
            }
        }
        
        return notificationService.getNewNotificationsSince(user.getId(), since);
    }
}