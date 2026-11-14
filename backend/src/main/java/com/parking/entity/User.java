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
    private boolean isHandicapped = false;
    @Column(name = "is_ev_owner" , nullable = false)
    private boolean isEvOwner = false;
    @Column (name="is_blocked" , nullable = false)
    private boolean isBlocked = false;
    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : Role.USER;
    }
    
}
