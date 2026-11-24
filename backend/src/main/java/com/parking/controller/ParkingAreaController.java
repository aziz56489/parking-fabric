package com.parking.controller;

import com.parking.dto.ParkingRequests.CreateParkingAreaRequest;
import com.parking.dto.ParkingRequests.LayoutRequest;
import com.parking.entity.ParkingArea;
import com.parking.external.WeatherService;
import com.parking.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/parking-areas")
@RequiredArgsConstructor
public class ParkingAreaController {

    private final ParkingService parkingService;
    private final WeatherService weatherService;

    @GetMapping
    public List<ParkingArea> getParkingAreas() {
        return parkingService.getParkingAreas();
    }

    @GetMapping("/{id}")
    public ParkingArea getParkingArea(@PathVariable int id) {
        return parkingService.getParkingArea(id);
    }

    /** Current weather at the area, from the external Open-Meteo service. */
    @GetMapping("/{id}/weather")
    public WeatherService.AreaWeather getWeather(@PathVariable int id) {
        return weatherService.getWeather(id);
    }

    @PostMapping
    public ResponseEntity<ParkingArea> createParkingArea(@RequestBody CreateParkingAreaRequest request) {
        ParkingArea area = parkingService.createParkingArea(request.name());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(area.getId())
                .toUri();
        return ResponseEntity.created(location).body(area);
    }

    @PutMapping("/{id}/layout")
    public ParkingArea updateLayout(@PathVariable int id, @RequestBody LayoutRequest request) {
        return parkingService.updateLayout(id, request);
    }
}
