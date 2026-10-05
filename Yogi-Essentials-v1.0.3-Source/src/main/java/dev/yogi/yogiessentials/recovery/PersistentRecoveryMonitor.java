package dev.yogi.yogiessentials.recovery;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PersistentRecoveryMonitor {
    private static final long POLL_MS = 700L;
    private static final long STARTUP_SETTLE_MS = 900L;
    private static final long HEARTBEAT_MS = 2_000L;
    private static final long PROCESS_START_WINDOW_MS = 90_000L;
    private static final Pattern ARG_FILE_PATTERN = Pattern.compile("(?:^|\\s)@(?:\"([^\"]+)\"|(\\S+))");

    private PersistentRecoveryMonitor() {
    }

    static void run(Path gameDir) {
        Path recoveryDir = gameDir.resolve("yogiessentials").resolve("recovery");
        Path lockPath = recoveryDir.resolve("persistent.lock");
        Path statePath = recoveryDir.resolve("persistent-state.properties");
        Path stopPath = recoveryDir.resolve("persistent-stop.flag");
        try {
            Files.createDirectories(recoveryDir);
            Files.deleteIfExists(stopPath);
        } catch (IOException ignored) {
        }

        appendLog(recoveryDir, "Persistent Early Recovery starting. pid=" + ProcessHandle.current().pid());
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            FileLock lock;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException ignored) {
                appendLog(recoveryDir, "Persistent helper already active in this JVM.");
                return;
            }
            if (lock == null) {
                appendLog(recoveryDir, "Persistent helper already active in another process.");
                return;
            }
            try (lock) {
                monitor(gameDir, recoveryDir, statePath, stopPath);
            }
        } catch (IOException exception) {
            writeState(statePath, "FAILED", -1L, safe(exception.getMessage()));
            appendLog(recoveryDir, "Persistent helper failed: " + safe(exception.getMessage()));
        }
    }

    private static void monitor(Path gameDir, Path recoveryDir, Path statePath, Path stopPath) {
        long selfPid = ProcessHandle.current().pid();
        Map<Long, Long> pending = new HashMap<>();
        Set<Long> alertedProcesses = new HashSet<>();
        Set<Long> initialized = new HashSet<>();
        writeState(statePath, "ACTIVE", selfPid, "Waiting for a Minecraft/Fabric launch attempt");
        appendLog(recoveryDir, "Persistent Early Recovery active for " + gameDir);
        long lastHeartbeatAt = System.currentTimeMillis();

        while (true) {
            if (Files.exists(stopPath)) {
                try {
                    Files.deleteIfExists(stopPath);
                } catch (IOException ignored) {
                }
                writeState(statePath, "STOPPED", selfPid, "Disabled by user");
                appendLog(recoveryDir, "Persistent Early Recovery stopped.");
                return;
            }

            long now = System.currentTimeMillis();
            Set<Long> helperPids = knownHelperPids(recoveryDir, selfPid);
            Map<Long, ProcessHandle> games = minecraftProcesses(gameDir, selfPid, helperPids, now);
            RecoveryAnalysis environment = null;
            boolean needsPreflight = false;

            for (Map.Entry<Long, ProcessHandle> entry : games.entrySet()) {
                long pid = entry.getKey();
                long startedAt = entry.getValue().info().startInstant().map(Instant::toEpochMilli).orElse(now);
                if (runtimeInitialized(recoveryDir, pid, startedAt)) {
                    initialized.add(pid);
                    pending.remove(pid);
                    continue;
                }
                if (!initialized.contains(pid)) {
                    Long previous = pending.putIfAbsent(pid, startedAt);
                    if (previous == null) {
                        String executable = entry.getValue().info().command().orElse("java");
                        appendLog(recoveryDir, "Minecraft/Fabric launch candidate detected. pid=" + pid + " executable=" + safe(executable));
                    }
                    needsPreflight = true;
                }
            }

            if (needsPreflight) {
                try {
                    environment = RecoveryAnalyzer.analyzeStartupEnvironment(gameDir);
                } catch (Throwable throwable) {
                    appendLog(recoveryDir, "Launch-time preflight scan failed: " + throwable.getClass().getSimpleName() + ": " + safe(throwable.getMessage()));
                }
            }

            for (Map.Entry<Long, ProcessHandle> entry : games.entrySet()) {
                long pid = entry.getKey();
                if (initialized.contains(pid)) {
                    continue;
                }
                Long startedAt = pending.get(pid);
                if (startedAt == null) {
                    continue;
                }
                if (!alertedProcesses.contains(pid) && now - startedAt >= STARTUP_SETTLE_MS && isBlocking(environment)) {
                    alertedProcesses.add(pid);
                    RecoveryAssistantMain.persistStartupAnalysis(gameDir, environment);
                    writeState(statePath, "STARTUP_ALERT", selfPid, "Detected a blocking Fabric compatibility problem during Minecraft launch PID " + pid);
                    appendLog(recoveryDir, "Opening recovery UI for launch-time startup blocker. pid=" + pid + " fingerprint=" + blockingFingerprint(environment) + " title=" + safe(environment.title()));
                    showRecovery(gameDir, environment, recoveryDir);
                }
            }

            List<Long> ended = new ArrayList<>();
            for (Map.Entry<Long, Long> entry : pending.entrySet()) {
                long pid = entry.getKey();
                if (games.containsKey(pid)) {
                    continue;
                }
                ended.add(pid);
                if (initialized.contains(pid) || alertedProcesses.contains(pid) || runtimeInitialized(recoveryDir, pid, entry.getValue())) {
                    continue;
                }
                sleep(900L);
                RecoveryAnalysis analysis = RecoveryAnalyzer.analyze(gameDir, entry.getValue());
                if (hasRealFailure(analysis)) {
                    alertedProcesses.add(pid);
                    RecoveryAssistantMain.persistStartupAnalysis(gameDir, analysis);
                    writeState(statePath, "STARTUP_ALERT", selfPid, "Minecraft/Fabric ended before Yogi Essentials initialized");
                    appendLog(recoveryDir, "Opening recovery UI after early startup failure. pid=" + pid + " title=" + safe(analysis.title()));
                    showRecovery(gameDir, analysis, recoveryDir);
                }
            }
            for (Long pid : ended) {
                pending.remove(pid);
                initialized.remove(pid);
                alertedProcesses.remove(pid);
            }

            if (now - lastHeartbeatAt >= HEARTBEAT_MS) {
                boolean activeAlertProcess = games.keySet().stream().anyMatch(alertedProcesses::contains);
                if (activeAlertProcess) {
                    touchHeartbeat(statePath, selfPid);
                } else if (needsPreflight) {
                    writeState(statePath, "ACTIVE", selfPid, "Minecraft/Fabric launch detected; checking startup health");
                } else {
                    writeState(statePath, "ACTIVE", selfPid, "Waiting for a Minecraft/Fabric launch attempt");
                }
                lastHeartbeatAt = now;
            }
            sleep(POLL_MS);
        }
    }

    private static void showRecovery(Path gameDir, RecoveryAnalysis analysis, Path recoveryDir) {
        try {
            RecoveryWindow.show(gameDir, analysis);
        } catch (Throwable throwable) {
            appendLog(recoveryDir, "Could not show recovery UI: " + throwable.getClass().getSimpleName() + ": " + safe(throwable.getMessage()));
        }
    }

    private static String blockingFingerprint(RecoveryAnalysis analysis) {
        StringBuilder builder = new StringBuilder();
        builder.append(safe(analysis.title())).append('|');
        analysis.issues().stream()
                .filter(issue -> issue.type() == RecoveryAnalysis.IssueType.INCOMPATIBILITY || issue.type() == RecoveryAnalysis.IssueType.DEPENDENCY)
                .filter(issue -> issue.confidence() == RecoveryAnalysis.Confidence.HIGH)
                .forEach(issue -> builder.append(issue.type()).append(':').append(safe(issue.title())).append(':').append(safe(issue.description())).append('|'));
        return Integer.toHexString(builder.toString().hashCode()) + '-' + builder.length();
    }

    private static boolean isBlocking(RecoveryAnalysis analysis) {
        return analysis != null && analysis.issues().stream().anyMatch(issue -> switch (issue.type()) {
            case INCOMPATIBILITY, DEPENDENCY -> issue.confidence() == RecoveryAnalysis.Confidence.HIGH;
            default -> false;
        });
    }

    private static boolean hasRealFailure(RecoveryAnalysis analysis) {
        if (analysis == null) {
            return false;
        }
        return analysis.issues().stream().anyMatch(issue -> switch (issue.type()) {
            case MIXIN, DEPENDENCY, INCOMPATIBILITY, CONFIG, DIRECT_EXCEPTION, LINKAGE, NATIVE -> true;
            case DUPLICATE_MOD -> analysis.title().toLowerCase(Locale.ROOT).contains("duplicate") || analysis.title().toLowerCase(Locale.ROOT).contains("dependency");
            case UNKNOWN -> false;
        });
    }

    private static Map<Long, ProcessHandle> minecraftProcesses(Path gameDir, long selfPid, Set<Long> helperPids, long now) {
        Map<Long, ProcessHandle> result = new HashMap<>();
        String gamePath = normalizePath(gameDir);
        ProcessHandle.allProcesses().forEach(handle -> {
            if (handle.pid() == selfPid || helperPids.contains(handle.pid()) || !handle.isAlive()) {
                return;
            }
            ProcessHandle.Info info = handle.info();
            String rawCommandLine = commandLine(handle);
            String expandedCommandLine = expandArgumentFiles(rawCommandLine, info.arguments().orElse(new String[0]));
            String lower = expandedCommandLine.toLowerCase(Locale.ROOT).replace('\\', '/');
            String executable = info.command().orElse("").toLowerCase(Locale.ROOT).replace('\\', '/');
            if (isRecoveryHelper(lower)) {
                return;
            }
            if (!isJavaExecutable(executable, lower)) {
                return;
            }
            boolean strictMinecraft = containsMinecraftMain(lower);
            boolean gameDirMatch = !gamePath.isBlank() && lower.contains(gamePath);
            boolean minecraftClasspath = lower.contains("/.minecraft/libraries/")
                    || lower.contains("/minecraft/libraries/")
                    || lower.contains("fabric-loader")
                    || lower.contains("minecraft-launcher")
                    || lower.contains("net.minecraft");
            boolean minecraftRuntime = executable.contains("minecraft launcher")
                    || executable.contains("/.minecraft/runtime/")
                    || executable.contains("/minecraft/runtime/")
                    || executable.contains("java-runtime-");
            boolean launcherParent = hasMinecraftLauncherAncestor(handle);
            long startedAt = info.startInstant().map(Instant::toEpochMilli).orElse(now);
            boolean recentlyStarted = startedAt <= now + 2_000L && now - startedAt <= PROCESS_START_WINDOW_MS;
            boolean likelyMinecraft = strictMinecraft
                    || gameDirMatch && minecraftClasspath
                    || recentlyStarted && launcherParent
                    || recentlyStarted && minecraftRuntime && (minecraftClasspath || gameDirMatch || launcherParent);
            if (!likelyMinecraft) {
                return;
            }
            boolean hasGameDirArgument = lower.contains("--gamedir") || lower.contains("--game-dir");
            if (hasGameDirArgument && !gameDirMatch) {
                return;
            }
            result.put(handle.pid(), handle);
        });
        return result;
    }

    private static boolean isRecoveryHelper(String lowerCommandLine) {
        return lowerCommandLine.contains("yogi-recovery-assistant")
                || lowerCommandLine.contains("dev.yogi.yogiessentials.recovery.recoveryassistantmain")
                || lowerCommandLine.contains("--persistent") && lowerCommandLine.contains("yogiessentials");
    }

    private static boolean isJavaExecutable(String executable, String commandLine) {
        String name = executable;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < name.length()) {
            name = name.substring(slash + 1);
        }
        return name.equals("java.exe")
                || name.equals("javaw.exe")
                || name.equals("java")
                || name.equals("javaw")
                || commandLine.contains("java.exe")
                || commandLine.contains("javaw.exe");
    }

    private static boolean containsMinecraftMain(String lower) {
        return lower.contains("net.fabricmc.loader.impl.launch.knot.knotclient")
                || lower.contains("knotclient")
                || lower.contains("net.minecraft.client.main.main")
                || lower.contains("minecraft.client.main")
                || lower.contains("fabric-loader") && lower.contains("minecraft");
    }

    private static boolean hasMinecraftLauncherAncestor(ProcessHandle handle) {
        ProcessHandle current = handle;
        for (int depth = 0; depth < 6; depth++) {
            var parent = current.parent();
            if (parent.isEmpty()) {
                return false;
            }
            current = parent.get();
            ProcessHandle.Info info = current.info();
            String command = info.command().orElse("").toLowerCase(Locale.ROOT).replace('\\', '/');
            String line = commandLine(current).toLowerCase(Locale.ROOT).replace('\\', '/');
            if (command.contains("minecraftlauncher")
                    || command.contains("minecraft launcher")
                    || line.contains("minecraftlauncher")
                    || line.contains("minecraft launcher")) {
                return true;
            }
        }
        return false;
    }

    private static String commandLine(ProcessHandle handle) {
        ProcessHandle.Info info = handle.info();
        StringBuilder builder = new StringBuilder(info.command().orElse(""));
        if (info.commandLine().isPresent()) {
            String commandLine = info.commandLine().get();
            if (!commandLine.isBlank()) {
                return commandLine;
            }
        }
        info.arguments().ifPresent(arguments -> {
            for (String argument : arguments) {
                builder.append(' ').append(argument);
            }
        });
        return builder.toString();
    }

    private static String expandArgumentFiles(String commandLine, String[] arguments) {
        StringBuilder expanded = new StringBuilder(commandLine == null ? "" : commandLine);
        Set<Path> candidates = new HashSet<>();
        Matcher matcher = ARG_FILE_PATTERN.matcher(commandLine == null ? "" : commandLine);
        while (matcher.find()) {
            String value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            addArgumentFile(candidates, value);
        }
        for (String argument : arguments) {
            if (argument != null && argument.startsWith("@") && argument.length() > 1) {
                String value = argument.substring(1);
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                addArgumentFile(candidates, value);
            }
        }
        for (Path candidate : candidates) {
            try {
                if (Files.isRegularFile(candidate) && Files.size(candidate) <= 2_000_000L) {
                    expanded.append(' ').append(Files.readString(candidate, StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {
            }
        }
        return expanded.toString();
    }

    private static void addArgumentFile(Set<Path> candidates, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            candidates.add(Path.of(value).toAbsolutePath().normalize());
        } catch (Exception ignored) {
        }
    }

    private static String normalizePath(Path path) {
        if (path == null) {
            return "";
        }
        return path.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT).replace('\\', '/');
    }

    private static Set<Long> knownHelperPids(Path recoveryDir, long selfPid) {
        Set<Long> result = new HashSet<>();
        result.add(selfPid);
        if (!Files.isDirectory(recoveryDir)) {
            return result;
        }
        try (var stream = Files.list(recoveryDir)) {
            stream.filter(path -> path.getFileName().toString().startsWith("watcher-"))
                    .filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .limit(32)
                    .forEach(path -> {
                        Properties properties = load(path);
                        long helperPid = parseLong(properties.getProperty("helperPid"), -1L);
                        if (helperPid > 0L) {
                            result.add(helperPid);
                        }
                    });
        } catch (IOException ignored) {
        }
        return result;
    }

    private static boolean runtimeInitialized(Path recoveryDir, long pid, long processStartedAt) {
        Path marker = recoveryDir.resolve("runtime-" + pid + ".properties");
        if (!Files.isRegularFile(marker)) {
            return false;
        }
        Properties properties = load(marker);
        String state = properties.getProperty("state", "");
        long markerTimestamp = parseLong(properties.getProperty("timestamp"), 0L);
        boolean currentProcess = markerTimestamp >= Math.max(0L, processStartedAt - 2_500L);
        return currentProcess && (state.equalsIgnoreCase("INITIALIZED") || state.equalsIgnoreCase("RUNNING") || state.equalsIgnoreCase("CLEAN"));
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void writeState(Path file, String state, long pid, String message) {
        if (file == null) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("state", state == null ? "UNKNOWN" : state);
        properties.setProperty("pid", Long.toString(pid));
        properties.setProperty("heartbeat", Long.toString(System.currentTimeMillis()));
        properties.setProperty("message", message == null ? "" : message);
        store(file, properties);
    }

    private static void touchHeartbeat(Path file, long pid) {
        Properties properties = load(file);
        properties.setProperty("pid", Long.toString(pid));
        properties.setProperty("heartbeat", Long.toString(System.currentTimeMillis()));
        store(file, properties);
    }

    private static Properties load(Path file) {
        Properties properties = new Properties();
        if (file == null || !Files.isRegularFile(file)) {
            return properties;
        }
        try (var input = Files.newInputStream(file)) {
            properties.load(input);
        } catch (IOException ignored) {
        }
        return properties;
    }

    private static void store(Path file, Properties properties) {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream output = Files.newOutputStream(temp)) {
                properties.store(output, "Yogi Essentials Early Recovery");
            }
            try {
                Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }

    private static void appendLog(Path recoveryDir, String message) {
        try {
            Files.createDirectories(recoveryDir);
            String line = Instant.now() + " [early-recovery] " + safe(message) + System.lineSeparator();
            Files.writeString(
                    recoveryDir.resolve("recovery-assistant.log"),
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.replace('\r', ' ').replace('\n', ' ');
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
