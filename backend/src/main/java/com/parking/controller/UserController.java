package com.parking.controller;

import com.parking.dto.AuthDtos.UpdateUserRequest;
import com.parking.dto.AuthDtos.UserResponse;
import com.parking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor 
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable int id, Authentication authentication) {
        requireOwnAccountOrAdmin(id, authentication);
        return userService.getUser(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable int id, @RequestBody UpdateUserRequest request,
                                   Authentication authentication) {
        requireOwnAccountOrAdmin(id, authentication);
        return userService.updateUser(id, request);
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    private void requireOwnAccountOrAdmin(int id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !Integer.valueOf(id).equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own account");
        }
    }
}
