package com.parking.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.entity.ParkingArea;
import com.parking.external.WeatherClient.OpenMeteoResponse;
import com.parking.service.ParkingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Current weather at a parking area, from the latitude / longitude saved in its layout. */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final ParkingService parkingService;
    private final WeatherClient weatherClient;
    private final ObjectMapper objectMapper;

    public WeatherService(ParkingService parkingService, WeatherClient weatherClient, ObjectMapper objectMapper) {
        this.parkingService = parkingService;
        this.weatherClient = weatherClient;
        this.objectMapper = objectMapper;
    }

    public AreaWeather getWeather(int areaId) {
        ParkingArea area = parkingService.getParkingArea(areaId);
        double[] position = position(area);
        OpenMeteoResponse response;
        try {
            response = weatherClient.currentWeather(position[0], position[1]);
        } catch (RestClientException e) {
            log.warn("Open-Meteo call failed for area {}: {}", areaId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The weather service is unavailable, try again later");
        }
        if (response == null || response.current() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unexpected answer from the weather service");
        }
        var current = response.current();
        return new AreaWeather(area.getId(), area.getName(), position[0], position[1], current.time(),
                response.timezone(), current.temperature(), current.precipitation(), current.windSpeed(),
                current.weatherCode(), describe(current.weatherCode()), "open-meteo.com");
    }

    private double[] position(ParkingArea area) {
        try {
            JsonNode layout = area.getLayout() == null ? null : objectMapper.readTree(area.getLayout());
            if (layout != null && layout.hasNonNull("latitude") && layout.hasNonNull("longitude")) {
                return new double[]{layout.get("latitude").asDouble(), layout.get("longitude").asDouble()};
            }
        } catch (Exception e) {
            log.warn("Unreadable layout for area {}: {}", area.getId(), e.getMessage());
        }
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "This parking area has no latitude / longitude in its layout");
    }

    /** WMO weather interpretation codes used by Open-Meteo. */
    static String describe(Integer code) {
        if (code == null) return "Unknown";
        if (code == 0) return "Clear sky";
        if (code <= 3) return "Partly cloudy";
        if (code <= 48) return "Fog";
        if (code <= 57) return "Drizzle";
        if (code <= 67) return "Rain";
        if (code <= 77) return "Snow";
        if (code <= 82) return "Rain showers";
        if (code <= 86) return "Snow showers";
        return "Thunderstorm";
    }

    public record AreaWeather(Integer areaId, String areaName, double latitude, double longitude, String time,
                              String timezone, Double temperatureCelsius, Double precipitationMm,
                              Double windSpeedKmh, Integer weatherCode, String description, String source) {
    }
}
