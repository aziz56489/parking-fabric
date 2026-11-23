package com.parking.dto;

import com.parking.entity.AppealStatus;
import com.parking.entity.BanAppeal;
import com.parking.entity.User;

import java.time.Instant;

/** Bans and appeals. */
public final class BanDtos {

    private BanDtos() {
    }

    /** The admin must say why the user is banned; the user sees it in the app. */
    public record BanRequest(String reason) {
    }

    /** A banned user can't log in, so they prove who they are with their email and password. */
    public record BanStatusRequest(String email, String password) {
    }

    public record AppealRequest(String email, String password, String message) {
    }

    /** The admin's optional answer when accepting or rejecting an appeal. */
    public record AppealDecisionRequest(String response) {
    }

    public record AppealResponse(
            Integer id,
            String message,
            AppealStatus status,
            Instant createdAt,
            String adminResponse,
            Instant resolvedAt) {

        public static AppealResponse of(BanAppeal appeal) {
            return appeal == null ? null : new AppealResponse(
                    appeal.getId(),
                    appeal.getMessage(),
                    appeal.getStatus(),
                    appeal.getCreatedAt(),
                    appeal.getAdminResponse(),
                    appeal.getResolvedAt());
        }
    }

    /**
     * What a banned user sees. Also the body of the 403 that login answers for a banned account
     * ({@code banned} tells the app to open the "account suspended" screen).
     */
    public record BanInfo(
            boolean banned,
            String message,
            String name,
            String email,
            String reason,
            Instant bannedAt,
            AppealResponse appeal,
            boolean canAppeal) {

        public static BanInfo of(User user, BanAppeal latestAppeal) {
            boolean pending = latestAppeal != null && latestAppeal.getStatus() == AppealStatus.PENDING;
            return new BanInfo(
                    user.isBlocked(),
                    user.isBlocked() ? "Your account is banned" : "Your account is active",
                    user.getName(),
                    user.getEmail(),
                    user.getBanReason(),
                    user.getBannedAt(),
                    AppealResponse.of(latestAppeal),
                    user.isBlocked() && !pending);
        }
    }

    /** An appeal as the admin sees it. */
    public record AdminAppeal(
            Integer id,
            Integer userId,
            String userName,
            String userEmail,
            boolean userBlocked,
            String banReason,
            String message,
            AppealStatus status,
            Instant createdAt,
            String adminResponse,
            String resolvedByName,
            Instant resolvedAt) {

        public static AdminAppeal of(BanAppeal appeal, User user, User resolver) {
            return new AdminAppeal(
                    appeal.getId(),
                    appeal.getUserId(),
                    user != null ? user.getName() : null,
                    user != null ? user.getEmail() : null,
                    user != null && user.isBlocked(),
                    appeal.getBanReason(),
                    appeal.getMessage(),
                    appeal.getStatus(),
                    appeal.getCreatedAt(),
                    appeal.getAdminResponse(),
                    resolver != null ? resolver.getName() : null,
                    appeal.getResolvedAt());
        }
    }
}
