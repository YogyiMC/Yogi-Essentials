package dev.yogi.yogiessentials.recovery;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class RecoveryAssistantMain {
    private static final String ANALYSIS_SCHEMA = "3";
    private RecoveryAssistantMain() {
    }

    public static void main(String[] args) {
        Map<String, String> arguments = parseArguments(args);
        Path gameDir = Path.of(arguments.getOrDefault("game-dir", ".")).toAbsolutePath().normalize();

        if (arguments.containsKey("persistent")) {
            PersistentRecoveryMonitor.run(gameDir);
            return;
        }

        if (arguments.containsKey("preflight")) {
            RecoveryWindow.show(gameDir, RecoveryAnalyzer.analyzeStartupEnvironment(gameDir));
            return;
        }

        if (arguments.containsKey("test")) {
            RecoveryWindow.show(gameDir, RecoveryAnalyzer.testAnalysis(gameDir));
            return;
        }

        if (arguments.containsKey("self-tests")) {
            RecoveryWindow.showTextReport("Yogi Essentials — Diagnosis Tests", RecoverySelfTest.run(gameDir));
            return;
        }

        if (arguments.containsKey("history")) {
            RecoveryWindow.showHistory(gameDir);
            return;
        }

        if (arguments.containsKey("disabled-manager")) {
            RecoveryWindow.showDisabledManager(gameDir);
            return;
        }

        if (arguments.containsKey("analyze-now")) {
            long startedAt = parseLong(arguments.get("session-start"), 0L);
            RecoveryWindow.show(gameDir, RecoveryAnalyzer.analyzeCurrentLogs(gameDir, startedAt));
            return;
        }

        if (arguments.containsKey("recover-session")) {
            Path previousSession = arguments.containsKey("session-file")
                    ? Path.of(arguments.get("session-file")).toAbsolutePath().normalize()
                    : null;
            if (previousSession == null) {
                return;
            }
            Properties session = loadProperties(previousSession);
            long startedAt = parseLong(session.getProperty("startedAt"), 0L);
            RecoveryAnalysis analysis = RecoveryAnalyzer.analyze(gameDir, startedAt);
            persistRealCrash(gameDir, analysis);
            updateSessionState(previousSession, "RECOVERED");
            RecoveryWindow.show(gameDir, analysis);
            return;
        }

        long pid = parseLong(arguments.get("watch"), -1L);
        Path sessionFile = arguments.containsKey("session-file")
                ? Path.of(arguments.get("session-file")).toAbsolutePath().normalize()
                : gameDir.resolve("yogiessentials").resolve("recovery").resolve("session.properties");
        Path lockFile = arguments.containsKey("lock-file")
                ? Path.of(arguments.get("lock-file")).toAbsolutePath().normalize()
                : null;
        Path watchStateFile = arguments.containsKey("watch-state")
                ? Path.of(arguments.get("watch-state")).toAbsolutePath().normalize()
                : null;

        if (pid <= 0L) {
            return;
        }

        writeWatcherState(watchStateFile, "READY", pid);
        boolean unexpected = waitForGameEnd(pid, lockFile, sessionFile, watchStateFile);
        if (!unexpected) {
            writeWatcherState(watchStateFile, "CLEAN_EXIT", pid);
            return;
        }

        writeWatcherState(watchStateFile, "PROCESS_ENDED", pid);
        sleep(1800L);

        Properties session = loadProperties(sessionFile);
        long startedAt = parseLong(session.getProperty("startedAt"), 0L);
        boolean clean = "CLEAN".equalsIgnoreCase(session.getProperty("state", "RUNNING"));
        boolean newCrashReport = hasNewCrashReport(gameDir, startedAt);
        if (clean && !newCrashReport) {
            writeWatcherState(watchStateFile, "CLEAN_EXIT", pid);
            return;
        }

        RecoveryAnalysis analysis = RecoveryAnalyzer.analyze(gameDir, startedAt);
        persistRealCrash(gameDir, analysis);
        updateSessionState(sessionFile, "RECOVERED");
        writeWatcherState(watchStateFile, "WINDOW_OPEN", pid);
        RecoveryWindow.show(gameDir, analysis);
    }

    private static void persistRealCrash(Path gameDir, RecoveryAnalysis analysis) {
        if (!isRealCrash(analysis)) {
            return;
        }
        writeLastAnalysis(gameDir, analysis, "CRASH");
        RecoveryHistory.record(gameDir, analysis);
    }

    static void persistStartupAnalysis(Path gameDir, RecoveryAnalysis analysis) {
        if (analysis == null || analysis.issues().isEmpty()) {
            return;
        }
        writeLastAnalysis(gameDir, analysis, "STARTUP");
        RecoveryHistory.record(gameDir, analysis);
    }

    private static boolean isRealCrash(RecoveryAnalysis analysis) {
        return analysis.issues().stream().anyMatch(issue -> switch (issue.type()) {
            case MIXIN, DEPENDENCY, INCOMPATIBILITY, CONFIG, DIRECT_EXCEPTION, LINKAGE, NATIVE -> true;
            case DUPLICATE_MOD, UNKNOWN -> false;
        });
    }

    private static boolean waitForGameEnd(long pid, Path lockFile, Path sessionFile, Path watchStateFile) {
        while (true) {
            Properties session = loadProperties(sessionFile);
            if ("CLEAN".equalsIgnoreCase(session.getProperty("state", ""))) {
                return false;
            }
            if (lockFile != null && processLockReleased(lockFile)) {
                return true;
            }
            var handle = ProcessHandle.of(pid);
            if (handle.isEmpty() || !handle.get().isAlive()) {
                return true;
            }
            writeWatcherState(watchStateFile, "WAITING", pid);
            sleep(500L);
        }
    }

    private static boolean processLockReleased(Path lockFile) {
        try {
            Files.createDirectories(lockFile.getParent());
            try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                try (FileLock lock = channel.tryLock()) {
                    return lock != null;
                } catch (OverlappingFileLockException ignored) {
                    return false;
                }
            }
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean hasNewCrashReport(Path gameDir, long startedAt) {
        Path crashDir = gameDir.resolve("crash-reports");
        if (!Files.isDirectory(crashDir)) {
            return false;
        }
        try (var stream = Files.list(crashDir)) {
            return stream.anyMatch(path -> {
                try {
                    return path.getFileName().toString().endsWith(".txt")
                            && Files.getLastModifiedTime(path).toMillis() >= Math.max(0L, startedAt - 2_000L);
                } catch (IOException ignored) {
                    return false;
                }
            });
        } catch (IOException ignored) {
            return false;
        }
    }

    static void writeLastAnalysis(Path gameDir, RecoveryAnalysis analysis, String analysisType) {
        Path file = gameDir.resolve("yogiessentials").resolve("recovery").resolve("last-analysis.properties");
        try {
            Files.createDirectories(file.getParent());
            Properties properties = new Properties();
            properties.setProperty("analysisType", analysisType == null ? "CRASH" : analysisType);
            properties.setProperty("analysisSchema", ANALYSIS_SCHEMA);
            properties.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
            properties.setProperty("title", safe(analysis.title()));
            properties.setProperty("summary", safe(analysis.summary()));
            properties.setProperty("confidence", analysis.confidence().name());
            properties.setProperty("testMode", "false");
            properties.setProperty("issueCount", Integer.toString(analysis.issues().size()));
            properties.setProperty("suspectCount", Integer.toString(analysis.suspects().size()));
            if (analysis.sourcePath() != null) {
                properties.setProperty("source", analysis.sourcePath().toString());
            }
            if (!analysis.suspects().isEmpty()) {
                RecoveryAnalysis.Suspect suspect = analysis.suspects().get(0);
                properties.setProperty("suspect", suspect.displayName());
                properties.setProperty("suspectId", safe(suspect.id()));
            }
            storeProperties(file, properties);
        } catch (IOException ignored) {
        }
    }

    private static void updateSessionState(Path file, String state) {
        if (file == null) {
            return;
        }
        Properties properties = loadProperties(file);
        properties.setProperty("state", state);
        properties.setProperty("resolvedAt", Long.toString(System.currentTimeMillis()));
        try {
            storeProperties(file, properties);
        } catch (IOException ignored) {
        }
    }

    private static void writeWatcherState(Path file, String state, long watchedPid) {
        if (file == null) {
            return;
        }
        try {
            Properties properties = new Properties();
            properties.setProperty("state", state);
            properties.setProperty("heartbeat", Long.toString(System.currentTimeMillis()));
            properties.setProperty("helperPid", Long.toString(ProcessHandle.current().pid()));
            properties.setProperty("watchedPid", Long.toString(watchedPid));
            storeProperties(file, properties);
        } catch (IOException ignored) {
        }
    }

    private static Properties loadProperties(Path file) {
        Properties properties = new Properties();
        if (file == null || !Files.isRegularFile(file)) {
            return properties;
        }
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        } catch (IOException ignored) {
        }
        return properties;
    }

    private static void storeProperties(Path file, Properties properties) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temp)) {
            properties.store(output, "Yogi Essentials Crash Assistant");
        }
        try {
            Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }
            String key = arg.substring(2);
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                values.put(key, args[++i]);
            } else {
                values.put(key, "true");
            }
        }
        return values;
    }

    private static long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
