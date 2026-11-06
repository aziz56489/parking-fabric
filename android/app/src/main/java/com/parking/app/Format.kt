package com.parking.app

import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import kotlin.math.roundToInt

fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.roundToInt()} m" else "%.1f km".format(meters / 1000)

fun formatDuration(minutes: Long): String =
    if (minutes < 60) "$minutes min" else "${minutes / 60} h %02d min".format(minutes % 60)

/** A running clock, for example 1:05:09. */
fun formatElapsed(duration: Duration): String {
    val seconds = duration.seconds.coerceAtLeast(0)
    return "%d:%02d:%02d".format(seconds / 3600, seconds % 3600 / 60, seconds % 60)
}

/** Uses the currency's own decimals: 0.025 TND (millimes), 1.50 EUR. */
fun formatMoney(amount: Double, currency: String): String =
    runCatching {
        val code = Currency.getInstance(currency)
        NumberFormat.getCurrencyInstance().apply {
            this.currency = code
            minimumFractionDigits = code.defaultFractionDigits
            maximumFractionDigits = code.defaultFractionDigits
        }.format(amount)
    }.getOrElse { "$amount $currency" }

private val timeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

fun formatTime(isoInstant: String): String =
    timeFormatter.withZone(ZoneId.systemDefault()).format(Instant.parse(isoInstant))

/** Same rule as the backend: every started minute counts, at least one. */
fun billedMinutes(start: Instant, end: Instant): Long =
    maxOf(1, (Duration.between(start, end).seconds + 59) / 60)

fun cost(pricePerHour: Double, minutes: Long): Double = pricePerHour * minutes / 60

/** The receipt as label/value lines, used by the receipt screen and the share text. */
fun receiptLines(receipt: Reservation): List<Pair<String, String>> = listOf(
    "Receipt no." to "#${receipt.id}",
    "Parking" to receipt.areaName,
    "Spot" to receipt.spotNumber,
    "Arrival" to formatTime(receipt.startedAt),
    "Exit" to (receipt.endedAt?.let(::formatTime) ?: "Still parked"),
    "Time spent" to formatDuration(receipt.minutes),
    "Rate" to "${formatMoney(receipt.pricePerHour, receipt.currency)} / hour",
    "Total" to (receipt.amount?.let { formatMoney(it, receipt.currency) } ?: "-"),
)
