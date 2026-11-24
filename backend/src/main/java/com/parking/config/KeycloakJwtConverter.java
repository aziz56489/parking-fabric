package com.parking.config;

import com.parking.entity.Role;
import com.parking.entity.User;
import com.parking.repository.UserRepository;
import com.parking.service.PasswordEncoderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns a Keycloak access token into the same authentication as a session login:
 * the principal is the local user id (Integer) and the authorities are the Keycloak realm roles (ROLE_ADMIN...).
 * The local account is found by e-mail and created on the first call if it does not exist yet.
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakJwtConverter.class);
    private static final List<String> APP_ROLES = List.of("ADMIN", "USER", "TICKET_COUNTER", "EMPLOYEE");

    private final UserRepository userRepository;
    private final PasswordEncoderService passwordEncoder;

    public KeycloakJwtConverter(UserRepository userRepository, PasswordEncoderService passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> roles = realmRoles(jwt);
        User user = findOrCreateUser(jwt, roles);
        if (user.isBlocked()) {
            throw new DisabledException("Your account is blocked. Please contact support");
        }
        Collection<GrantedAuthority> authorities = roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getId(), jwt, authorities);
        authentication.setDetails(jwt.getClaims());
        return authentication;
    }

    /** Keycloak puts realm roles in {"realm_access": {"roles": [...]}}; only the application's roles are kept. */
    @SuppressWarnings("unchecked")
    private static List<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return ((Collection<Object>) roles).stream()
                .map(String::valueOf)
                .filter(APP_ROLES::contains)
                .toList();
    }

    private User findOrCreateUser(Jwt jwt, List<String> roles) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            // Service accounts (client credentials) have no e-mail.
            email = jwt.getClaimAsString("preferred_username") + "@keycloak.local";
        }
        String finalEmail = email;
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Role role = roles.contains("ADMIN") ? Role.ADMIN
                    : roles.contains("TICKET_COUNTER") ? Role.TICKET_COUNTER : Role.USER;
            String name = jwt.getClaimAsString("name") != null ? jwt.getClaimAsString("name")
                    : jwt.getClaimAsString("preferred_username");
            // The password is never used: this account signs in through Keycloak.
            User created = userRepository.save(new User(name, finalEmail,
                    passwordEncoder.encode(UUID.randomUUID().toString()), role));
            log.info("Created local account {} for Keycloak user {}", created.getId(), finalEmail);
            return created;
        });
    }
}
