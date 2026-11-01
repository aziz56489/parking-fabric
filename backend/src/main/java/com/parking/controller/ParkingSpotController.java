package com.parking.controller;

import com.parking.dto.ParkingRequests.SpotUpdateRequest;
import com.parking.entity.ParkingSpot;
import com.parking.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking-spots")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
@RequiredArgsConstructor
public class ParkingSpotController {

    private final ParkingService parkingService;

    @GetMapping("/{id}")
    public ParkingSpot getParkingSpot(@PathVariable int id) {
        return parkingService.getParkingSpot(id);
    }

    @PatchMapping("/{id}")
    public ParkingSpot updateParkingSpot(@PathVariable int id, @RequestBody SpotUpdateRequest request) {
        return parkingService.updateParkingSpot(id, request);
    }
}
