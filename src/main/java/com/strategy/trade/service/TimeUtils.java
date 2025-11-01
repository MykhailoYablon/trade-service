package com.strategy.trade.service;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class TimeUtils {
    public static LocalDateTime parseIbTime(String ibTime) {
        // IB returns time in format "20250703 09:30:00 US/Eastern"
        try {
            // Split the timezone part
            String[] parts = ibTime.split(" ");
            if (parts.length == 3) {
                // Format: "20250703 09:30:00 US/Eastern"
                String dateTimePart = parts[0] + " " + parts[1];
                String timezonePart = parts[2];

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
                LocalDateTime localDateTime = LocalDateTime.parse(dateTimePart, formatter);

                // Convert from market timezone to UTC for storage
                ZoneId marketZone = parseTimeZone(timezonePart);
                ZonedDateTime marketTime = localDateTime.atZone(marketZone);

                // Store as UTC in database
                return marketTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            } else {
                // Fallback for format without timezone
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
                return LocalDateTime.parse(ibTime, formatter);
            }
        } catch (Exception e) {
            log.error("Failed to parse IB time: {}", ibTime, e);
            throw new RuntimeException("Invalid date format from IB: " + ibTime, e);
        }
    }

    public static ZoneId parseTimeZone(String timezone) {
        // Map IB timezone strings to Java ZoneId
        switch (timezone) {
            case "US/Eastern":
                return ZoneId.of("America/New_York");
            case "US/Central":
                return ZoneId.of("America/Chicago");
            case "US/Mountain":
                return ZoneId.of("America/Denver");
            case "US/Pacific":
                return ZoneId.of("America/Los_Angeles");
            case "Europe/London":
                return ZoneId.of("Europe/London");
            case "Asia/Tokyo":
                return ZoneId.of("Asia/Tokyo");
            case "Australia/Sydney":
                return ZoneId.of("Australia/Sydney");
            default:
                log.warn("Unknown timezone from IB: {}, defaulting to UTC", timezone);
                return ZoneOffset.UTC;
        }
    }
}
