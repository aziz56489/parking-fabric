package com.parking.controller;

import com.parking.dto.BanDtos.AdminAppeal;
import com.parking.dto.BanDtos.AppealDecisionRequest;
import com.parking.dto.BanDtos.BanRequest;
import com.parking.dto.CounterDtos.NonPayingReportResponse;
import com.parking.dto.StatsDtos.EarningsStats;
import com.parking.dto.StatsDtos.EntryStats;
import com.parking.dto.StatsDtos.Period;
import com.parking.service.BanService;
import com.parking.service.StatsService;
import com.parking.service.TicketCounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin dashboard: non paying reports, bans and appeals, statistics. ADMIN only (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminReportController {

    private final TicketCounterService ticketCounterService;
    private final BanService banService;
    private final StatsService statsService;

    @GetMapping("/non-paying-reports")
    public List<NonPayingReportResponse> reports() {
        return ticketCounterService.reports();
    }

    /** Bans the user with a reason they will see. Returns every report closed by the ban. */
    @PostMapping("/non-paying-reports/{id}/ban")
    public List<NonPayingReportResponse> ban(@PathVariable int id, @RequestBody(required = false) BanRequest request,
                                             Authentication authentication) {
        return ticketCounterService.ban(id, request == null ? null : request.reason(), adminId(authentication));
    }

    @PostMapping("/non-paying-reports/{id}/dismiss")
    public NonPayingReportResponse dismiss(@PathVariable int id, Authentication authentication) {
        return ticketCounterService.dismiss(id, adminId(authentication));
    }

    @PostMapping("/users/{id}/unban")
    public ResponseEntity<Void> unban(@PathVariable int id, Authentication authentication) {
        ticketCounterService.unban(id, adminId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/appeals")
    public List<AdminAppeal> appeals() {
        return banService.appeals();
    }

    /** Accepting an appeal unbans the user. */
    @PostMapping("/appeals/{id}/accept")
    public AdminAppeal acceptAppeal(@PathVariable int id, @RequestBody(required = false) AppealDecisionRequest request,
                                    Authentication authentication) {
        return banService.accept(id, request == null ? null : request.response(), adminId(authentication));
    }

    @PostMapping("/appeals/{id}/reject")
    public AdminAppeal rejectAppeal(@PathVariable int id, @RequestBody(required = false) AppealDecisionRequest request,
                                    Authentication authentication) {
        return banService.reject(id, request == null ? null : request.response(), adminId(authentication));
    }

    /** SCRUM-81: period is DAY (last 30 days), MONTH (last 12 months) or YEAR (last 5 years). */
    @GetMapping("/stats/earnings")
    public EarningsStats earnings(@RequestParam(defaultValue = "DAY") Period period) {
        return statsService.earnings(period);
    }

    /** SCRUM-80: entries per day; without areaId, for all parking areas. */
    @GetMapping("/stats/entries")
    public EntryStats entries(@RequestParam(required = false) Integer areaId,
                              @RequestParam(defaultValue = "30") int days) {
        return statsService.entries(areaId, days);
    }

    private static int adminId(Authentication authentication) {
        return (Integer) authentication.getPrincipal();
    }
}
