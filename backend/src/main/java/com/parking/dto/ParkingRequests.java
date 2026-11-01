package com.parking.dto;

import com.parking.entity.SpotStatus;
import com.parking.entity.SpotType;

import java.util.List;

public final class ParkingRequests {

    private ParkingRequests() {
    }

    public record CreateParkingAreaRequest(String name) {
    }

    public record LayoutRequest(String layout, List<Spot> spots) {

        public record Spot(String number, SpotType type) {
        }
    }

    public record SpotUpdateRequest(SpotType type, SpotStatus status) {
    }
}
