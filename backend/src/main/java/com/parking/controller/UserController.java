package com.parking.controller;

import com.parking.dto.AuthDtos.UpdateUserRequest;
import com.parking.dto.AuthDtos.UserResponse;
import com.parking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor 
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable int id) {
        return userService.getUser(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable int id, @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }
}
