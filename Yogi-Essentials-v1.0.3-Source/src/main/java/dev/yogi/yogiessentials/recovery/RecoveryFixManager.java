package dev.yogi.yogiessentials.recovery;

import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.FixAction;
import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.FixType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

final class RecoveryFixManager {
    private RecoveryFixManager() {
    }

    static ApplyResult apply(Path gameDir, RecoveryAnalysis analysis, List<FixAction> selected) {
        String transactionId = Long.toString(System.currentTimeMillis());
        List<AppliedChange> changes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (FixAction fix : selected) {
            try {
                AppliedChange change = switch (fix.type()) {
                    case DISABLE_MOD, DISABLE_DUPLICATE -> disableMod(gameDir, analysis.testMode(), fix, transactionId);
                    case RESET_CONFIG -> resetConfig(gameDir, analysis.testMode(), fix, transactionId);
                };
                changes.add(change);
            } catch (IOException exception) {
                String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                if ((fix.type() == FixType.DISABLE_MOD || fix.type() == FixType.DISABLE_DUPLICATE) && looksLikeFileLock(exception, message)) {
                    message += " Close the Fabric Loader/Minecraft error window, then try the fix again.";
                }
                errors.add(fix.label() + ": " + message);
            }
        }

        if (!analysis.testMode() && !changes.isEmpty()) {
            writeTransaction(gameDir, transactionId, analysis, changes);
        }
        return new ApplyResult(transactionId, List.copyOf(changes), List.copyOf(errors));
    }

    static RestoreResult restoreChanges(Path gameDir, ApplyResult result) {
        int restored = 0;
        List<String> errors = new ArrayList<>();
        List<AppliedChange> changes = result == null ? List.of() : result.changes();

        for (int i = changes.size() - 1; i >= 0; i--) {
            AppliedChange change = changes.get(i);
            try {
                if (!Files.exists(change.changedPath())) {
                    continue;
                }
                Files.createDirectories(change.originalPath().getParent());
                Path destination = Files.exists(change.originalPath())
                        ? uniquePath(change.originalPath())
                        : change.originalPath();
                Files.move(change.changedPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                restored++;
                if (change.manifestPath() != null) {
                    Files.deleteIfExists(change.manifestPath());
                }
            } catch (IOException exception) {
                errors.add(change.label() + ": " + exception.getMessage());
            }
        }

        return new RestoreResult(restored, List.copyOf(errors));
    }

    static List<DisabledEntry> listDisabled(Path gameDir) {
        List<DisabledEntry> result = new ArrayList<>();
        Path manifests = recoveryDir(gameDir).resolve("disabled-manifests");
        if (Files.isDirectory(manifests)) {
            try (var stream = Files.list(manifests)) {
                for (Path manifest : stream.filter(path -> path.getFileName().toString().endsWith(".properties")).toList()) {
                    Properties properties = loadProperties(manifest);
                    Path disabled = path(properties.getProperty("disabled"));
                    Path original = path(properties.getProperty("original"));
                    if (disabled != null && Files.isRegularFile(disabled)) {
                        result.add(new DisabledEntry(
                                properties.getProperty("name", disabled.getFileName().toString()),
                                properties.getProperty("modId", ""),
                                original,
                                disabled,
                                parseLong(properties.getProperty("timestamp"), modifiedMillis(manifest)),
                                properties.getProperty("reason", "Disabled by Crash Assistant"),
                                manifest
                        ));
                    } else {
                        try {
                            Files.deleteIfExists(manifest);
                        } catch (IOException ignored) {
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }

        Path disabledDir = gameDir.resolve("yogiessentials").resolve("disabled-mods");
        if (Files.isDirectory(disabledDir)) {
            try (var stream = Files.list(disabledDir)) {
                for (Path disabled : stream.filter(Files::isRegularFile).toList()) {
                    boolean known = result.stream().anyMatch(entry -> entry.disabledPath().equals(disabled.toAbsolutePath().normalize()));
                    if (!known) {
                        result.add(new DisabledEntry(
                                disabled.getFileName().toString(),
                                "",
                                gameDir.resolve("mods").resolve(baseDisabledName(disabled.getFileName().toString())).toAbsolutePath().normalize(),
                                disabled.toAbsolutePath().normalize(),
                                modifiedMillis(disabled),
                                "Disabled by an earlier Crash Assistant build",
                                null
                        ));
                    }
                }
            } catch (IOException ignored) {
            }
        }

        result.sort(Comparator.comparingLong(DisabledEntry::timestamp).reversed());
        return result;
    }

    static Path restoreDisabled(Path gameDir, DisabledEntry entry) throws IOException {
        Path disabledRoot = gameDir.resolve("yogiessentials").resolve("disabled-mods").toAbsolutePath().normalize();
        Path source = entry.disabledPath().toAbsolutePath().normalize();
        if (!source.startsWith(disabledRoot) || !Files.isRegularFile(source)) {
            throw new IOException("Disabled mod is no longer available.");
        }
        Path modsRoot = gameDir.resolve("mods").toAbsolutePath().normalize();
        Path desired = entry.originalPath() == null
                ? modsRoot.resolve(baseDisabledName(source.getFileName().toString()))
                : entry.originalPath().toAbsolutePath().normalize();
        if (!desired.startsWith(modsRoot)) {
            desired = modsRoot.resolve(baseDisabledName(source.getFileName().toString()));
        }
        Files.createDirectories(modsRoot);
        Path target = Files.exists(desired) ? uniquePath(desired) : desired;
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        if (entry.manifestPath() != null) {
            Files.deleteIfExists(entry.manifestPath());
        }
        return target;
    }

    static void deleteDisabled(Path gameDir, DisabledEntry entry) throws IOException {
        Path disabledRoot = gameDir.resolve("yogiessentials").resolve("disabled-mods").toAbsolutePath().normalize();
        Path source = entry.disabledPath().toAbsolutePath().normalize();
        if (!source.startsWith(disabledRoot)) {
            throw new IOException("Refusing to delete a file outside the disabled-mods folder.");
        }
        Files.deleteIfExists(source);
        if (entry.manifestPath() != null) {
            Files.deleteIfExists(entry.manifestPath());
        }
    }

    static void deleteActiveMod(Path gameDir, Path path, boolean testMode) throws IOException {
        validateModPath(gameDir, path, testMode);
        Files.deleteIfExists(path);
    }

    private static AppliedChange disableMod(Path gameDir, boolean testMode, FixAction fix, String transactionId) throws IOException {
        Path source = fix.targetPath();
        validateModPath(gameDir, source, testMode);
        Path disabledDir = testMode
                ? gameDir.resolve("yogiessentials").resolve("recovery").resolve("test-sandbox").resolve("disabled-mods")
                : gameDir.resolve("yogiessentials").resolve("disabled-mods");
        Files.createDirectories(disabledDir);

        String fileName = source.getFileName().toString();
        Path target = uniquePath(disabledDir.resolve(fileName));
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        Path manifest = null;
        if (!testMode) {
            manifest = writeDisabledManifest(gameDir, transactionId, fix, source, target);
        }
        return new AppliedChange(fix.label(), fix.type(), source, target, manifest);
    }

    private static AppliedChange resetConfig(Path gameDir, boolean testMode, FixAction fix, String transactionId) throws IOException {
        Path source = fix.targetPath();
        Path configRoot = testMode
                ? gameDir.resolve("yogiessentials").resolve("recovery").resolve("test-sandbox").resolve("config").toAbsolutePath().normalize()
                : gameDir.resolve("config").toAbsolutePath().normalize();
        Path normalized = source == null ? null : source.toAbsolutePath().normalize();
        if (normalized == null || !normalized.startsWith(configRoot) || !Files.isRegularFile(normalized)) {
            throw new IOException("Refusing to reset a file outside the allowed config directory.");
        }

        Path backupRoot = testMode
                ? gameDir.resolve("yogiessentials").resolve("recovery").resolve("test-sandbox").resolve("config-backups").resolve(transactionId)
                : recoveryDir(gameDir).resolve("config-backups").resolve(transactionId);
        Path relative = configRoot.relativize(normalized);
        Path backup = backupRoot.resolve(relative).toAbsolutePath().normalize();
        Files.createDirectories(backup.getParent());
        Files.move(normalized, backup, StandardCopyOption.REPLACE_EXISTING);
        return new AppliedChange(fix.label(), FixType.RESET_CONFIG, normalized, backup, null);
    }

    private static Path writeDisabledManifest(Path gameDir, String transactionId, FixAction fix, Path original, Path disabled) throws IOException {
        Path dir = recoveryDir(gameDir).resolve("disabled-manifests");
        Files.createDirectories(dir);
        String safeId = fix.modId() == null || fix.modId().isBlank() ? "mod" : fix.modId().replaceAll("[^A-Za-z0-9_.-]", "_");
        Path file = uniquePath(dir.resolve(System.currentTimeMillis() + "-" + safeId + ".properties"));
        Properties properties = new Properties();
        properties.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
        properties.setProperty("transaction", transactionId);
        properties.setProperty("name", fix.label().replaceFirst("(?i)^Disable (duplicate )?", ""));
        properties.setProperty("modId", fix.modId() == null ? "" : fix.modId());
        properties.setProperty("original", original.toAbsolutePath().normalize().toString());
        properties.setProperty("disabled", disabled.toAbsolutePath().normalize().toString());
        properties.setProperty("reason", fix.description());
        storeProperties(file, properties);
        return file;
    }

    private static void writeTransaction(Path gameDir, String transactionId, RecoveryAnalysis analysis, List<AppliedChange> changes) {
        Path file = recoveryDir(gameDir).resolve("actions").resolve(transactionId + ".properties");
        try {
            Files.createDirectories(file.getParent());
            Properties properties = new Properties();
            properties.setProperty("timestamp", transactionId);
            properties.setProperty("title", analysis.title());
            properties.setProperty("count", Integer.toString(changes.size()));
            for (int i = 0; i < changes.size(); i++) {
                AppliedChange change = changes.get(i);
                properties.setProperty("change." + i + ".label", change.label());
                properties.setProperty("change." + i + ".type", change.type().name());
                properties.setProperty("change." + i + ".original", change.originalPath().toString());
                properties.setProperty("change." + i + ".changed", change.changedPath().toString());
            }
            storeProperties(file, properties);
        } catch (IOException ignored) {
        }
    }

    private static void validateModPath(Path gameDir, Path path, boolean testMode) throws IOException {
        if (path == null) {
            throw new IOException("No mod file was supplied.");
        }
        Path normalized = path.toAbsolutePath().normalize();
        Path allowed = testMode
                ? gameDir.resolve("yogiessentials").resolve("recovery").resolve("test-sandbox").resolve("mods").toAbsolutePath().normalize()
                : gameDir.resolve("mods").toAbsolutePath().normalize();
        if (!normalized.startsWith(allowed) || !normalized.getFileName().toString().toLowerCase().endsWith(".jar")) {
            throw new IOException("Refusing to modify a file outside the allowed mods directory.");
        }
        if (!Files.isRegularFile(normalized)) {
            throw new IOException("The selected mod file no longer exists.");
        }
    }

    private static boolean looksLikeFileLock(IOException exception, String message) {
        if (exception instanceof java.nio.file.AccessDeniedException) {
            return true;
        }
        String lower = message == null ? "" : message.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("used by another process")
                || lower.contains("being used by another process")
                || lower.contains("access is denied")
                || lower.contains("cannot access the file");
    }

    private static Path recoveryDir(Path gameDir) {
        return gameDir.resolve("yogiessentials").resolve("recovery");
    }

    private static Path uniquePath(Path desired) {
        if (!Files.exists(desired)) {
            return desired;
        }
        String fileName = desired.getFileName().toString();
        Path parent = desired.getParent();
        int suffix = 1;
        Path candidate;
        do {
            candidate = parent.resolve(fileName + "." + suffix++);
        } while (Files.exists(candidate));
        return candidate;
    }

    private static String baseDisabledName(String name) {
        return name.replaceFirst("\\.disabled-\\d+$", "").replaceFirst("\\.\\d+$", "");
    }

    private static Path path(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Path.of(value).toAbsolutePath().normalize();
        } catch (Exception ignored) {
            return null;
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
        Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temp)) {
            properties.store(output, "Yogi Essentials Crash Assistant");
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
            return Instant.now().toEpochMilli();
        }
    }

    record AppliedChange(
            String label,
            FixType type,
            Path originalPath,
            Path changedPath,
            Path manifestPath
    ) {
    }

    record ApplyResult(
            String transactionId,
            List<AppliedChange> changes,
            List<String> errors
    ) {
        boolean changedAnything() {
            return !changes.isEmpty();
        }
    }

    record RestoreResult(int restored, List<String> errors) {
    }

    record DisabledEntry(
            String name,
            String modId,
            Path originalPath,
            Path disabledPath,
            long timestamp,
            String reason,
            Path manifestPath
    ) {
        String displayName() {
            return modId == null || modId.isBlank() ? name : name + " (" + modId + ")";
        }
    }
}
