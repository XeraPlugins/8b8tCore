package me.txmc.core.util;

import lombok.Getter;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Handles logging for map creation events.
 *
 * <p>This class is part of the 8b8tCore plugin and is responsible for managing
 * the logging of map creation details into a dedicated log file.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Creating a directory for storing log files if it does not exist</li>
 *     <li>Initializing a file handler for logging map creation events</li>
 * </ul>
 *
 * <p>Log File:</p>
 * <ul>
 *     <li><b>File Path:</b> critical-logs-do-not-delete/map_creation.log</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/27 11:42 AM
 */
public class MapCreationLogger {
    @Getter
    private static final Logger logger = Logger.getLogger("MapCreationLogger");
    private static FileHandler fileHandler;

    static {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("critical-logs-do-not-delete"));

            fileHandler = new FileHandler("critical-logs-do-not-delete/map_creation.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException ignored) {

        }
    }
}
