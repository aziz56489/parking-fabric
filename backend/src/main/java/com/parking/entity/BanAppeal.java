package com.parking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** A banned user asks the admin to lift the ban. */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "ban_appeal", schema = "public")
public class BanAppeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false, length = 1000)
    private String message;

    /** The ban reason at the time of the appeal, so the admin sees what the user answers to. */
    @Column(length = 1000)
    private String banReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppealStatus status = AppealStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt;

    /** Optional answer from the admin, shown to the user. */
    @Column(length = 1000)
    private String adminResponse;

    private Integer resolvedById;

    private Instant resolvedAt;

    public BanAppeal(User user, String message) {
        this.userId = user.getId();
        this.message = message;
        this.banReason = user.getBanReason();
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public void resolve(AppealStatus decision, String response, int adminId) {
        this.status = decision;
        this.adminResponse = response;
        this.resolvedById = adminId;
        this.resolvedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
