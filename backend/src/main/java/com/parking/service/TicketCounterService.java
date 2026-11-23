package com.parking.service;

import com.parking.dto.CounterDtos.CounterReservation;
import com.parking.dto.CounterDtos.NonPayingReportResponse;
import com.parking.dto.ParkingRequests.NonPayingReportRequest;
import com.parking.entity.NonPayingReport;
import com.parking.entity.ReportStatus;
import com.parking.entity.Reservation;
import com.parking.entity.Role;
import com.parking.entity.User;
import com.parking.repository.NonPayingReportRepository;
import com.parking.repository.ReservationRepository;
import com.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** SCRUM-88: As a ticket counter, I can report a non paying user. */
@Service
@RequiredArgsConstructor
public class TicketCounterService {

    private final ReservationRepository reservationRepository;
    private final NonPayingReportRepository reportRepository;
    private final UserRepository userRepository;
    private final BanService banService;

    /** Recent reservations, newest first, with the driver's name and email. */
    public List<CounterReservation> recentReservations() {
        List<Reservation> reservations = reservationRepository.findTop100ByOrderByStartedAtDesc();
        Map<Integer, User> users = usersById(reservations.stream().map(r -> parseUserId(r.getUserId())));
        Set<Integer> reported = reportRepository
                .findByReservationIdIn(reservations.stream().map(Reservation::getId).toList())
                .stream()
                .map(NonPayingReport::getReservationId)
                .collect(Collectors.toCollection(HashSet::new));
        return reservations.stream()
                .map(r -> {
                    Integer userId = parseUserId(r.getUserId());
                    return CounterReservation.of(r, userId, users.get(userId), reported.contains(r.getId()));
                })
                .toList();
    }

    @Transactional
    public NonPayingReportResponse report(int ticketCounterId, NonPayingReportRequest request) {
        if (request == null || request.reservationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The reservation is required");
        }
        String note = request.note() == null ? "" : request.note().trim();
        if (note.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The note is limited to 1000 characters");
        }
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        Integer userId = parseUserId(reservation.getUserId());
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reservation has no registered user to report");
        }
        if (user.getId() == ticketCounterId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot report yourself");
        }
        if (reportRepository.existsByReservationId(reservation.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This reservation was already reported as not paid");
        }
        NonPayingReport report = reportRepository.save(
                new NonPayingReport(ticketCounterId, user.getId(), reservation, note.isEmpty() ? null : note));
        return toResponses(List.of(report)).get(0);
    }

    /** All reports, newest first. */
    public List<NonPayingReportResponse> reports() {
        return toResponses(reportRepository.findAllByOrderByReportedAtDesc());
    }

    /**
     * Admin decision: bans (blocks) the reported user. The user can no longer log in or reserve.
     * Every other pending report about the same user is closed as BANNED too.
     */
    @Transactional
    public List<NonPayingReportResponse> ban(int reportId, String reason, int adminId) {
        String banReason = BanService.text(reason, "Please write the reason of the ban; the user will see it");
        NonPayingReport report = getReport(reportId);
        User user = userRepository.findById(report.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The reported user no longer exists"));
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An administrator account cannot be banned");
        }
        user.ban(banReason);
        userRepository.save(user);
        List<NonPayingReport> closed = reportRepository.findByUserId(user.getId()).stream()
                .filter(r -> r.getId().equals(reportId) || r.getStatus() == ReportStatus.PENDING)
                .peek(r -> r.resolve(ReportStatus.BANNED, adminId))
                .toList();
        reportRepository.saveAll(closed);
        return toResponses(closed);
    }

    /** Admin decision: lets it go. The user is not banned; the report stays in the history. */
    @Transactional
    public NonPayingReportResponse dismiss(int reportId, int adminId) {
        NonPayingReport report = getReport(reportId);
        if (report.getStatus() == ReportStatus.BANNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This user was already banned for this report. Unban the user instead.");
        }
        report.resolve(ReportStatus.DISMISSED, adminId);
        return toResponses(List.of(reportRepository.save(report))).get(0);
    }

    /** Admin decision: gives a banned user their account back. */
    @Transactional
    public void unban(int userId, int adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.unban();
        userRepository.save(user);
        banService.closePendingAppeals(user, adminId);
    }

    private NonPayingReport getReport(int id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    private List<NonPayingReportResponse> toResponses(List<NonPayingReport> reports) {
        Map<Integer, User> users = usersById(reports.stream()
                .flatMap(r -> Stream.of(r.getUserId(), r.getTicketCounterId(), r.getResolvedById())));
        return reports.stream()
                .map(r -> NonPayingReportResponse.of(
                        r,
                        users.get(r.getUserId()),
                        users.get(r.getTicketCounterId()),
                        r.getResolvedById() == null ? null : users.get(r.getResolvedById())))
                .toList();
    }

    private Map<Integer, User> usersById(Stream<Integer> ids) {
        List<Integer> distinct = ids.filter(Objects::nonNull).distinct().toList();
        return userRepository.findAllById(distinct).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /** Reservations store the user id as text; old ones may hold a random app id instead of a user id. */
    private static Integer parseUserId(String userId) {
        if (userId == null) {
            return null;
        }
        try {
            return Integer.valueOf(userId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
