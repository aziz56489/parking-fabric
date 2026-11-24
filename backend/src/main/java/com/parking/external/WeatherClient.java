package com.parking.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * REST client for an external service: Open-Meteo (https://open-meteo.com), free and without an API key.
 * The RestClient.Builder comes from Spring Boot, so each call is measured (Prometheus) and traced (Tempo).
 */
@Component
public class WeatherClient {

    private final RestClient restClient;

    public WeatherClient(RestClient.Builder builder,
                         @Value("${parking.weather.base-url}") String baseUrl,
                         @Value("${parking.weather.connect-timeout}") Duration connectTimeout,
                         @Value("${parking.weather.read-timeout}") Duration readTimeout) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(connectTimeout)
                        .withReadTimeout(readTimeout)))
                .build();
    }

    /** GET /v1/forecast?latitude=..&longitude=..&current=... */
    public OpenMeteoResponse currentWeather(double latitude, double longitude) {
        return restClient.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("current", "temperature_2m,precipitation,weather_code,wind_speed_10m")
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .body(OpenMeteoResponse.class);
    }

    public record OpenMeteoResponse(double latitude, double longitude, String timezone, Current current,
                                    @JsonProperty("current_units") CurrentUnits currentUnits) {
    }

    public record Current(String time,
                          @JsonProperty("temperature_2m") Double temperature,
                          Double precipitation,
                          @JsonProperty("weather_code") Integer weatherCode,
                          @JsonProperty("wind_speed_10m") Double windSpeed) {
    }

    public record CurrentUnits(@JsonProperty("temperature_2m") String temperature,
                               String precipitation,
                               @JsonProperty("wind_speed_10m") String windSpeed) {
    }
}
