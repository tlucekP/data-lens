package com.datalens.diagnostics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppDiagnostics {
    private static final Logger ROOT_LOGGER = Logger.getLogger("com.datalens");
    private static volatile boolean configured;

    private AppDiagnostics() {
    }

    public static Logger getLogger(Class<?> type) {
        configure();
        return Logger.getLogger(type.getName());
    }

    private static void configure() {
        if (configured) {
            return;
        }

        synchronized (AppDiagnostics.class) {
            if (configured) {
                return;
            }

            ROOT_LOGGER.setUseParentHandlers(false);
            ROOT_LOGGER.setLevel(Level.INFO);

            try {
                Path logDirectory = Path.of(System.getProperty("user.home"), ".datalens", "logs");
                Files.createDirectories(logDirectory);

                FileHandler fileHandler = new FileHandler(
                        logDirectory.resolve("datalens.log").toString(),
                        1_000_000,
                        3,
                        true
                );
                fileHandler.setEncoding(StandardCharsets.UTF_8.name());
                fileHandler.setFormatter(new SimpleFormatter());
                ROOT_LOGGER.addHandler(fileHandler);
            } catch (IOException exception) {
                Logger.getLogger(AppDiagnostics.class.getName()).log(Level.WARNING,
                        "Diagnostics logging could not be configured.", exception);
            }

            configured = true;
        }
    }
}
