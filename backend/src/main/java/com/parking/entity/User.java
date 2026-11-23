package com.parking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "users")
@Getter 
@Setter 
@NoArgsConstructor 
public class User {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column (nullable = false, length = 100)
    private String name;
    @Column (nullable = false , length = 255 , unique = true)
    private String email;
    @Column(nullable = false , length = 255)
    private String password;
    @Enumerated(EnumType.STRING)
    @Column (nullable = false)
    private Role role = Role.USER;
    @Column (nullable =false , name = "is_handicapped")
    private boolean isHandicapped;
    @Column(name = "is_ev_owner" , nullable = false)
    private boolean isEvOwner;
    @Column (name="is_blocked" , nullable = false)
    private boolean isBlocked;
    /** Why the admin banned this user; shown to the user in the app. Null when not banned. */
    @Column (name = "ban_reason", length = 1000)
    private String banReason;
    @Column (name = "banned_at")
    private java.time.Instant bannedAt;

    public void ban(String reason) {
        this.isBlocked = true;
        this.banReason = reason;
        this.bannedAt = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    public void unban() {
        this.isBlocked = false;
        this.banReason = null;
        this.bannedAt = null;
    }
    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : Role.USER;
    }
    
}
