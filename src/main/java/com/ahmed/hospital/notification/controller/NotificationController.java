package com.ahmed.hospital.notification.controller;

import com.ahmed.hospital.auth.service.CurrentUserService;
import com.ahmed.hospital.notification.dto.NotificationResponse;
import com.ahmed.hospital.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications",
        description = "In-app notification management for the authenticated user. " +
                      "Real-time delivery is provided via WebSocket at /ws (STOMP). " +
                      "These REST endpoints cover history and read-state management.")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Get my notifications (paginated)",
            description = "Returns a page of notifications for the authenticated user, " +
                          "ordered by most recent first. Default page size is 10.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification page returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @PageableDefault(size = 10)
            @Parameter(hidden = true) Pageable pageable
    ) {

        Long userId =
                currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                notificationService.getUserNotifications(
                        userId,
                        pageable
                )
        );
    }

    @Operation(summary = "Get unread notification count",
            description = "Returns the number of unread notifications for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Count returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {

        Long userId =
                currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                notificationService.getUnreadCount(userId)
        );
    }

    @Operation(summary = "Mark a notification as read")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marked as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Notification belongs to a different user"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable Long id
    ) {

        Long userId =
                currentUserService.getCurrentUserId();

        notificationService.markAsRead(
                id,
                userId
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark a notification as unread")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marked as unread"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Notification belongs to a different user"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PatchMapping("/{id}/unread")
    public ResponseEntity<Void> markAsUnread(
            @Parameter(description = "Notification ID") @PathVariable Long id
    ) {

        Long userId =
                currentUserService.getCurrentUserId();

        notificationService.markAsUnread(
                id,
                userId
        );

        return ResponseEntity.noContent().build();
    }
}