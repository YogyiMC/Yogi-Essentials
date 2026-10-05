package dev.yogi.yogiessentials.client.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public final class CrashAssistantManager {
    private static final String RECOVERY_MAIN = "dev.yogi.yogiessentials.recovery.RecoveryAssistantMain";
    private static final String HELPER_JAR_PREFIX = "yogi-recovery-assistant-1.0.2-";
    private static final long WATCHER_HEARTBEAT_TIMEOUT_MS = 5_000L;
    private static final String RECOVERY_SCHEMA = "3";
    private static final String ANALYSIS_SCHEMA = "3";
    private static final List<String> HELPER_ENTRIES = List.of(
            "dev/yogi/yogiessentials/recovery/RecoveryAssistantMain.class",
            "dev/yogi/yogiessentials/recovery/RecoveryAssistantMain$1.class",
            "dev/yogi/yogiessentials/recovery/RecoveryAnalysis.class",
            "dev/yogi/yogiessentials/recovery/RecoveryAnalysis$Confidence.class",
            "dev/yogi/yogiessentials/recovery/RecoveryAnalysis$IssueType.class",
            "dev/yogi/yogiessentials/recovery/RecoveryAnalysis$FixType.class",
            "dev/yogi/yogiessentials/recovery/RecoveryAnalysis$Suspect.class",
            "dev/yogi/yogiessentials/recovery/RecoveryAnalysis$Issue.class",
            "dev/yogi/yogiessentials/recovery/RecoveryAnalysis$FixAction.class",
            "dev/yogi/yogiessentials/recovery/RecoveryAnalyzer.class",
            "dev/yogi/yogiessentials/recovery/RecoveryAnalyzer$1.class",
            "dev/yogi/yogiessentials/recovery/RecoveryModScanner.class",
            "dev/yogi/yogiessentials/recovery/RecoveryModScanner$ModFile.class",
            "dev/yogi/yogiessentials/recovery/RecoveryModScanner$CompatibilityProblem.class",
            "dev/yogi/yogiessentials/recovery/PersistentRecoveryMonitor.class",
            "dev/yogi/yogiessentials/recovery/PersistentRecoveryMonitor$1.class",
            "dev/yogi/yogiessentials/recovery/RecoveryFixManager.class",
            "dev/yogi/yogiessentials/recovery/RecoveryFixManager$1.class",
            "dev/yogi/yogiessentials/recovery/RecoveryFixManager$AppliedChange.class",
            "dev/yogi/yogiessentials/recovery/RecoveryFixManager$ApplyResult.class",
            "dev/yogi/yogiessentials/recovery/RecoveryFixManager$RestoreResult.class",
            "dev/yogi/yogiessentials/recovery/RecoveryFixManager$DisabledEntry.class",
            "dev/yogi/yogiessentials/recovery/RecoveryHistory.class",
            "dev/yogi/yogiessentials/recovery/RecoveryHistory$HistoryEntry.class",
            "dev/yogi/yogiessentials/recovery/RecoverySelfTest.class",
            "dev/yogi/yogiessentials/recovery/RecoverySelfTest$TestResult.class",
            "dev/yogi/yogiessentials/recovery/RecoveryWindow.class"
    );
    private static Path gameDir;
    private static Path recoveryDir;
    private static Path sessionFile;
    private static Path lockFile;
    private static Path watcherStateFile;
    private static Path runtimeMarkerFile;
    private static long sessionStartedAt;
    private static boolean watcherLaunchSucceeded;
    private static FileChannel lockChannel;
    private static FileLock processLock;
    private static long lastWatcherStatusReadAt;
    private static String lastEarlyRecoveryError = "";
    private static WatcherStatus cachedWatcherStatus = new WatcherStatus(false, -1L, -1L, "STARTING");

    private CrashAssistantManager() {
    }

    public static void disableAllRecovery() {
        try {
            gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
            recoveryDir = gameDir.resolve("yogiessentials").resolve("recovery");

            if (isEarlyStartupRecoverySupported()) {
                Properties previous = loadProperties(recoveryDir.resolve("early-recovery.properties"));
                removeStartupRegistration(previous.getProperty("registrationMethod", ""));
            }

            Files.createDirectories(recoveryDir);
            Files.writeString(
                    recoveryDir.resolve("persistent-stop.flag"),
                    "disabled",
                    StandardCharsets.UTF_8
            );

            Properties persistent = loadProperties(recoveryDir.resolve("persistent-state.properties"));
            long helperPid = parseLong(persistent.getProperty("pid"), -1L);
            if (helperPid > 0L) {
                ProcessHandle.of(helperPid).ifPresent(handle -> {
                    try {
                        handle.destroy();
                    } catch (Throwable ignored) {
                    }
                });
            }

            Properties properties = loadProperties(recoveryDir.resolve("early-recovery.properties"));
            properties.setProperty("enabled", "false");
            properties.setProperty("disabledAt", Long.toString(System.currentTimeMillis()));
            properties.setProperty("disabledByBuild", "1.0.2");
            properties.setProperty("lastError", "");
            storeProperties(recoveryDir.resolve("early-recovery.properties"), properties);
        } catch (Throwable ignored) {
        }
    }

    public static void initialize() {
        gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        recoveryDir = gameDir.resolve("yogiessentials").resolve("recovery");
        sessionStartedAt = System.currentTimeMillis();
        long pid = ProcessHandle.current().pid();
        String sessionId = sessionStartedAt + "-" + pid;
        sessionFile = recoveryDir.resolve("session-" + sessionId + ".properties");
        lockFile = recoveryDir.resolve("session-" + sessionId + ".lock");
        watcherStateFile = recoveryDir.resolve("watcher-" + sessionId + ".properties");
        runtimeMarkerFile = recoveryDir.resolve("runtime-" + pid + ".properties");

        try {
            Files.createDirectories(recoveryDir);
            writeRuntimeMarker("INITIALIZED");
            if (isEarlyStartupRecoveryEnabled()) {
                refreshEarlyStartupRecovery();
            }
            recoverPreviousInterruptedSessions();
            acquireProcessLock();
            writeSession("RUNNING");
            Files.deleteIfExists(watcherStateFile);
            watcherLaunchSucceeded = launchAssistant(List.of(
                    "--watch",
                    Long.toString(pid),
                    "--game-dir",
                    gameDir.toString(),
                    "--session-file",
                    sessionFile.toString(),
                    "--lock-file",
                    lockFile.toString(),
                    "--watch-state",
                    watcherStateFile.toString()
            ));
        } catch (IOException exception) {
            watcherLaunchSucceeded = false;
            releaseProcessLock();
            System.err.println("[Yogi Essentials] Crash Assistant could not start: " + exception.getMessage());
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> markCleanShutdown());
    }

    public static boolean isWatcherRunning() {
        WatcherStatus status = getWatcherStatus();
        return status.active();
    }

    public static WatcherStatus getWatcherStatus() {
        long now = System.currentTimeMillis();
        if (now - lastWatcherStatusReadAt < 750L) {
            return cachedWatcherStatus;
        }
        lastWatcherStatusReadAt = now;

        if (!watcherLaunchSucceeded) {
            cachedWatcherStatus = new WatcherStatus(false, -1L, -1L, "FAILED");
            return cachedWatcherStatus;
        }
        if (watcherStateFile == null || !Files.isRegularFile(watcherStateFile)) {
            cachedWatcherStatus = now - sessionStartedAt < 5_000L
                    ? new WatcherStatus(true, -1L, ProcessHandle.current().pid(), "STARTING")
                    : new WatcherStatus(false, -1L, ProcessHandle.current().pid(), "NO HANDSHAKE");
            return cachedWatcherStatus;
        }

        Properties properties = loadProperties(watcherStateFile);
        String state = properties.getProperty("state", "UNKNOWN");
        long heartbeat = parseLong(properties.getProperty("heartbeat"), 0L);
        long helperPid = parseLong(properties.getProperty("helperPid"), -1L);
        long watchedPid = parseLong(properties.getProperty("watchedPid"), ProcessHandle.current().pid());
        boolean stateActive = "WAITING".equalsIgnoreCase(state) || "READY".equalsIgnoreCase(state);
        boolean fresh = heartbeat > 0L && now - heartbeat <= WATCHER_HEARTBEAT_TIMEOUT_MS;
        cachedWatcherStatus = new WatcherStatus(stateActive && fresh, helperPid, watchedPid, state);
        return cachedWatcherStatus;
    }

    public static long getSessionStartedAt() {
        return sessionStartedAt;
    }

    public static Path getRecoveryDir() {
        return recoveryDir;
    }

    public static boolean launchTestAssistant() {
        if (gameDir == null) {
            return false;
        }
        return launchAssistant(List.of(
                "--test",
                "--game-dir",
                gameDir.toString()
        ));
    }

    public static boolean analyzeCurrentLogs() {
        if (gameDir == null) {
            return false;
        }
        return launchAssistant(List.of(
                "--analyze-now",
                "--game-dir",
                gameDir.toString(),
                "--session-start",
                Long.toString(sessionStartedAt)
        ));
    }

    public static boolean runDiagnosisTests() {
        if (gameDir == null) {
            return false;
        }
        return launchAssistant(List.of(
                "--self-tests",
                "--game-dir",
                gameDir.toString()
        ));
    }

    public static boolean openCrashHistory() {
        if (gameDir == null) {
            return false;
        }
        return launchAssistant(List.of(
                "--history",
                "--game-dir",
                gameDir.toString()
        ));
    }

    public static boolean openDisabledModsManager() {
        if (gameDir == null) {
            return false;
        }
        return launchAssistant(List.of(
                "--disabled-manager",
                "--game-dir",
                gameDir.toString()
        ));
    }

    public static boolean runStartupPreflight() {
        if (gameDir == null) {
            return false;
        }
        return launchAssistant(List.of(
                "--preflight",
                "--game-dir",
                gameDir.toString()
        ));
    }

    public static boolean isEarlyStartupRecoverySupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static boolean isEarlyStartupRecoveryEnabled() {
        if (recoveryDir == null) {
            return false;
        }
        Properties properties = loadProperties(recoveryDir.resolve("early-recovery.properties"));
        return Boolean.parseBoolean(properties.getProperty("enabled", "false"));
    }

    public static EarlyRecoveryStatus getEarlyRecoveryStatus() {
        boolean supported = isEarlyStartupRecoverySupported();
        boolean enabled = isEarlyStartupRecoveryEnabled();
        if (recoveryDir == null) {
            return new EarlyRecoveryStatus(supported, enabled, false, -1L, "NOT READY", "");
        }
        Path stateFile = recoveryDir.resolve("persistent-state.properties");
        Properties properties = loadProperties(stateFile);
        String state = properties.getProperty("state", enabled ? "STARTING" : "DISABLED");
        long pid = parseLong(properties.getProperty("pid"), -1L);
        long heartbeat = parseLong(properties.getProperty("heartbeat"), 0L);
        boolean alive = pid > 0L && ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
        boolean fresh = heartbeat > 0L && System.currentTimeMillis() - heartbeat <= 5_000L;
        boolean active = enabled && alive && fresh && (state.equalsIgnoreCase("ACTIVE") || state.equalsIgnoreCase("STARTUP_ALERT"));
        return new EarlyRecoveryStatus(supported, enabled, active, pid, state, properties.getProperty("message", ""));
    }

    public static String getLastEarlyRecoveryError() {
        return lastEarlyRecoveryError == null ? "" : lastEarlyRecoveryError;
    }

    public static boolean enableEarlyStartupRecovery() {
        lastEarlyRecoveryError = "";
        if (gameDir == null || recoveryDir == null) {
            lastEarlyRecoveryError = "Crash Assistant is not initialized yet.";
            return false;
        }
        if (!isEarlyStartupRecoverySupported()) {
            lastEarlyRecoveryError = "Early Startup Recovery is currently supported on Windows only.";
            return false;
        }
        try {
            Files.createDirectories(recoveryDir);
            Files.deleteIfExists(recoveryDir.resolve("persistent-stop.flag"));
            Path helperJar = ensureHelperJar();
            Path javaExecutable = resolveJavaExecutable();
            StartupRegistration registration = installStartupRegistration(helperJar, javaExecutable);
            if (!registration.installed()) {
                lastEarlyRecoveryError = registration.message();
                writeEarlyRecoveryFailure(registration.message());
                return false;
            }

            Properties properties = new Properties();
            properties.setProperty("enabled", "true");
            properties.setProperty("registeredAt", Long.toString(System.currentTimeMillis()));
            properties.setProperty("helperJar", helperJar.toString());
            properties.setProperty("java", javaExecutable.toString());
            properties.setProperty("gameDir", gameDir.toString());
            properties.setProperty("registrationMethod", registration.method());
            properties.setProperty("startupArtifact", registration.artifact() == null ? "" : registration.artifact().toString());
            properties.setProperty("lastError", "");
            storeProperties(recoveryDir.resolve("early-recovery.properties"), properties);

            try {
                launchPersistentHelper(helperJar);
            } catch (IOException exception) {
                removeStartupRegistration(registration.method());
                properties.setProperty("enabled", "false");
                properties.setProperty("lastError", safeError(exception.getMessage()));
                storeProperties(recoveryDir.resolve("early-recovery.properties"), properties);
                lastEarlyRecoveryError = "Startup registration succeeded, but the recovery helper could not start: " + safeError(exception.getMessage());
                return false;
            }
            return true;
        } catch (Exception exception) {
            lastEarlyRecoveryError = "Could not enable Early Startup Recovery: " + safeError(exception.getMessage());
            writeEarlyRecoveryFailure(lastEarlyRecoveryError);
            System.err.println("[Yogi Essentials] " + lastEarlyRecoveryError);
            return false;
        }
    }

    public static boolean disableEarlyStartupRecovery() {
        if (recoveryDir == null || !isEarlyStartupRecoverySupported()) {
            return false;
        }
        Properties properties = loadProperties(recoveryDir.resolve("early-recovery.properties"));
        String method = properties.getProperty("registrationMethod", "");
        boolean persistenceRemoved = removeStartupRegistration(method);
        try {
            Files.createDirectories(recoveryDir);
            Files.writeString(recoveryDir.resolve("persistent-stop.flag"), "stop", StandardCharsets.UTF_8);
            properties.setProperty("enabled", "false");
            properties.setProperty("disabledAt", Long.toString(System.currentTimeMillis()));
            properties.setProperty("lastError", persistenceRemoved ? "" : "Could not fully remove the Windows startup registration.");
            storeProperties(recoveryDir.resolve("early-recovery.properties"), properties);
        } catch (IOException exception) {
            lastEarlyRecoveryError = safeError(exception.getMessage());
            return false;
        }
        if (!persistenceRemoved) {
            lastEarlyRecoveryError = "Could not fully remove the Windows startup registration.";
        } else {
            lastEarlyRecoveryError = "";
        }
        return persistenceRemoved;
    }

    private static void refreshEarlyStartupRecovery() {
        try {
            Properties previous = loadProperties(recoveryDir.resolve("early-recovery.properties"));
            String previousHelper = previous.getProperty("helperJar", "");
            EarlyRecoveryStatus previousStatus = getEarlyRecoveryStatus();
            Path helperJar = ensureHelperJar();
            Path javaExecutable = resolveJavaExecutable();
            StartupRegistration registration = installStartupRegistration(helperJar, javaExecutable);
            if (!registration.installed()) {
                lastEarlyRecoveryError = registration.message();
                writeEarlyRecoveryFailure(registration.message());
                return;
            }

            Properties properties = loadProperties(recoveryDir.resolve("early-recovery.properties"));
            properties.setProperty("enabled", "true");
            properties.setProperty("helperJar", helperJar.toString());
            properties.setProperty("java", javaExecutable.toString());
            properties.setProperty("gameDir", gameDir.toString());
            properties.setProperty("registrationMethod", registration.method());
            properties.setProperty("startupArtifact", registration.artifact() == null ? "" : registration.artifact().toString());
            properties.setProperty("lastError", "");
            storeProperties(recoveryDir.resolve("early-recovery.properties"), properties);

            boolean helperChanged = !helperJar.toString().equals(previousHelper);
            if (helperChanged && previousStatus.active()) {
                Files.writeString(recoveryDir.resolve("persistent-stop.flag"), "upgrade", StandardCharsets.UTF_8);
                Thread.sleep(900L);
            }
            Files.deleteIfExists(recoveryDir.resolve("persistent-stop.flag"));
            if (helperChanged || !previousStatus.active()) {
                launchPersistentHelper(helperJar);
            }
            lastEarlyRecoveryError = "";
        } catch (Exception exception) {
            lastEarlyRecoveryError = "Could not refresh Early Startup Recovery: " + safeError(exception.getMessage());
            writeEarlyRecoveryFailure(lastEarlyRecoveryError);
            System.err.println("[Yogi Essentials] " + lastEarlyRecoveryError);
        }
    }

    private static StartupRegistration installStartupRegistration(Path helperJar, Path javaExecutable) {
        String command = persistentCommand(helperJar, javaExecutable);
        String registryError = "";
        try {
            Process process = new ProcessBuilder(
                    resolveWindowsRegExecutable().toString(),
                    "add",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v",
                    "Yogi Essentials Recovery",
                    "/t",
                    "REG_SZ",
                    "/d",
                    command,
                    "/f"
            ).redirectErrorStream(true).start();
            String output;
            try (InputStream input = process.getInputStream()) {
                output = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            int exit = process.waitFor();
            if (exit == 0) {
                deleteStartupScript();
                return new StartupRegistration(true, "REGISTRY", null, output);
            }
            registryError = "Registry startup registration failed (exit " + exit + ")" + (output.isBlank() ? "." : ": " + compactError(output));
        } catch (Exception exception) {
            registryError = "Registry startup registration failed: " + safeError(exception.getMessage());
        }

        try {
            Path startupFolder = resolveWindowsStartupFolder();
            Files.createDirectories(startupFolder);
            Path script = startupFolder.resolve("Yogi Essentials Recovery.vbs");
            String escapedCommand = command.replace("\"", "\"\"");
            String scriptText = "Set shell = CreateObject(\"WScript.Shell\")\r\n"
                    + "shell.Run \"" + escapedCommand + "\", 0, False\r\n"
                    + "Set shell = Nothing\r\n";
            Files.writeString(script, scriptText, StandardCharsets.UTF_8);
            if (!Files.isRegularFile(script)) {
                throw new IOException("Startup launcher file was not created.");
            }
            removeRegistryStartup();
            return new StartupRegistration(true, "STARTUP_VBS", script, "Windows Startup folder fallback installed.");
        } catch (Exception exception) {
            String startupError = "Startup folder fallback failed: " + safeError(exception.getMessage());
            return new StartupRegistration(false, "NONE", null, registryError + " " + startupError);
        }
    }

    private static boolean removeStartupRegistration(String configuredMethod) {
        boolean registryRemoved = removeRegistryStartup();
        boolean startupRemoved = deleteStartupScript();
        if ("REGISTRY".equalsIgnoreCase(configuredMethod)) {
            return registryRemoved;
        }
        if ("STARTUP_VBS".equalsIgnoreCase(configuredMethod)) {
            return startupRemoved;
        }
        return registryRemoved || startupRemoved;
    }

    private static boolean removeRegistryStartup() {
        try {
            Process process = new ProcessBuilder(
                    resolveWindowsRegExecutable().toString(),
                    "delete",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                    "/v",
                    "Yogi Essentials Recovery",
                    "/f"
            ).redirectErrorStream(true).start();
            try (InputStream input = process.getInputStream()) {
                input.readAllBytes();
            }
            int exit = process.waitFor();
            return exit == 0 || exit == 1;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean deleteStartupScript() {
        try {
            Files.deleteIfExists(resolveWindowsStartupFolder().resolve("Yogi Essentials Recovery.vbs"));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Path resolveWindowsStartupFolder() throws IOException {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            throw new IOException("APPDATA is unavailable.");
        }
        return Path.of(appData, "Microsoft", "Windows", "Start Menu", "Programs", "Startup");
    }

    private static Path resolveWindowsRegExecutable() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot != null && !systemRoot.isBlank()) {
            Path candidate = Path.of(systemRoot, "System32", "reg.exe");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return Path.of("reg.exe");
    }

    private static String persistentCommand(Path helperJar, Path javaExecutable) {
        return windowsQuoted(javaExecutable.toString())
                + " -jar " + windowsQuoted(helperJar.toString())
                + " --persistent --game-dir " + windowsQuoted(gameDir.toString());
    }

    private static void writeEarlyRecoveryFailure(String message) {
        if (recoveryDir == null) {
            return;
        }
        try {
            Properties properties = loadProperties(recoveryDir.resolve("early-recovery.properties"));
            properties.setProperty("enabled", "false");
            properties.setProperty("lastError", safeError(message));
            properties.setProperty("failedAt", Long.toString(System.currentTimeMillis()));
            storeProperties(recoveryDir.resolve("early-recovery.properties"), properties);
        } catch (IOException ignored) {
        }
    }

    private static String compactError(String value) {
        String compact = value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (compact.length() > 220) {
            return compact.substring(0, 220) + "...";
        }
        return compact;
    }

    private static String safeError(String value) {
        String error = value == null || value.isBlank() ? "unknown error" : value.trim();
        return compactError(error);
    }

    private static void launchPersistentHelper(Path helperJar) throws IOException {
        Files.deleteIfExists(recoveryDir.resolve("persistent-stop.flag"));
        List<String> command = new ArrayList<>();
        command.add(resolveJavaExecutable().toString());
        command.add("-jar");
        command.add(helperJar.toString());
        command.add("--persistent");
        command.add("--game-dir");
        command.add(gameDir.toString());
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(gameDir.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(recoveryDir.resolve("recovery-assistant.log").toFile()));
        builder.start();
    }

    private static String windowsQuoted(String value) {
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    public static LastAnalysis getLastAnalysis() {
        if (recoveryDir == null) {
            return LastAnalysis.none();
        }
        Path file = recoveryDir.resolve("last-analysis.properties");
        if (!Files.isRegularFile(file)) {
            return LastAnalysis.none();
        }

        Properties properties = loadProperties(file);
        String analysisType = properties.getProperty("analysisType", "");
        if (!("CRASH".equalsIgnoreCase(analysisType) || "STARTUP".equalsIgnoreCase(analysisType))
                || !ANALYSIS_SCHEMA.equals(properties.getProperty("analysisSchema", ""))) {
            return LastAnalysis.none();
        }
        return new LastAnalysis(
                properties.getProperty("title", "No analysis available"),
                properties.getProperty("summary", ""),
                properties.getProperty("confidence", "NONE"),
                properties.getProperty("suspect", ""),
                parseLong(properties.getProperty("timestamp"), 0L),
                false
        );
    }

    public static boolean openRecoveryFolder() {
        if (recoveryDir == null) {
            return false;
        }
        try {
            Files.createDirectories(recoveryDir);
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder builder;
            if (os.contains("win")) {
                builder = new ProcessBuilder("explorer.exe", recoveryDir.toString());
            } else if (os.contains("mac")) {
                builder = new ProcessBuilder("open", recoveryDir.toString());
            } else {
                builder = new ProcessBuilder("xdg-open", recoveryDir.toString());
            }
            builder.start();
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static void recoverPreviousInterruptedSessions() {
        if (!Files.isDirectory(recoveryDir)) {
            return;
        }
        try (var stream = Files.list(recoveryDir)) {
            List<Path> sessions = stream
                    .filter(path -> path.getFileName().toString().startsWith("session-"))
                    .filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .sorted(Comparator.comparingLong(CrashAssistantManager::modifiedMillis).reversed())
                    .limit(12)
                    .toList();

            for (Path previous : sessions) {
                Properties properties = loadProperties(previous);
                if (!RECOVERY_SCHEMA.equals(properties.getProperty("recoverySchema", ""))) {
                    if ("RUNNING".equalsIgnoreCase(properties.getProperty("state", ""))) {
                        properties.setProperty("state", "LEGACY_IGNORED");
                        storeProperties(previous, properties);
                    }
                    continue;
                }
                if (!"RUNNING".equalsIgnoreCase(properties.getProperty("state", ""))) {
                    continue;
                }
                long previousPid = parseLong(properties.getProperty("pid"), -1L);
                if (previousPid > 0L && ProcessHandle.of(previousPid).map(ProcessHandle::isAlive).orElse(false)) {
                    continue;
                }
                long startedAt = parseLong(properties.getProperty("startedAt"), 0L);
                if (startedAt <= 0L) {
                    continue;
                }
                properties.setProperty("state", "RECOVERY_QUEUED");
                storeProperties(previous, properties);
                boolean launched = launchAssistant(List.of(
                        "--recover-session",
                        "--game-dir",
                        gameDir.toString(),
                        "--session-file",
                        previous.toString()
                ));
                if (!launched) {
                    properties.setProperty("state", "RUNNING");
                    storeProperties(previous, properties);
                }
                break;
            }
        } catch (IOException ignored) {
        }
    }

    private static void acquireProcessLock() throws IOException {
        lockChannel = FileChannel.open(
                lockFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        );
        processLock = lockChannel.lock();
    }

    private static void markCleanShutdown() {
        try {
            writeRuntimeMarker("CLEAN");
            writeSession("CLEAN");
        } catch (IOException ignored) {
        } finally {
            releaseProcessLock();
        }
    }

    private static void releaseProcessLock() {
        if (processLock != null) {
            try {
                processLock.release();
            } catch (IOException ignored) {
            }
            processLock = null;
        }
        if (lockChannel != null) {
            try {
                lockChannel.close();
            } catch (IOException ignored) {
            }
            lockChannel = null;
        }
    }

    private static void writeRuntimeMarker(String state) throws IOException {
        if (runtimeMarkerFile == null) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("state", state);
        properties.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        properties.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
        properties.setProperty("version", "1.0.2");
        storeProperties(runtimeMarkerFile, properties);
    }

    private static void writeSession(String state) throws IOException {
        Files.createDirectories(recoveryDir);
        Properties properties = new Properties();
        properties.setProperty("state", state);
        properties.setProperty("startedAt", Long.toString(sessionStartedAt));
        properties.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        properties.setProperty("version", "1.0.2");
        properties.setProperty("recoverySchema", RECOVERY_SCHEMA);
        properties.setProperty("gameDir", gameDir.toString());
        properties.setProperty("lockFile", lockFile.toString());
        storeProperties(sessionFile, properties);
    }

    private static boolean launchAssistant(List<String> arguments) {
        try {
            Path helperJar = ensureHelperJar();

            List<String> command = new ArrayList<>();
            command.add(resolveJavaExecutable().toString());
            command.add("-jar");
            command.add(helperJar.toString());
            command.addAll(arguments);

            Path logFile = recoveryDir.resolve("recovery-assistant.log");
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(gameDir.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
            builder.start();
            return true;
        } catch (Exception exception) {
            System.err.println("[Yogi Essentials] Failed to launch Crash Assistant: " + exception.getMessage());
            return false;
        }
    }

    private static Path ensureHelperJar() throws IOException {
        Files.createDirectories(recoveryDir);
        Path temp = recoveryDir.resolve("yogi-recovery-assistant-building.tmp");
        Files.deleteIfExists(temp);

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, RECOVERY_MAIN);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is unavailable.", exception);
        }

        ClassLoader loader = CrashAssistantManager.class.getClassLoader();
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(temp), manifest)) {
            for (String entryName : HELPER_ENTRIES) {
                byte[] data;
                try (InputStream input = loader.getResourceAsStream(entryName)) {
                    if (input == null) {
                        throw new IOException("Missing recovery helper class: " + entryName);
                    }
                    data = input.readAllBytes();
                }
                digest.update(entryName.getBytes(StandardCharsets.UTF_8));
                digest.update(data);
                output.putNextEntry(new JarEntry(entryName));
                output.write(data);
                output.closeEntry();
            }

            String icon = "assets/yogiessentials/icon.png";
            try (InputStream input = loader.getResourceAsStream(icon)) {
                if (input != null) {
                    byte[] data = input.readAllBytes();
                    digest.update(icon.getBytes(StandardCharsets.UTF_8));
                    digest.update(data);
                    output.putNextEntry(new JarEntry(icon));
                    output.write(data);
                    output.closeEntry();
                }
            }
        }

        String fingerprint = HexFormat.of().formatHex(digest.digest(), 0, 6);
        Path helperJar = recoveryDir.resolve(HELPER_JAR_PREFIX + fingerprint + ".jar");
        if (Files.isRegularFile(helperJar)) {
            Files.deleteIfExists(temp);
            return helperJar;
        }

        try {
            Files.move(temp, helperJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temp, helperJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return helperJar;
    }

    private static Path resolveJavaExecutable() {
        Path bin = Path.of(System.getProperty("java.home"), "bin");
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (windows) {
            Path javaw = bin.resolve("javaw.exe");
            if (Files.isRegularFile(javaw)) {
                return javaw;
            }
            return bin.resolve("java.exe");
        }
        return bin.resolve("java");
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

    private static long modifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private record StartupRegistration(boolean installed, String method, Path artifact, String message) {
    }

    public record WatcherStatus(boolean active, long helperPid, long watchedPid, String state) {
    }

    public record EarlyRecoveryStatus(boolean supported, boolean enabled, boolean active, long pid, String state, String message) {
    }

    public record LastAnalysis(
            String title,
            String summary,
            String confidence,
            String suspect,
            long timestamp,
            boolean testMode
    ) {
        public static LastAnalysis none() {
            return new LastAnalysis(
                    "No crash analyzed yet",
                    "Crash Assistant has not analyzed a real crash on this installation yet.",
                    "NONE",
                    "",
                    0L,
                    false
            );
        }
    }
}
