package com.parking.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.parking.dto.AuthDtos.UserResponse;
import com.parking.dto.AuthDtos.loginRequest;
import com.parking.dto.AuthDtos.registerRequest;
import com.parking.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/auth")
@RequiredArgsConstructor 
public class AuthController {
    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @PostMapping ("/login")
    public UserResponse login(@RequestBody loginRequest loginRequest,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        UserResponse user = userService.login(loginRequest);
        if (!"ADMIN".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
        }

        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            request.changeSessionId();
        } else {
            request.getSession(true);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.id(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return user;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getUser((Integer) authentication.getPrincipal());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }
    @PostMapping ("/register")
    public ResponseEntity<UserResponse> register(@RequestBody registerRequest request){
        UserResponse response = userService.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(response.id())
                    .toUri();
        return ResponseEntity.created(location).body(response);
    }
    
}
