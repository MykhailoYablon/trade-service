package com.example.tradeservice.service.impl;

import com.example.tradeservice.configuration.TwelveDataClient;
import com.example.tradeservice.model.StockResponse;
import com.example.tradeservice.model.enums.TimeFrame;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class YearlyHistoricalDataService {

    private final HistoricalDataCsvService excelService;
    private final TwelveDataClient twelveDataClient;

    private static final DateTimeFormatter API_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss");
    private static final DateTimeFormatter RESPONSE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30, 0);
    private static final Set<LocalDate> US_HOLIDAYS = getUSHolidays(2025);
    private static final int RATE_LIMIT_DELAY_MS = 8000; // 8 seconds between calls (8 calls per minute)

    public List<StockResponse.Value> collectYearlyDataEfficiently(String symbol, TimeFrame timeFrame, int year,
                                                                  Integer limitCandles) {
        List<StockResponse.Value> allFilteredData = new ArrayList<>();
        int totalApiCalls = 0;

        // Loop through each month
        for (int month = 1; month <= 12; month++) {
            try {
                log.info("Processing month {}/{}", month, year);

                // Get first and last day of the month
                LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
                LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

                // API call for entire month (9:30 AM to 4:00 PM)
                String startTime = firstDayOfMonth.atTime(9, 30, 0).format(API_FORMATTER);
                String endTime = lastDayOfMonth.atTime(12, 0, 0).format(API_FORMATTER);

                log.info("API Call {}: {} to {}", ++totalApiCalls, startTime, endTime);

                List<StockResponse.Value> monthData = twelveDataClient.timeSeries(symbol, timeFrame.getTwelveFormat(), startTime, endTime).getValues();

                if (monthData != null && !monthData.isEmpty()) {
                    // Filter to get only first 3 intervals per trading day
                    if (Objects.nonNull(limitCandles)) {
                        List<StockResponse.Value> filteredData = limitFirstIntervalsPerDay(monthData, limitCandles);
                        allFilteredData.addAll(filteredData);
                        log.info("Month {}: Retrieved {} records, filtered to {} records",
                                month, monthData.size(), filteredData.size());
                    } else {
                        allFilteredData = monthData;
                    }

                } else {
                    log.warn("No data returned for month {}", month);
                }

                // Rate limiting: wait 8 seconds between API calls
                if (month < 12) { // Don't wait after the last month
                    log.info("Waiting 8 seconds for rate limit...");
                    Thread.sleep(RATE_LIMIT_DELAY_MS);
                }

            } catch (Exception e) {
                log.error("Error processing month {}: {}", month, e.getMessage(), e);
                // Continue with next month
            }
        }

        log.info("Collection complete! Total API calls: {}, Total filtered records: {}",
                totalApiCalls, allFilteredData.size());

        return allFilteredData;
    }

    public List<StockResponse.Value> limitFirstIntervalsPerDay(List<StockResponse.Value> monthData, int limit) {
        Map<LocalDate, List<StockResponse.Value>> dataByDate = monthData.stream()
                .filter(data -> isTradingDay(parseDateTime(data.getDatetime()).toLocalDate()))
                .collect(Collectors.groupingBy(
                        data -> parseDateTime(data.getDatetime()).toLocalDate(),
                        TreeMap::new, // Keep dates sorted
                        Collectors.toList()
                ));

        List<StockResponse.Value> filteredData = new ArrayList<>();

        for (Map.Entry<LocalDate, List<StockResponse.Value>> entry : dataByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<StockResponse.Value> dayData = entry.getValue();

            // Sort by datetime and get first 3 intervals after market open
            List<StockResponse.Value> first3Intervals = dayData.stream()
                    .filter(data -> isWithinFirstThreeIntervals(parseDateTime(data.getDatetime())))
                    .sorted(Comparator.comparing(data -> parseDateTime(data.getDatetime())))
                    .limit(limit)
                    .toList();

            filteredData.addAll(first3Intervals);

            if (!first3Intervals.isEmpty()) {
                LocalDateTime firstTime = parseDateTime(first3Intervals.get(0).getDatetime());
                log.info("Date {}: Selected {} intervals starting from {}",
                        date, first3Intervals.size(), firstTime.toLocalTime());
            }
        }

        return filteredData;
    }

    private LocalDateTime parseDateTime(String datetime) {
        return LocalDateTime.parse(datetime, RESPONSE_FORMATTER);
    }

    private boolean isWithinFirstThreeIntervals(LocalDateTime timestamp) {
        LocalTime time = timestamp.toLocalTime();
        LocalTime marketOpen = MARKET_OPEN;
        LocalTime cutoffTime = marketOpen.plusMinutes(15); // First 3 intervals of 5 minutes = 15 minutes

        return !time.isBefore(marketOpen) && !time.isAfter(cutoffTime);
    }

    private boolean isTradingDay(LocalDate date) {
        return !isNonTradingDay(date);
    }

    private boolean isNonTradingDay(LocalDate date) {
        // Skip weekends
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return true;
        }

        // Skip US market holidays
        return US_HOLIDAYS.contains(date);
    }

    // US Market holidays for the specified year
    private static Set<LocalDate> getUSHolidays(int year) {
        Set<LocalDate> holidays = new HashSet<>();

        // Fixed date holidays
        holidays.add(LocalDate.of(year, 1, 1));   // New Year's Day
        holidays.add(LocalDate.of(year, 7, 4));   // Independence Day
        holidays.add(LocalDate.of(year, 12, 25)); // Christmas Day

        // Variable date holidays
        holidays.add(getNthWeekdayOfMonth(year, 1, DayOfWeek.MONDAY, 3));  // MLK Day
        holidays.add(getNthWeekdayOfMonth(year, 2, DayOfWeek.MONDAY, 3));  // Presidents' Day
        holidays.add(getLastWeekdayOfMonth(year, 5, DayOfWeek.MONDAY));    // Memorial Day
        holidays.add(getNthWeekdayOfMonth(year, 9, DayOfWeek.MONDAY, 1));  // Labor Day
        holidays.add(getNthWeekdayOfMonth(year, 11, DayOfWeek.THURSDAY, 4)); // Thanksgiving

        return holidays;
    }

    private static LocalDate getNthWeekdayOfMonth(int year, int month, DayOfWeek dayOfWeek, int n) {
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        LocalDate firstWeekday = firstOfMonth.with(java.time.temporal.TemporalAdjusters.firstInMonth(dayOfWeek));
        return firstWeekday.plusWeeks(n - 1);
    }

    private static LocalDate getLastWeekdayOfMonth(int year, int month, DayOfWeek dayOfWeek) {
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        return firstOfMonth.with(java.time.temporal.TemporalAdjusters.lastInMonth(dayOfWeek));
    }
}
