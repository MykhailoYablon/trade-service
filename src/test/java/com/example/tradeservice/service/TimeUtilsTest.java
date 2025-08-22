package com.example.tradeservice.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilsTest {

    @Test
    void parseIbTime_ShouldParseValidTimeWithTimezone() {
        // Given
        String ibTime = "20250703 09:30:00 US/Eastern";

        // When
        LocalDateTime result = TimeUtils.parseIbTime(ibTime);

        // Then
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(7, result.getMonthValue());
        assertEquals(3, result.getDayOfMonth());
        assertEquals(9, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(0, result.getSecond());
    }

    @Test
    void parseIbTime_ShouldParseValidTimeWithoutTimezone() {
        // Given
        String ibTime = "20250703 09:30:00";

        // When
        LocalDateTime result = TimeUtils.parseIbTime(ibTime);

        // Then
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(7, result.getMonthValue());
        assertEquals(3, result.getDayOfMonth());
        assertEquals(9, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(0, result.getSecond());
    }

    @Test
    void parseIbTime_ShouldThrowRuntimeException_WhenInvalidFormat() {
        // Given
        String invalidTime = "invalid-time-format";

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            TimeUtils.parseIbTime(invalidTime);
        });
    }

    @Test
    void parseIbTime_ShouldThrowRuntimeException_WhenInvalidDate() {
        // Given
        String invalidDate = "20251345 25:70:00 US/Eastern";

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            TimeUtils.parseIbTime(invalidDate);
        });
    }

    @Test
    void parseTimeZone_ShouldReturnCorrectZoneId_ForUSEastern() {
        // Given
        String timezone = "US/Eastern";

        // When
        ZoneId result = TimeUtils.parseTimeZone(timezone);

        // Then
        assertEquals(ZoneId.of("America/New_York"), result);
    }

    @Test
    void parseTimeZone_ShouldReturnCorrectZoneId_ForUSCentral() {
        // Given
        String timezone = "US/Central";

        // When
        ZoneId result = TimeUtils.parseTimeZone(timezone);

        // Then
        assertEquals(ZoneId.of("America/Chicago"), result);
    }

    @Test
    void parseTimeZone_ShouldReturnCorrectZoneId_ForUSMountain() {
        // Given
        String timezone = "US/Mountain";

        // When
        ZoneId result = TimeUtils.parseTimeZone(timezone);

        // Then
        assertEquals(ZoneId.of("America/Denver"), result);
    }

    @Test
    void parseTimeZone_ShouldReturnCorrectZoneId_ForUSPacific() {
        // Given
        String timezone = "US/Pacific";

        // When
        ZoneId result = TimeUtils.parseTimeZone(timezone);

        // Then
        assertEquals(ZoneId.of("America/Los_Angeles"), result);
    }

    @Test
    void parseTimeZone_ShouldReturnCorrectZoneId_ForEuropeLondon() {
        // Given
        String timezone = "Europe/London";

        // When
        ZoneId result = TimeUtils.parseTimeZone(timezone);

        // Then
        assertEquals(ZoneId.of("Europe/London"), result);
    }

    @Test
    void parseTimeZone_ShouldReturnCorrectZoneId_ForAsiaTokyo() {
        // Given
        String timezone = "Asia/Tokyo";

        // When
        ZoneId result = TimeUtils.parseTimeZone(timezone);

        // Then
        assertEquals(ZoneId.of("Asia/Tokyo"), result);
    }

    @Test
    void parseTimeZone_ShouldReturnCorrectZoneId_ForAustraliaSydney() {
        // Given
        String timezone = "Australia/Sydney";

        // When
        ZoneId result = TimeUtils.parseTimeZone(timezone);

        // Then
        assertEquals(ZoneId.of("Australia/Sydney"), result);
    }

    @Test
    void parseTimeZone_ShouldReturnUTC_ForUnknownTimezone() {
        // Given
        String unknownTimezone = "Unknown/Timezone";

        // When
        ZoneId result = TimeUtils.parseTimeZone(unknownTimezone);

        // Then
        assertEquals(ZoneOffset.UTC, result);
    }

    @Test
    void parseTimeZone_ShouldReturnUTC_ForNullTimezone() {
        // Given
        String nullTimezone = null;

        // When
        ZoneId result = TimeUtils.parseTimeZone(nullTimezone);

        // Then
        assertEquals(ZoneOffset.UTC, result);
    }

    @Test
    void parseTimeZone_ShouldReturnUTC_ForEmptyTimezone() {
        // Given
        String emptyTimezone = "";

        // When
        ZoneId result = TimeUtils.parseTimeZone(emptyTimezone);

        // Then
        assertEquals(ZoneOffset.UTC, result);
    }

    @Test
    void parseIbTime_ShouldHandleDifferentTimeFormats() {
        // Test various valid time formats
        String[] validTimes = {
            "20250101 00:00:00 US/Eastern",
            "20251231 23:59:59 US/Pacific",
            "20250615 12:30:45 Europe/London",
            "20250320 15:45:30 Asia/Tokyo"
        };

        for (String time : validTimes) {
            LocalDateTime result = TimeUtils.parseIbTime(time);
            assertNotNull(result, "Failed to parse: " + time);
        }
    }

    @Test
    void parseIbTime_ShouldConvertToUTC_WhenTimezoneProvided() {
        // Given
        String ibTime = "20250703 09:30:00 US/Eastern";
        
        // When
        LocalDateTime result = TimeUtils.parseIbTime(ibTime);
        
        // Then
        // The result should be in UTC, which would be 4 hours ahead of Eastern time in July (EDT)
        // So 9:30 AM EDT should be 1:30 PM UTC
        assertEquals(13, result.getHour());
        assertEquals(30, result.getMinute());
    }
}
