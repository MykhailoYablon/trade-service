package com.strategy.trade.service.csv;

import com.strategy.trade.strategy.dataclient.TwelveDataClient;
import com.strategy.trade.model.StockResponse;
import com.strategy.trade.model.enums.TimeFrame;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class HistoricalDataService {

    private final TwelveDataClient twelveDataClient;
    private final CsvServiceImpl csvServiceImpl;

    private static final DateTimeFormatter API_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss");
    private static final DateTimeFormatter RESPONSE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30, 0);
    private static final Set<LocalDate> US_HOLIDAYS = getUSHolidays(2025);
    private static final int RATE_LIMIT_DELAY_MS = 7600; // 7.6 seconds between calls (8 calls per minute)
    private static int totalApiCalls = 0;


    public void collectYearlyDataEfficiently(String symbol, TimeFrame timeFrame, int year) {
        int allFilteredDataSize = 0;
        // Loop through each month
        for (int month = 1; month <= 12; month++) {
            try {
                log.info("Processing month {}/{}", month, year);

                // Get first and last day of the month
                LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
                LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

                List<StockResponse.Value> monthData;
                // API call for entire month (9:30 AM to 4:00 PM)

                if (TimeFrame.FIVE_MIN.equals(timeFrame)) {
                    // Process 5-minute timeframe in 3 chunks to stay under 5000 record limit
                    monthData = processMonthInChunks(symbol, timeFrame, firstDayOfMonth, lastDayOfMonth, 3);
                } else {
                    // Process 1-minute timeframe in smaller chunks (e.g., daily or weekly)
                    monthData = processMonthInChunks(symbol, timeFrame, firstDayOfMonth, lastDayOfMonth, 4);
//                            calculateOptimalChunks(firstDayOfMonth, lastDayOfMonth));
                }

                if (!monthData.isEmpty()) {
                    List<StockResponse.Value> filteredData = limitFirstIntervalsPerDay(monthData, timeFrame);

                    csvServiceImpl.exportToCsvTwelve(symbol, timeFrame, filteredData);
                    allFilteredDataSize += filteredData.size();

                    log.info("Month {}: Retrieved {} records, filtered to {} records",
                            month, monthData.size(), filteredData.size());
                } else {
                    log.warn("No data returned for month {}", month);
                }

                // Rate limiting: wait 8 seconds between API calls
                if (month < 12) { // Don't wait after the last month
                    log.info("Waiting 7.6 seconds for rate limit...");
                    Thread.sleep(RATE_LIMIT_DELAY_MS);
                }

            } catch (Exception e) {
                log.error("Error processing month {}: {}", month, e.getMessage(), e);
                // Continue with next month
            }
        }

        log.info("Collection complete! Total API calls: {}, Total filtered records: {}",
                totalApiCalls, allFilteredDataSize);
    }

    public void collectCloseDataPerDay(String symbol) {
        String csv = twelveDataClient.csvTimeSeries(symbol, null, null);

        log.info("Got csv");

        csvServiceImpl.writeDayCsv(symbol, csv);

        log.info("Data fetched and saved");
    }

    public List<StockResponse.Value> limitFirstIntervalsPerDay(List<StockResponse.Value> monthData,
                                                               TimeFrame timeFrame) {
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
                    .filter(data -> TimeFrame.ONE_MIN.equals(timeFrame) ?
                            isAfterFifteenInterval(parseDateTime(data.getDatetime())) :
                            isWithinFirstThreeIntervals(parseDateTime(data.getDatetime())))
                    .sorted(Comparator.comparing(data -> parseDateTime(data.getDatetime())))
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

    private boolean isAfterFifteenInterval(LocalDateTime timestamp) {
        LocalTime time = timestamp.toLocalTime();
        LocalTime cutoffTime = MARKET_OPEN.plusMinutes(15); // First 3 intervals of 5 minutes = 15 minutes

        return time.isAfter(cutoffTime) && time.isBefore(MARKET_OPEN.plusHours(2));
    }

    private boolean isTradingDay(LocalDate date) {
        return !isNonTradingDay(date);
    }

    public static boolean isNonTradingDay(LocalDate date) {
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

    /**
     * Process a month in chunks to avoid the 5000 record API limit
     */
    private List<StockResponse.Value> processMonthInChunks(String symbol, TimeFrame timeFrame,
                                                           LocalDate firstDay, LocalDate lastDay,
                                                           int numberOfChunks) {
        List<StockResponse.Value> allChunkData = new ArrayList<>();

        // Calculate chunk duration in days
        long totalDays = ChronoUnit.DAYS.between(firstDay, lastDay) + 1;
        long daysPerChunk = Math.max(1, totalDays / numberOfChunks);

        LocalDate chunkStart = firstDay;
        int chunkNumber = 1;

        while (!chunkStart.isAfter(lastDay)) {
            LocalDate chunkEnd = chunkStart.plusDays(daysPerChunk - 1);
            if (chunkEnd.isAfter(lastDay)) {
                chunkEnd = lastDay;
            }

            try {
                String startTime;
                String endTime;

                if (TimeFrame.FIVE_MIN.equals(timeFrame)) {
                    startTime = chunkStart.atTime(9, 30, 0).format(API_FORMATTER);
                    endTime = chunkEnd.atTime(9, 46, 0).format(API_FORMATTER);
                } else {
                    startTime = chunkStart.atTime(9, 46, 0).format(API_FORMATTER);
                    endTime = chunkEnd.atTime(11, 0, 0).format(API_FORMATTER);
                }

                log.info("API Call {}: Chunk {}/{} - {} to {}", ++totalApiCalls, chunkNumber, numberOfChunks, startTime, endTime);

                List<StockResponse.Value> chunkData = twelveDataClient.timeSeries(symbol, timeFrame.getTwelveFormat(), startTime, endTime).getValues();

                if (chunkData != null && !chunkData.isEmpty()) {
                    allChunkData.addAll(chunkData);
                    log.info("Chunk {}: Retrieved {} records", chunkNumber, chunkData.size());

                    // Check if we're approaching the 5000 record limit
                    if (chunkData.size() >= 4500) {
                        log.warn("Chunk {} returned {} records - close to API limit. Consider smaller chunks.",
                                chunkNumber, chunkData.size());
                    }
                } else {
                    log.warn("No data returned for chunk {} ({} to {})", chunkNumber, chunkStart, chunkEnd);
                }

                // Rate limiting: wait 8 seconds between API calls (except for the last chunk of the last month)
                if (!chunkStart.plusDays(daysPerChunk).isAfter(lastDay) || chunkNumber < numberOfChunks) {
                    log.info("Waiting chunk for rate limit...");
                    Thread.sleep(RATE_LIMIT_DELAY_MS);
                }

            } catch (Exception e) {
                log.error("Error processing chunk {} ({} to {}): {}", chunkNumber, chunkStart, chunkEnd, e.getMessage(), e);
                // Continue with next chunk
            }

            chunkStart = chunkStart.plusDays(daysPerChunk);
            chunkNumber++;
        }

        return allChunkData;
    }
}
