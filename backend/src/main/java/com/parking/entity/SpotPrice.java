package com.parking.entity;

import java.math.BigDecimal;

/** Hourly price of one spot of a parking area, stored in parking_area.spot_pricing. */
public record SpotPrice(String spotNumber, BigDecimal pricePerHour) {
}
