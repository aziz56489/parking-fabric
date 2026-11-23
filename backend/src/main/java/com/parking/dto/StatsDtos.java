package com.parking.dto;

import java.math.BigDecimal;
import java.util.List;

/** Parking statistics for the admin (SCRUM-80, SCRUM-81). Dates are ISO yyyy-MM-dd in the parking's time zone. */
public final class StatsDtos {

    private StatsDtos() {
    }

    public enum Period { DAY, MONTH, YEAR }

    /** Earnings of one day, month or year; {@code start} is its first day. */
    public record EarningsBucket(String start, String label, BigDecimal total, long exits) {
    }

    /** SCRUM-81: daily, monthly and yearly earnings. */
    public record EarningsStats(
            Period period,
            String currency,
            BigDecimal today,
            BigDecimal thisMonth,
            BigDecimal thisYear,
            List<EarningsBucket> buckets) {
    }

    public record DailyEntries(String date, String label, long entries) {
    }

    public record AreaEntries(Integer areaId, String areaName, long today, long total, double averagePerDay) {
    }

    /** SCRUM-80: entries per day, for one area or all of them, over the last {@code days} days. */
    public record EntryStats(
            Integer areaId,
            int days,
            long today,
            double averagePerDay,
            DailyEntries peak,
            List<DailyEntries> daily,
            List<AreaEntries> areas) {
    }
}
