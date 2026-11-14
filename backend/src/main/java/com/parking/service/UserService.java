package com.parking.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.parking.dto.AuthDtos.UpdateUserRequest;
import com.parking.dto.AuthDtos.UserResponse;
import com.parking.dto.AuthDtos.loginRequest;
import com.parking.dto.AuthDtos.registerRequest;
import com.parking.entity.Role;
import com.parking.entity.User;
import com.parking.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.Locale;

@Service 
@RequiredArgsConstructor 
public class UserService {
    private static final Logger log= LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoderService passwordEncoderService;
    @Value("${parking.admin.name:System Administrator}")
    private String adminName;
    @Value("${parking.admin.email:admin@parking.com}")
    private String adminEmail;
    @Value("${parking.admin.password:admin123}")
    private String adminPassword;
    @PostConstruct 
    public void initDefaultAdmin(){
        if(userRepository.countByRole(Role.ADMIN) == 0){
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("No administrator exists and ADMIN_PASSWORD is not configured; skipping admin bootstrap");
                return;
            }
            log.info("No administrator account found . Initializing default admin user ...");
            User defaultAdmin = new User(
                adminName,
                adminEmail,
                passwordEncoderService.encode(adminPassword),
                Role.ADMIN
            );
            userRepository.save(defaultAdmin);
            log.info("Default administrator created: email={}", adminEmail);
        }
    }
    public UserResponse login(loginRequest request){
        if(request == null || request .email() == null || request.password() == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED , "Email and password are required"); 
        }
        String email = request.email().trim();
        String password = request.password();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED , "Invalid email or password"));
        if(!passwordEncoderService.matches(password , user.getPassword())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        } 
        if (user.isBlocked()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN , "Account is blocked. Please contact support");
        }
        return UserResponse.from(user);
    }
    @Transactional 
    public UserResponse register(registerRequest request){
        if(request == null || request.name()==null || request.name().isBlank() || request.email() == null || request.email().isBlank() || request.password() == null || request.password().isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Name , email , and password are required");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if(userRepository.existsByEmailIgnoreCase(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT , "Email is already registred");
        }
        User user = new User(
            request.name().trim(),
            email,
            passwordEncoderService.encode(request.password()),
            Role.USER
        );
        userRepository.save(user);
        return UserResponse.from(user);
    }
    @Transactional
    public UserResponse updateUser(int id , UpdateUserRequest request){
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one user field must be provided");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND , "User not found"));
        if (request.name() != null && !request.name().isBlank()){
            user.setName(request.name().trim());
        } 
        if(request.email() != null && !request.email().isBlank()){
            String newEmail = request.email().trim().toLowerCase(Locale.ROOT);
            userRepository.findByEmailIgnoreCase(newEmail).ifPresent(existing -> {
                if(!existing.getId().equals(user.getId())){
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
                }
            });
            user.setEmail(newEmail);
        }
        if(request.isHandicapped() != null){
            user.setHandicapped(request.isHandicapped());
        }
        if(request.isEvOwner() != null){
            user.setEvOwner(request.isEvOwner());
        }
        userRepository.save(user);
        return UserResponse.from(user);
    }
    public UserResponse getUser(int id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserResponse.from(user);
    }
    public List<UserResponse> getAllUsers(){
        return userRepository.findAll().stream()
            .map(UserResponse::from)
            .toList();
    }
}
