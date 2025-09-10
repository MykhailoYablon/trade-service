package com.example.tradeservice.strategy.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class FileUtils {

    /**
     * Creates a log file name with current date
     * Format: OpeningBreakRange-YYYY-MM-DD.log
     */
    public static String createLogFileName(String prefix, String date) {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateString = Objects.nonNull(date) ? date : currentDate.format(formatter);
        return prefix + dateString + ".log";
    }

    /**
     * Writes a line to the log file
     *
     * @param fileName The log file name
     * @param message  The message to write
     */
    public static void writeToLog(String fileName, String message) {
        new File("logs").mkdirs();
        try (FileWriter writer = new FileWriter("logs/" + fileName, true)) {
            // Add timestamp to each log entry
            String timestamp = LocalDateTime.now().toString();
            writer.write("[" + timestamp + "] " + message + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

}
