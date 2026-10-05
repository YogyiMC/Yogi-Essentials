package dev.yogi.yogiessentials.recovery;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

final class RecoveryHistory {
    private static final String HISTORY_SCHEMA = "3";
    private RecoveryHistory() {
    }

    static Path record(Path gameDir, RecoveryAnalysis analysis) {
        Path dir = gameDir.resolve("yogiessentials").resolve("recovery").resolve("history");
        long timestamp = System.currentTimeMillis();
        Path file = dir.resolve(timestamp + ".properties");
        try {
            Files.createDirectories(dir);
            Properties properties = new Properties();
            properties.setProperty("historySchema", HISTORY_SCHEMA);
            properties.setProperty("timestamp", Long.toString(timestamp));
            properties.setProperty("title", safe(analysis.title()));
            properties.setProperty("summary", safe(analysis.summary()));
            properties.setProperty("confidence", analysis.confidence().name());
            properties.setProperty("source", analysis.sourcePath() == null ? "" : analysis.sourcePath().toString());
            properties.setProperty("details", safe(analysis.details()));
            properties.setProperty("suspectCount", Integer.toString(analysis.suspects().size()));
            for (int i = 0; i < analysis.suspects().size(); i++) {
                RecoveryAnalysis.Suspect suspect = analysis.suspects().get(i);
                properties.setProperty("suspect." + i + ".id", safe(suspect.id()));
                properties.setProperty("suspect." + i + ".name", safe(suspect.displayName()));
                properties.setProperty("suspect." + i + ".version", safe(suspect.version()));
                properties.setProperty("suspect." + i + ".confidence", suspect.confidence().name());
                properties.setProperty("suspect." + i + ".reason", safe(suspect.reason()));
            }
            properties.setProperty("issueCount", Integer.toString(analysis.issues().size()));
            for (int i = 0; i < analysis.issues().size(); i++) {
                RecoveryAnalysis.Issue issue = analysis.issues().get(i);
                properties.setProperty("issue." + i + ".type", issue.type().name());
                properties.setProperty("issue." + i + ".title", safe(issue.title()));
                properties.setProperty("issue." + i + ".description", safe(issue.description()));
                properties.setProperty("issue." + i + ".confidence", issue.confidence().name());
            }
            storeProperties(file, properties);
            trimOld(dir, 40);
            return file;
        } catch (IOException ignored) {
            return null;
        }
    }

    static List<HistoryEntry> list(Path gameDir) {
        Path dir = gameDir.resolve("yogiessentials").resolve("recovery").resolve("history");
        List<HistoryEntry> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (var stream = Files.list(dir)) {
            for (Path file : stream.filter(path -> path.getFileName().toString().endsWith(".properties")).toList()) {
                Properties properties = loadProperties(file);
                if (!HISTORY_SCHEMA.equals(properties.getProperty("historySchema", ""))) {
                    continue;
                }
                result.add(new HistoryEntry(
                        parseLong(properties.getProperty("timestamp"), modifiedMillis(file)),
                        properties.getProperty("title", "Minecraft crash"),
                        properties.getProperty("summary", ""),
                        properties.getProperty("confidence", "NONE"),
                        properties.getProperty("suspect.0.name", ""),
                        properties.getProperty("details", ""),
                        file
                ));
            }
        } catch (IOException ignored) {
        }
        result.sort(Comparator.comparingLong(HistoryEntry::timestamp).reversed());
        return result;
    }

    private static void trimOld(Path dir, int max) {
        try (var stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .sorted(Comparator.comparingLong(RecoveryHistory::modifiedMillis).reversed())
                    .toList();
            for (int i = max; i < files.size(); i++) {
                Files.deleteIfExists(files.get(i));
            }
        } catch (IOException ignored) {
        }
    }

    private static Properties loadProperties(Path file) {
        Properties properties = new Properties();
        if (!Files.isRegularFile(file)) {
            return properties;
        }
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        } catch (IOException ignored) {
        }
        return properties;
    }

    private static void storeProperties(Path file, Properties properties) throws IOException {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temp)) {
            properties.store(output, "Yogi Essentials Crash Assistant History");
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long modifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    record HistoryEntry(
            long timestamp,
            String title,
            String summary,
            String confidence,
            String suspect,
            String details,
            Path file
    ) {
    }
}
