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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.parking.dto.AuthDtos.UserResponse;
import com.parking.dto.AuthDtos.loginRequest;
import com.parking.dto.AuthDtos.registerRequest;
import com.parking.dto.BanDtos.AppealRequest;
import com.parking.dto.BanDtos.BanInfo;
import com.parking.dto.BanDtos.BanStatusRequest;
import com.parking.service.AccountBannedException;
import com.parking.service.BanService;
import com.parking.service.UserService;
import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/auth")
@RequiredArgsConstructor 
public class AuthController {
    private final UserService userService;
    private final BanService banService;
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
        authenticate(user, request, response);
        return user;
    }

    private void authenticate(UserResponse user, HttpServletRequest request, HttpServletResponse response) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            request.changeSessionId();
        } else {
            request.getSession(true);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.id(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    /** A banned user can't log in: answer 403 with the ban reason and appeal, for the app's suspended screen. */
    @ExceptionHandler(AccountBannedException.class)
    public ResponseEntity<BanInfo> banned(AccountBannedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getBanInfo());
    }

    @PostMapping("/ban-status")
    public BanInfo banStatus(@RequestBody BanStatusRequest request) {
        return banService.status(request);
    }

    @PostMapping("/appeal")
    @ResponseStatus(HttpStatus.CREATED)
    public BanInfo appeal(@RequestBody AppealRequest request) {
        return banService.appeal(request);
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
    public ResponseEntity<UserResponse> register(@RequestBody registerRequest request,
                                                 HttpServletRequest servletRequest,
                                                 HttpServletResponse servletResponse) {
        UserResponse user = userService.register(request);
        authenticate(user, servletRequest, servletResponse);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(user.id())
                    .toUri();
        return ResponseEntity.created(location).body(user);
    }
    
}
