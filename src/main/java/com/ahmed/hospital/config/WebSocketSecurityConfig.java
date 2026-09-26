package com.ahmed.hospital.config;

import com.ahmed.hospital.auth.security.JwtService;
import com.ahmed.hospital.auth.service.CustomUserDetailsService;
import com.ahmed.hospital.user.entity.User;
import com.ahmed.hospital.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;

import java.util.List;
import java.util.Optional;

/**
 * WebSocket Security Configuration.
 *
 * Implements two security measures:
 *
 * 1. STOMP CONNECT authentication via JWT:
 *    Clients must send a valid JWT in the "Authorization" STOMP header
 *    during CONNECT. The authenticated principal is then available throughout
 *    the WebSocket session.
 *
 * 2. SUBSCRIBE ownership validation:
 *    When a client subscribes to /topic/notifications/{userId}, we verify
 *    that the authenticated user's database ID matches the {userId} in the topic.
 *    This prevents user 20 from subscribing to user 15's private notifications
 *    simply by guessing the topic path.
 */
@Configuration
@RequiredArgsConstructor
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log =
            LoggerFactory.getLogger(WebSocketSecurityConfig.class);

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {

            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {

                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

                if (accessor == null) {
                    return message;
                }

                StompCommand command = accessor.getCommand();

                // ─── CONNECT: authenticate via JWT ───────────────────────────
                if (StompCommand.CONNECT.equals(command)) {

                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        log.warn("WebSocket CONNECT rejected: missing or invalid Authorization header");
                        throw new org.springframework.messaging.MessageDeliveryException(
                                message,
                                "Authentication required for WebSocket connections"
                        );
                    }

                    String token = authHeader.substring(7);

                    try {
                        String email = jwtService.extractEmail(token);

                        if (email == null) {
                            throw new IllegalArgumentException("Unable to extract email from token");
                        }

                        UserDetails userDetails =
                                userDetailsService.loadUserByUsername(email);

                        if (!jwtService.isTokenValid(token, email)) {
                            throw new IllegalArgumentException("Token is invalid or expired");
                        }

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        accessor.setUser(authentication);

                    } catch (Exception ex) {
                        log.warn("WebSocket CONNECT rejected: invalid JWT");
                        throw new org.springframework.messaging.MessageDeliveryException(
                                message,
                                "Invalid or expired token"
                        );
                    }
                }

                // ─── SUBSCRIBE: validate topic ownership ──────────────────────
                if (StompCommand.SUBSCRIBE.equals(command)) {

                    String destination = accessor.getDestination();

                    if (destination != null &&
                            destination.startsWith("/topic/notifications/")) {

                        String requestedUserIdStr =
                                destination.replace("/topic/notifications/", "").trim();

                        Long requestedUserId;

                        try {
                            requestedUserId = Long.parseLong(requestedUserIdStr);
                        } catch (NumberFormatException ex) {
                            log.warn(
                                    "WebSocket SUBSCRIBE rejected: invalid userId in destination '{}'",
                                    destination
                            );
                            throw new org.springframework.messaging.MessageDeliveryException(
                                    message,
                                    "Invalid notification topic"
                            );
                        }

                        // Retrieve the authenticated principal set during CONNECT
                        java.security.Principal principal = accessor.getUser();

                        if (principal == null) {
                            log.warn(
                                    "WebSocket SUBSCRIBE rejected: unauthenticated attempt on '{}'",
                                    destination
                            );
                            throw new org.springframework.messaging.MessageDeliveryException(
                                    message,
                                    "Authentication required"
                            );
                        }

                        String email = principal.getName();

                        Optional<User> userOpt = userRepository.findByEmail(email);

                        if (userOpt.isEmpty()) {
                            log.warn(
                                    "WebSocket SUBSCRIBE rejected: user '{}' not found",
                                    email
                            );
                            throw new org.springframework.messaging.MessageDeliveryException(
                                    message,
                                    "User not found"
                            );
                        }

                        Long authenticatedUserId = userOpt.get().getId();

                        if (!authenticatedUserId.equals(requestedUserId)) {
                            // Log the attempt without exposing user IDs in the message
                            log.warn(
                                    "WebSocket SUBSCRIBE rejected: user tried to subscribe to another user's notification channel"
                            );
                            throw new org.springframework.messaging.MessageDeliveryException(
                                    message,
                                    "You are not allowed to subscribe to this channel"
                            );
                        }
                    }
                }

                return message;
            }
        });
    }
}
