package com.parking.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Stateless OAuth2 resource server: every API call needs a Keycloak access token. */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // no cookies, only Bearer tokens
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus",
                                "/actuator/circuitbreakers", "/actuator/circuitbreakerevents/**",
                                "/actuator/retries", "/actuator/retryevents/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // Listing every user's notifications is reserved to admins.
                        .requestMatchers(request -> HttpMethod.GET.matches(request.getMethod())
                                && "/api/notifications".equals(request.getRequestURI())
                                && request.getParameter("userId") == null).hasRole("ADMIN")
                        .requestMatchers("/api/notifications/**").hasAnyRole("USER", "ADMIN", "TICKET_COUNTER")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRoles())));
        return http.build();
    }

    /** Keycloak realm roles ({"realm_access": {"roles": [...]}}) become ROLE_ authorities. */
    @SuppressWarnings("unchecked")
    private static Converter<Jwt, AbstractAuthenticationToken> keycloakRoles() {
        return jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            Collection<GrantedAuthority> authorities = realmAccess == null
                    || !(realmAccess.get("roles") instanceof Collection<?> roles)
                    ? List.of()
                    : ((Collection<Object>) roles).stream()
                    .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("preferred_username"));
        };
    }
}
