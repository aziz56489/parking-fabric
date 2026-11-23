package com.parking.service;

import com.parking.dto.BanDtos.AdminAppeal;
import com.parking.dto.BanDtos.AppealRequest;
import com.parking.dto.BanDtos.BanInfo;
import com.parking.dto.BanDtos.BanStatusRequest;
import com.parking.entity.AppealStatus;
import com.parking.entity.BanAppeal;
import com.parking.entity.User;
import com.parking.repository.BanAppealRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A banned user sees why and can appeal; the admin accepts (unbans) or rejects the appeal. */
@Service
@RequiredArgsConstructor
public class BanService {

    static final int MAX_TEXT = 1000;

    private final UserRepository userRepository;
    private final BanAppealRepository appealRepository;
    private final PasswordEncoderService passwordEncoderService;

    public BanInfo banInfo(User user) {
        return BanInfo.of(user, appealRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null));
    }

    /** For the app's "account suspended" screen: the ban reason and the latest appeal. */
    public BanInfo status(BanStatusRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }
        return banInfo(authenticate(request.email(), request.password()));
    }

    @Transactional
    public BanInfo appeal(AppealRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }
        User user = authenticate(request.email(), request.password());
        if (!user.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Your account is not banned. You can log in.");
        }
        String message = text(request.message(), "Please explain why the ban should be lifted");
        if (appealRepository.existsByUserIdAndStatus(user.getId(), AppealStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an appeal waiting for an admin");
        }
        appealRepository.save(new BanAppeal(user, message));
        return banInfo(user);
    }

    public List<AdminAppeal> appeals() {
        List<BanAppeal> appeals = appealRepository.findAllByOrderByCreatedAtDesc();
        Map<Integer, User> users = userRepository.findAllById(appeals.stream()
                        .flatMap(a -> Stream.of(a.getUserId(), a.getResolvedById()))
                        .filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return appeals.stream()
                .map(a -> AdminAppeal.of(a, users.get(a.getUserId()),
                        a.getResolvedById() == null ? null : users.get(a.getResolvedById())))
                .toList();
    }

    /** Accepting an appeal unbans the user. */
    @Transactional
    public AdminAppeal accept(int appealId, String response, int adminId) {
        BanAppeal appeal = pendingAppeal(appealId);
        User user = userRepository.findById(appeal.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        appeal.resolve(AppealStatus.ACCEPTED, optionalText(response), adminId);
        user.unban();
        userRepository.save(user);
        return AdminAppeal.of(appealRepository.save(appeal), user, userRepository.findById(adminId).orElse(null));
    }

    @Transactional
    public AdminAppeal reject(int appealId, String response, int adminId) {
        BanAppeal appeal = pendingAppeal(appealId);
        appeal.resolve(AppealStatus.REJECTED, optionalText(response), adminId);
        return AdminAppeal.of(appealRepository.save(appeal),
                userRepository.findById(appeal.getUserId()).orElse(null),
                userRepository.findById(adminId).orElse(null));
    }

    /** When the admin unbans directly, open appeals have nothing left to decide. */
    @Transactional
    public void closePendingAppeals(User user, int adminId) {
        List<BanAppeal> pending = appealRepository.findByUserIdAndStatus(user.getId(), AppealStatus.PENDING);
        pending.forEach(a -> a.resolve(AppealStatus.ACCEPTED, null, adminId));
        appealRepository.saveAll(pending);
    }

    private BanAppeal pendingAppeal(int id) {
        BanAppeal appeal = appealRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appeal not found"));
        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This appeal was already answered");
        }
        return appeal;
    }

    private User authenticate(String email, String password) {
        if (email == null || password == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email and password are required");
        }
        return userRepository.findByEmailIgnoreCase(email.trim())
                .filter(u -> passwordEncoderService.matches(password, u.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
    }

    static String text(String value, String missingMessage) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, missingMessage);
        }
        if (trimmed.length() > MAX_TEXT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The text is limited to " + MAX_TEXT + " characters");
        }
        return trimmed;
    }

    private static String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return text(value, "");
    }
}
