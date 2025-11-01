package com.strategy.trade.strategy.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class FileUtils {

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
