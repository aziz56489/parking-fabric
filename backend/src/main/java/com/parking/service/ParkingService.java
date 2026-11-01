package com.parking.service;

import com.parking.dto.ParkingRequests.LayoutRequest;
import com.parking.dto.ParkingRequests.SpotUpdateRequest;
import com.parking.entity.ParkingArea;
import com.parking.entity.ParkingSpot;
import com.parking.entity.SpotStatus;
import com.parking.entity.SpotType;
import com.parking.repository.ParkingAreaRepository;
import com.parking.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParkingService {

    private final ParkingAreaRepository parkingAreaRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    public List<ParkingArea> getParkingAreas() {
        return parkingAreaRepository.findAll();
    }

    public ParkingArea getParkingArea(int id) {
        return parkingAreaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking area not found"));
    }

    public ParkingArea createParkingArea(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The name is required");
        }
        if (parkingAreaRepository.existsByName(name.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A parking area with this name already exists");
        }
        ParkingArea area = new ParkingArea();
        area.setName(name.trim());
        return parkingAreaRepository.save(area);
    }

    public ParkingSpot getParkingSpot(int id) {
        return getSpot(id);
    }

    @Transactional
    public ParkingArea updateLayout(int areaId, LayoutRequest request) {
        if (request == null || request.layout() == null || request.spots() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Layout and spots are required");
        }

        ParkingArea area = getParkingArea(areaId);
        Map<String, SpotType> requestedSpots = new HashMap<>();
        for (LayoutRequest.Spot spot : request.spots()) {
            requestedSpots.put(spot.number(), spot.type());
        }

        for (ParkingSpot spot : List.copyOf(area.getSpots())) {
            if (!requestedSpots.containsKey(spot.getNumber())) {
                checkNotInUse(spot);
                area.getSpots().remove(spot);
            }
        }

        Map<String, ParkingSpot> existingSpots = new HashMap<>();
        for (ParkingSpot spot : area.getSpots()) {
            existingSpots.put(spot.getNumber(), spot);
        }

        requestedSpots.forEach((number, type) -> {
            if (existingSpots.containsKey(number)) {
                existingSpots.get(number).updateType(type);
            } else {
                area.getSpots().add(parkingSpotRepository.save(new ParkingSpot(number, type, area)));
            }
        });

        area.setLayout(request.layout());
        area.setCapacity(area.getSpots().size());
        return area;
    }

    @Transactional
    public ParkingSpot updateParkingSpot(int spotId, SpotUpdateRequest request) {
        if (request == null || request.type() == null && request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one spot field must be provided");
        }

        ParkingSpot spot = getSpot(spotId);
        if (request.type() != null) {
            spot.updateType(request.type());
        }
        if (request.status() != null) {
            if (request.status() == SpotStatus.NOT_AVAILABLE) {
                checkNotInUse(spot);
            }
            spot.updateStatus(request.status());
        }
        return spot;
    }

    private ParkingSpot getSpot(int id) {
        return parkingSpotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking spot not found"));
    }

    private void checkNotInUse(ParkingSpot spot) {
        if (spot.getStatus() == SpotStatus.RESERVED || spot.getStatus() == SpotStatus.OCCUPIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Spot " + spot.getNumber() + " is reserved or occupied");
        }
    }
}
