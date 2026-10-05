package dev.yogi.yogiessentials.recovery;

import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.Confidence;
import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.FixAction;
import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.FixType;
import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.Issue;
import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.IssueType;
import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.Suspect;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RecoveryAnalyzer {
    private static final int MAX_LOG_BYTES = 2_000_000;
    private static final Pattern PROVIDED_BY = Pattern.compile("provided by ['\\\"]([^'\\\"]+)['\\\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONFIG_PATH = Pattern.compile("(?i)(?:^|[\\s'\\\"=:])((?:\\.?[\\w.-]+[\\\\/])?config[\\\\/][^\\s'\\\"<>|]+)");
    private static final Pattern MOD_IN_PARENS = Pattern.compile("(?i)mod\\s+['\\\"][^'\\\"]+['\\\"]\\s*\\(([-a-z0-9_.]+)\\)");
    private static final Pattern REQUIRES_ID = Pattern.compile("(?i)\\b([-a-z0-9_.]+)\\b.{0,100}\\brequires\\b");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private RecoveryAnalyzer() {
    }

    public static RecoveryAnalysis analyze(Path gameDir, long sessionStartedAt) {
        return analyzeInternal(gameDir, sessionStartedAt, true);
    }

    public static RecoveryAnalysis analyzeCurrentLogs(Path gameDir, long sessionStartedAt) {
        return analyzeInternal(gameDir, sessionStartedAt, false);
    }

    public static RecoveryAnalysis analyzeStartupEnvironment(Path gameDir) {
        List<RecoveryModScanner.ModFile> mods = RecoveryModScanner.scan(gameDir.resolve("mods"));
        List<RecoveryModScanner.CompatibilityProblem> problems = RecoveryModScanner.compatibilityProblems(mods);
        List<Suspect> suspects = new ArrayList<>();
        List<Issue> issues = new ArrayList<>();
        List<FixAction> fixes = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        Set<String> suspectIds = new LinkedHashSet<>();
        Set<Path> hardConflictPaths = new HashSet<>();
        String hardConflictPrimaryName = "";
        String hardConflictSecondaryName = "";

        for (RecoveryModScanner.CompatibilityProblem problem : problems) {
            RecoveryModScanner.ModFile primary = problem.primary();
            RecoveryModScanner.ModFile secondary = problem.secondary();
            switch (problem.kind()) {
                case "INCOMPATIBILITY" -> {
                    if (hardConflictPrimaryName.isBlank() && primary != null && secondary != null) {
                        hardConflictPrimaryName = primary.name();
                        hardConflictSecondaryName = secondary.name();
                    }
                    if (primary != null && primary.jarPath() != null) {
                        hardConflictPaths.add(primary.jarPath());
                    }
                    if (secondary != null && secondary.jarPath() != null) {
                        hardConflictPaths.add(secondary.jarPath());
                    }
                    issues.add(new Issue(
                            IssueType.INCOMPATIBILITY,
                            "Explicit mod incompatibility",
                            problem.description(),
                            Confidence.HIGH,
                            primary == null ? "" : primary.id(),
                            primary == null ? null : primary.jarPath()
                    ));
                    addStartupSuspect(suspects, suspectIds, primary, "declares or participates in an explicit Fabric incompatibility");
                    addStartupSuspect(suspects, suspectIds, secondary, "is the installed mod named by the incompatibility rule");
                    String conflictGroup = "incompatibility:" + sortedPair(primary == null ? "" : primary.id(), secondary == null ? problem.dependencyId() : secondary.id());
                    if (primary != null) {
                        fixes.add(new FixAction(FixType.DISABLE_MOD, "Disable " + primary.name(), "Temporarily move this jar out of the mods folder. Keep the other conflicting mod installed.", Confidence.HIGH, primary.id(), primary.jarPath(), false, conflictGroup));
                    }
                    if (secondary != null && (primary == null || !secondary.jarPath().equals(primary.jarPath()))) {
                        fixes.add(new FixAction(FixType.DISABLE_MOD, "Disable " + secondary.name(), "Temporarily move this jar out of the mods folder. Keep the other conflicting mod installed.", Confidence.HIGH, secondary.id(), secondary.jarPath(), false, conflictGroup));
                    }
                }
                case "SOFT_CONFLICT" -> {
                    issues.add(new Issue(
                            IssueType.INCOMPATIBILITY,
                            "Soft compatibility warning",
                            problem.description(),
                            Confidence.MEDIUM,
                            primary == null ? "" : primary.id(),
                            primary == null ? null : primary.jarPath()
                    ));
                    addStartupSuspect(suspects, suspectIds, primary, "declares or participates in a soft Fabric compatibility conflict", Confidence.MEDIUM);
                    addStartupSuspect(suspects, suspectIds, secondary, "is the installed mod named by the soft conflict rule", Confidence.MEDIUM);
                    String conflictGroup = "soft-conflict:" + sortedPair(primary == null ? "" : primary.id(), secondary == null ? problem.dependencyId() : secondary.id());
                    if (primary != null) {
                        fixes.add(new FixAction(FixType.DISABLE_MOD, "Disable " + primary.name(), "Optionally move this jar out of the mods folder if the warning corresponds to a real problem. Fabric does not treat this soft conflict alone as a startup blocker.", Confidence.MEDIUM, primary.id(), primary.jarPath(), false, conflictGroup));
                    }
                    if (secondary != null && (primary == null || !secondary.jarPath().equals(primary.jarPath()))) {
                        fixes.add(new FixAction(FixType.DISABLE_MOD, "Disable " + secondary.name(), "Optionally move this jar out of the mods folder if the warning corresponds to a real problem. Fabric does not treat this soft conflict alone as a startup blocker.", Confidence.MEDIUM, secondary.id(), secondary.jarPath(), false, conflictGroup));
                    }
                }
                case "MISSING_DEPENDENCY" -> {
                    String required = problem.dependencyId();
                    issues.add(new Issue(
                            IssueType.DEPENDENCY,
                            "Missing required dependency",
                            problem.description() + predicateSuffix(problem.predicates()),
                            Confidence.HIGH,
                            primary == null ? "" : primary.id(),
                            primary == null ? null : primary.jarPath()
                    ));
                    addStartupSuspect(suspects, suspectIds, primary, "requires the missing dependency " + required);
                    if (primary != null) {
                        fixes.add(new FixAction(FixType.DISABLE_MOD, "Disable " + primary.name(), "Temporarily disable the mod that requires the missing dependency.", Confidence.HIGH, primary.id(), primary.jarPath(), false));
                    }
                    suggestions.add("Install the required dependency " + required + " at a compatible version, or disable the mod that requires it.");
                }
                case "VERSION_DEPENDENCY" -> {
                    issues.add(new Issue(
                            IssueType.DEPENDENCY,
                            "Dependency version mismatch",
                            problem.description() + predicateSuffix(problem.predicates()),
                            Confidence.HIGH,
                            primary == null ? "" : primary.id(),
                            primary == null ? null : primary.jarPath()
                    ));
                    addStartupSuspect(suspects, suspectIds, primary, "requires a different dependency version");
                    addStartupSuspect(suspects, suspectIds, secondary, "provides the currently installed dependency version");
                    if (primary != null) {
                        fixes.add(new FixAction(FixType.DISABLE_MOD, "Disable " + primary.name(), "Temporarily disable the mod whose dependency requirement cannot currently be satisfied.", Confidence.HIGH, primary.id(), primary.jarPath(), false));
                    }
                }
                case "DUPLICATE_COPY" -> {
                    List<RecoveryModScanner.ModFile> group = RecoveryModScanner.duplicates(mods).getOrDefault(problem.dependencyId().toLowerCase(Locale.ROOT), List.of());
                    RecoveryModScanner.ModFile keep = group.isEmpty() ? primary : group.get(0);
                    issues.add(new Issue(
                            IssueType.DUPLICATE_MOD,
                            "Duplicate mod file",
                            "Multiple copies of " + (keep == null ? problem.dependencyId() : keep.name()) + " declare the same mod id, name, and version.",
                            Confidence.HIGH,
                            keep == null ? problem.dependencyId() : keep.id(),
                            keep == null ? null : keep.jarPath()
                    ));
                    if (keep != null) {
                        addStartupSuspect(suspects, suspectIds, keep, "has duplicate top-level jar copies in the mods folder");
                    }
                    for (int i = 1; i < group.size(); i++) {
                        RecoveryModScanner.ModFile extra = group.get(i);
                        fixes.add(new FixAction(FixType.DISABLE_DUPLICATE, "Disable duplicate " + extra.jarPath().getFileName(), "Keep " + keep.jarPath().getFileName() + " active and move this duplicate out of the mods folder. This is optional unless Fabric itself reports a duplicate-mod startup failure.", Confidence.HIGH, extra.id(), extra.jarPath(), false));
                    }
                }
                case "MOD_ID_COLLISION" -> {
                    issues.add(new Issue(
                            IssueType.DUPLICATE_MOD,
                            "Fabric mod-id collision",
                            "Different top-level jar files declare the same Fabric mod id. This is an environment warning, not proof that startup is blocked.",
                            Confidence.HIGH,
                            problem.dependencyId(),
                            primary == null ? null : primary.jarPath()
                    ));
                    List<RecoveryModScanner.ModFile> group = RecoveryModScanner.duplicates(mods).getOrDefault(problem.dependencyId().toLowerCase(Locale.ROOT), List.of());
                    String collisionGroup = "mod-id:" + problem.dependencyId().toLowerCase(Locale.ROOT);
                    for (RecoveryModScanner.ModFile member : group) {
                        addStartupSuspect(suspects, suspectIds, member, "declares the same Fabric mod id as another different jar");
                        fixes.add(new FixAction(FixType.DISABLE_MOD, "Disable " + member.name(), "Disable one side of this mod-id collision. This action is reversible.", Confidence.HIGH, member.id(), member.jarPath(), false, collisionGroup));
                    }
                }
                default -> {
                }
            }
        }

        if (!hardConflictPaths.isEmpty()) {
            suspects.sort(Comparator
                    .comparingInt((Suspect suspect) -> suspect.jarPath() != null && hardConflictPaths.contains(suspect.jarPath()) ? 0 : 1)
                    .thenComparing(Comparator.comparingInt(Suspect::score).reversed())
                    .thenComparing(suspect -> suspect.displayName().toLowerCase(Locale.ROOT)));
        }

        fixes = deduplicateFixes(fixes);
        if (issues.isEmpty()) {
            String title = "Startup preflight passed";
            String summary = "No explicit Fabric mod incompatibility, missing dependency, duplicate copy, or top-level mod-id collision was detected.";
            suggestions.add("This preflight is conservative. Fabric Loader remains authoritative for constraints Crash Assistant cannot evaluate safely.");
            String details = buildDetails(title, summary, Confidence.NONE, null, List.of(), List.of(), List.of(), suggestions, "");
            return new RecoveryAnalysis(title, summary, details, Confidence.NONE, null, List.of(), List.of(), List.of(), List.copyOf(suggestions), false);
        }

        boolean hardIncompatibility = issues.stream().anyMatch(issue -> issue.type() == IssueType.INCOMPATIBILITY && issue.confidence() == Confidence.HIGH);
        boolean softIncompatibility = issues.stream().anyMatch(issue -> issue.type() == IssueType.INCOMPATIBILITY && issue.confidence() != Confidence.HIGH);
        boolean dependency = issues.stream().anyMatch(issue -> issue.type() == IssueType.DEPENDENCY && issue.confidence() == Confidence.HIGH);
        boolean duplicate = issues.stream().anyMatch(issue -> issue.type() == IssueType.DUPLICATE_MOD);
        String title = hardIncompatibility
                ? "Startup blocked — mod incompatibility"
                : dependency
                ? "Startup blocked — dependency problem"
                : softIncompatibility || duplicate
                ? "Startup preflight warning"
                : "Startup compatibility problem detected";
        String summary = hardIncompatibility && !hardConflictPrimaryName.isBlank() && !hardConflictSecondaryName.isBlank()
                ? hardConflictPrimaryName + " and " + hardConflictSecondaryName + " have an explicit hard incompatibility."
                : softIncompatibility && !hardIncompatibility && !dependency && !duplicate
                ? "Fabric metadata reports a soft compatibility conflict. This warning alone does not block startup."
                : duplicate && !hardIncompatibility && !dependency
                ? "Duplicate copies or mod-id collisions were found. They are reported as environment warnings and are not treated as startup blockers unless Fabric reports a real resolution failure."
                : "Crash Assistant found " + issues.size() + " startup compatibility issue" + (issues.size() == 1 ? "" : "s") + ".";
        suggestions.add("Review the proposed changes before applying them. Different conflicting mods are treated as alternatives, not duplicate copies.");
        Confidence confidence = overallConfidence(suspects, issues);
        String details = buildDetails(title, summary, confidence, null, suspects, issues, fixes, suggestions, "");
        return new RecoveryAnalysis(title, summary, details, confidence, null, List.copyOf(suspects), List.copyOf(issues), List.copyOf(fixes), List.copyOf(suggestions), false);
    }

    private static void addStartupSuspect(List<Suspect> suspects, Set<String> ids, RecoveryModScanner.ModFile mod, String reason) {
        addStartupSuspect(suspects, ids, mod, reason, Confidence.HIGH);
    }

    private static void addStartupSuspect(List<Suspect> suspects, Set<String> ids, RecoveryModScanner.ModFile mod, String reason, Confidence confidence) {
        if (mod == null) {
            return;
        }
        String key = mod.id().toLowerCase(Locale.ROOT) + "|" + mod.jarPath();
        if (!ids.add(key)) {
            return;
        }
        int score = confidence == Confidence.HIGH ? 500 : confidence == Confidence.MEDIUM ? 250 : 100;
        suspects.add(new Suspect(mod.id(), mod.name(), mod.version(), mod.jarPath(), confidence, score, reason));
    }

    private static String sortedPair(String left, String right) {
        String a = left == null ? "" : left.toLowerCase(Locale.ROOT);
        String b = right == null ? "" : right.toLowerCase(Locale.ROOT);
        return a.compareTo(b) <= 0 ? a + ":" + b : b + ":" + a;
    }

    private static String predicateSuffix(List<String> predicates) {
        if (predicates == null || predicates.isEmpty() || predicates.stream().allMatch(value -> value.equals("*"))) {
            return "";
        }
        return " Required version: " + String.join(" OR ", predicates) + ".";
    }

    private static RecoveryAnalysis analyzeInternal(Path gameDir, long sessionStartedAt, boolean unexpectedShutdown) {
        Path crashReport = newestCrashReport(gameDir.resolve("crash-reports"), sessionStartedAt);
        Path latestLog = gameDir.resolve("logs").resolve("latest.log");
        String crashText = readText(crashReport);
        String logText = readTail(latestLog, MAX_LOG_BYTES);
        String focusedLog = focusLog(logText);
        String combined = crashText.isBlank() ? focusedLog : crashText + "\n\n" + focusedLog;
        boolean hasCrashReport = crashReport != null && !crashText.isBlank();

        boolean mixin = containsAny(combined,
                "MixinApplyError",
                "MixinTransformerError",
                "InvalidInjectionException",
                "InjectionError",
                "mixin apply failed",
                "Critical injection failure");
        boolean dependency = containsAny(combined,
                "ModResolutionException",
                "Incompatible mod set",
                "Incompatible mods found",
                "requires version",
                "requires any version",
                "depends on",
                "Could not resolve mod set",
                "ResolutionException");
        boolean rawLinkage = containsAny(combined,
                "NoClassDefFoundError",
                "ClassNotFoundException",
                "NoSuchMethodError",
                "NoSuchFieldError",
                "AbstractMethodError",
                "IncompatibleClassChangeError",
                "UnsupportedClassVersionError");
        boolean rawConfigCrash = containsAny(combined,
                "ConfigException",
                "failed to parse config",
                "failed to load config",
                "error loading config",
                "failed to load configuration",
                "malformed json",
                "toml parse error");
        boolean nativeCrash = containsAny(combined,
                "EXCEPTION_ACCESS_VIOLATION",
                "A fatal error has been detected by the Java Runtime Environment",
                "SIGSEGV",
                "Problematic frame:");
        boolean directException = containsAny(combined,
                "Reported exception thrown",
                "---- Minecraft Crash Report ----",
                "Unhandled exception",
                "Uncaught exception in thread \"Render thread\"",
                "Uncaught exception in thread \"main\"",
                "Exception in thread \"Render thread\"",
                "Exception in thread \"main\"",
                "Minecraft has crashed",
                "Game crashed");
        boolean fatalContext = hasCrashReport || mixin || dependency || nativeCrash || directException;
        boolean linkage = rawLinkage && fatalContext;
        boolean configCrash = rawConfigCrash && fatalContext;
        boolean crashSignature = hasCrashReport || mixin || dependency || linkage || configCrash || nativeCrash || directException;
        String evidenceText = !crashText.isBlank() ? crashText : focusedLog;

        List<RecoveryModScanner.ModFile> mods = RecoveryModScanner.scan(gameDir.resolve("mods"));
        Map<String, List<RecoveryModScanner.ModFile>> duplicates = RecoveryModScanner.duplicates(mods);
        List<Suspect> suspects = crashSignature ? rankSuspects(evidenceText, crashText, mods, dependency) : List.of();
        List<Issue> issues = new ArrayList<>();
        List<FixAction> fixes = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        addDuplicateIssues(duplicates, issues, fixes, crashSignature && dependency);

        if (mixin) {
            Suspect primary = firstSuspect(suspects);
            issues.add(new Issue(
                    IssueType.MIXIN,
                    "Mixin failure",
                    primary == null
                            ? "Minecraft failed while applying a mixin, but ownership could not be tied to one installed mod confidently."
                            : "The failing mixin is most strongly associated with " + primary.displayName() + ".",
                    primary == null ? Confidence.LOW : primary.confidence(),
                    primary == null ? "" : primary.id(),
                    primary == null ? null : primary.jarPath()
            ));
            suggestions.add("Disable the mod that owns the failing mixin first, then launch Minecraft again.");
        }

        if (dependency) {
            Suspect primary = firstSuspect(suspects);
            issues.add(new Issue(
                    IssueType.DEPENDENCY,
                    "Dependency or version resolution failure",
                    primary == null
                            ? "Fabric reported a missing, incompatible, or unresolved dependency."
                            : primary.displayName() + " is the strongest installed mod associated with the dependency failure.",
                    primary == null ? Confidence.MEDIUM : primary.confidence(),
                    primary == null ? "" : primary.id(),
                    primary == null ? null : primary.jarPath()
            ));
            suggestions.add("Check the exact required Minecraft, Fabric Loader, Fabric API, and dependency versions in the details.");
        }

        List<Path> configPaths = configCrash ? findConfigPaths(gameDir, evidenceText) : List.of();
        if (configCrash) {
            Suspect primary = firstSuspect(suspects);
            Path configPath = configPaths.isEmpty() ? null : configPaths.get(0);
            Confidence configConfidence = configPath != null ? Confidence.HIGH : primary == null ? Confidence.LOW : primary.confidence();
            issues.add(new Issue(
                    IssueType.CONFIG,
                    "Configuration load failure",
                    configPath == null
                            ? "The crash looks config-related, but Yogi Essentials could not safely identify one concrete config file to reset."
                            : "The crash references " + gameDir.relativize(configPath) + ". Crash Assistant can back it up and let the owning mod regenerate it.",
                    configConfidence,
                    primary == null ? "" : primary.id(),
                    configPath
            ));
            if (configPath != null) {
                fixes.add(new FixAction(
                        FixType.RESET_CONFIG,
                        "Back up and reset " + configPath.getFileName(),
                        "Back up the config file, remove the active copy, and let the mod regenerate defaults on the next launch.",
                        configConfidence,
                        primary == null ? "" : primary.id(),
                        configPath,
                        configConfidence == Confidence.HIGH
                ));
            }
            suggestions.add("If a config reset is offered, Crash Assistant backs up the original before removing the active copy.");
        }

        if (linkage) {
            Suspect primary = firstSuspect(suspects);
            issues.add(new Issue(
                    IssueType.LINKAGE,
                    "Class or API incompatibility",
                    primary == null
                            ? "Minecraft hit a missing class, method, or field. This commonly means two mod versions are incompatible."
                            : primary.displayName() + " is the strongest owner of the failing class/API reference.",
                    primary == null ? Confidence.LOW : primary.confidence(),
                    primary == null ? "" : primary.id(),
                    primary == null ? null : primary.jarPath()
            ));
            suggestions.add("Check for an outdated mod or a mod built for a different Minecraft/API version.");
        }

        if (nativeCrash) {
            Suspect primary = firstSuspect(suspects);
            issues.add(new Issue(
                    IssueType.NATIVE,
                    "Native Java or graphics crash",
                    primary == null
                            ? "The JVM reported a native crash and no installed mod can be blamed confidently."
                            : primary.displayName() + " is the strongest mod-level suspect in the native crash context.",
                    primary == null ? Confidence.LOW : primary.confidence(),
                    primary == null ? "" : primary.id(),
                    primary == null ? null : primary.jarPath()
            ));
            suggestions.add("If no high-confidence mod is found, also check graphics drivers, overlays, and native injectors.");
        }

        if (crashSignature && !mixin && !dependency && !configCrash && !linkage && !nativeCrash) {
            Suspect primary = firstSuspect(suspects);
            issues.add(new Issue(
                    primary == null ? IssueType.UNKNOWN : IssueType.DIRECT_EXCEPTION,
                    primary == null ? "Crash cause not identified" : "Direct mod exception",
                    primary == null
                            ? "A crash is present, but no installed mod owns enough high-value evidence to be blamed reliably."
                            : primary.displayName() + " owns the strongest exception/stack evidence found in the crash.",
                    primary == null ? Confidence.NONE : primary.confidence(),
                    primary == null ? "" : primary.id(),
                    primary == null ? null : primary.jarPath()
            ));
        }

        addSuspectFixes(suspects, fixes, configCrash, dependency);
        fixes = deduplicateFixes(fixes);

        String title;
        String summary;
        if (mixin) {
            title = "Mixin / mod conflict detected";
            summary = suspectSummary("Minecraft crashed while applying a mixin.", suspects);
        } else if (dependency) {
            title = "Mod dependency / version problem detected";
            summary = suspectSummary("Fabric reported a dependency or version-resolution problem.", suspects);
        } else if (configCrash) {
            title = "Broken mod configuration detected";
            summary = configPaths.isEmpty()
                    ? suspectSummary("Minecraft appears to have crashed while loading a configuration file.", suspects)
                    : "Minecraft appears to have crashed while loading " + gameDir.relativize(configPaths.get(0)) + ".";
        } else if (linkage) {
            title = "Mod API / class incompatibility detected";
            summary = suspectSummary("Minecraft hit an incompatible or missing class/API member.", suspects);
        } else if (nativeCrash) {
            title = "Native Java crash detected";
            summary = suspectSummary("The Java process ended with a native crash.", suspects);
        } else if (crashSignature) {
            title = "Minecraft crash detected";
            summary = suspects.isEmpty()
                    ? "A crash was detected, but Yogi Essentials could not identify a mod reliably."
                    : "The strongest suspect is " + suspects.get(0).displayName() + ".";
        } else if (unexpectedShutdown) {
            title = "Unexpected Minecraft shutdown detected";
            summary = duplicates.isEmpty()
                    ? "Minecraft ended without a clean shutdown, but this session does not contain a clear crash signature. No mod will be blamed from ordinary log mentions alone."
                    : "Minecraft ended without a clean shutdown, but no crash signature was found. Separate duplicate-mod warnings were detected, but they are not being treated as the cause of this shutdown.";
            suggestions.add("If you intentionally force-closed Minecraft, no crash repair is needed.");
            suggestions.add("If Minecraft closed on its own, copy the details and check whether Windows or the launcher terminated the process.");
        } else {
            title = "No crash detected in current logs";
            summary = duplicates.isEmpty()
                    ? "The current session does not contain a new crash report or a recognized crash signature."
                    : "Minecraft is running without a detected crash. Crash Assistant found separate duplicate-mod warnings, but they are not evidence that Minecraft crashed.";
            suggestions.add("No crash repair is needed based on the current logs.");
        }

        if (crashSignature && suspects.isEmpty()) {
            suggestions.add("Crash Assistant will not disable a random mod when the evidence is too weak.");
        }

        Confidence confidence = crashSignature ? overallConfidence(suspects, issues) : Confidence.NONE;
        Path source = hasCrashReport ? crashReport : (Files.exists(latestLog) ? latestLog : null);
        String details = buildDetails(title, summary, confidence, source, suspects, issues, fixes, suggestions, combined);

        return new RecoveryAnalysis(
                title,
                summary,
                details,
                confidence,
                source,
                suspects,
                List.copyOf(issues),
                List.copyOf(fixes),
                List.copyOf(suggestions),
                false
        );
    }

    public static RecoveryAnalysis testAnalysis(Path gameDir) {
        try {
            Path testRoot = gameDir.resolve("yogiessentials").resolve("recovery").resolve("test-sandbox");
            Path testMods = testRoot.resolve("mods");
            Files.createDirectories(testMods);
            Path dummy = testMods.resolve("crash-assistant-test-mod.jar");
            if (!Files.exists(dummy)) {
                Files.writeString(dummy, "Yogi Essentials Crash Assistant harmless test file", StandardCharsets.UTF_8);
            }

            Suspect suspect = new Suspect(
                    "crash-assistant-test-mod",
                    "Crash Assistant Test Mod",
                    "1.0-test",
                    dummy,
                    Confidence.HIGH,
                    999,
                    "Synthetic high-confidence crash suspect used only to test recovery actions."
            );
            FixAction fix = new FixAction(
                    FixType.DISABLE_MOD,
                    "Disable Crash Assistant Test Mod",
                    "Move the harmless sandbox file out of the sandbox mods folder.",
                    Confidence.HIGH,
                    suspect.id(),
                    dummy,
                    true
            );
            Issue issue = new Issue(
                    IssueType.DIRECT_EXCEPTION,
                    "Synthetic direct mod crash",
                    "This issue exists only to exercise the real Fix All and restore flow.",
                    Confidence.HIGH,
                    suspect.id(),
                    dummy
            );
            List<String> suggestions = List.of(
                    "Use Fix All Detected to test the reversible disable action.",
                    "Use Restore Changes to verify the action can be undone.",
                    "No real Minecraft mod will be changed in test mode."
            );
            String details = "TEST MODE\n\nThis is a synthetic crash analysis. No real mod or config file will be changed.\n\nSuspect: Crash Assistant Test Mod\nConfidence: HIGH\nSandbox file: " + dummy;
            return new RecoveryAnalysis(
                    "Crash Assistant test",
                    "The desktop recovery UI, Fix All plan, reversible disable flow, and restore flow are ready to test.",
                    details,
                    Confidence.HIGH,
                    null,
                    List.of(suspect),
                    List.of(issue),
                    List.of(fix),
                    suggestions,
                    true
            );
        } catch (IOException exception) {
            return new RecoveryAnalysis(
                    "Crash Assistant test failed",
                    "The harmless test sandbox could not be created: " + exception.getMessage(),
                    exception.toString(),
                    Confidence.NONE,
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of("Check that Minecraft can write to its game directory."),
                    true
            );
        }
    }

    private static List<Suspect> rankSuspects(String text, String crashText, List<RecoveryModScanner.ModFile> mods, boolean dependencyFailure) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String lower = text.toLowerCase(Locale.ROOT);
        String crashLower = crashText == null ? "" : crashText.toLowerCase(Locale.ROOT);
        String suspectedSection = suspectedModsSection(text).toLowerCase(Locale.ROOT);
        List<String> providedByIds = providedByIds(text);
        Set<String> dependencyIds = dependencyIds(text);
        List<Suspect> suspects = new ArrayList<>();

        for (RecoveryModScanner.ModFile mod : mods) {
            String id = safe(mod.id()).toLowerCase(Locale.ROOT);
            String name = safe(mod.name()).toLowerCase(Locale.ROOT);
            int score = 0;
            List<String> reasons = new ArrayList<>();

            if (!suspectedSection.isBlank() && (!id.isBlank() && suspectedSection.contains("(" + id + ")") || !name.isBlank() && suspectedSection.contains(name))) {
                score += 240;
                reasons.add("listed in the crash report's Suspected Mods section");
            }

            if (providedByIds.stream().anyMatch(value -> value.equalsIgnoreCase(mod.id()))) {
                score += 220;
                reasons.add("named as the mod providing the failing Fabric entrypoint");
            }

            for (String mixinConfig : mod.mixinConfigs()) {
                String mixinLower = mixinConfig.toLowerCase(Locale.ROOT);
                if (mixinLower.length() >= 5 && lower.contains(mixinLower)) {
                    score += 210;
                    reasons.add("owns the mixin configuration " + mixinConfig);
                    break;
                }
            }

            for (String entrypoint : mod.entrypoints()) {
                String entryLower = entrypoint.toLowerCase(Locale.ROOT);
                if (lower.contains(entryLower)) {
                    score += 170;
                    reasons.add("its Fabric entrypoint class appears in the failure");
                    break;
                }
            }

            if (dependencyFailure && dependencyIds.stream().anyMatch(value -> value.equalsIgnoreCase(mod.id()))) {
                score += 150;
                reasons.add("its mod id is directly named in the dependency-resolution failure");
            }

            String fileName = mod.jarPath().getFileName().toString().toLowerCase(Locale.ROOT);
            if (lower.contains(fileName)) {
                score += 150;
                reasons.add("its exact jar filename appears in the crash context");
            }

            int bestHits = 0;
            String bestPrefix = null;
            for (String prefix : mod.packagePrefixes()) {
                String prefixLower = prefix.toLowerCase(Locale.ROOT);
                int hits = countOccurrences(lower, prefixLower);
                if (hits > bestHits) {
                    bestHits = hits;
                    bestPrefix = prefix;
                }
            }
            if (bestHits > 0) {
                int packageScore = Math.min(190, 105 + bestHits * 20);
                score += packageScore;
                reasons.add("exception/stack context references package " + bestPrefix + " " + bestHits + " time" + (bestHits == 1 ? "" : "s"));
            }

            if (!crashLower.isBlank() && !id.isBlank() && (crashLower.contains("'" + id + "'") || crashLower.contains("\"" + id + "\"") || crashLower.contains("(" + id + ")"))) {
                score += 55;
                reasons.add("mod id is named directly in the crash report");
            }

            if (score <= 0) {
                continue;
            }

            Confidence confidence = score >= 210
                    ? Confidence.HIGH
                    : score >= 120
                    ? Confidence.MEDIUM
                    : Confidence.LOW;

            suspects.add(new Suspect(
                    mod.id(),
                    mod.name(),
                    mod.version(),
                    mod.jarPath(),
                    confidence,
                    score,
                    String.join("; ", reasons)
            ));
        }

        suspects.sort(Comparator.comparingInt(Suspect::score).reversed());
        if (suspects.size() > 1) {
            Suspect first = suspects.get(0);
            Suspect second = suspects.get(1);
            if (first.score() < 120 && first.score() - second.score() < 35) {
                suspects = suspects.stream()
                        .map(suspect -> suspect.confidence() == Confidence.MEDIUM
                                ? new Suspect(suspect.id(), suspect.name(), suspect.version(), suspect.jarPath(), Confidence.LOW, suspect.score(), suspect.reason())
                                : suspect)
                        .toList();
            }
        }
        return suspects.stream().limit(6).toList();
    }

    private static void addDuplicateIssues(
            Map<String, List<RecoveryModScanner.ModFile>> duplicates,
            List<Issue> issues,
            List<FixAction> fixes,
            boolean blockingFailure
    ) {
        for (Map.Entry<String, List<RecoveryModScanner.ModFile>> entry : duplicates.entrySet()) {
            List<RecoveryModScanner.ModFile> group = entry.getValue();
            if (group.size() < 2) {
                continue;
            }
            RecoveryModScanner.ModFile keep = group.get(0);
            boolean sameCopies = RecoveryModScanner.likelySameModCopies(group);
            String description = sameCopies
                    ? "Multiple jar files provide the same mod id, name, and version. " + keep.jarPath().getFileName() + " is the preferred copy."
                    : "Different jar files declare the same Fabric mod id " + entry.getKey() + ". This is a mod-id collision, not proof that the files are duplicate copies.";
            issues.add(new Issue(
                    IssueType.DUPLICATE_MOD,
                    sameCopies ? "Duplicate mod file: " + keep.name() : "Mod-id collision: " + entry.getKey(),
                    description,
                    Confidence.HIGH,
                    keep.id(),
                    keep.jarPath()
            ));
            for (int i = 1; i < group.size(); i++) {
                RecoveryModScanner.ModFile extra = group.get(i);
                fixes.add(new FixAction(
                        sameCopies ? FixType.DISABLE_DUPLICATE : FixType.DISABLE_MOD,
                        (sameCopies ? "Disable duplicate " : "Disable conflicting ") + extra.jarPath().getFileName(),
                        sameCopies
                                ? "Keep " + keep.jarPath().getFileName() + " active and move this duplicate out of the mods folder."
                                : "Temporarily disable one side of the mod-id collision. This is reversible.",
                        Confidence.HIGH,
                        extra.id(),
                        extra.jarPath(),
                        blockingFailure && sameCopies
                ));
            }
        }
    }

    private static void addSuspectFixes(List<Suspect> suspects, List<FixAction> fixes, boolean configCrash, boolean dependency) {
        if (suspects.isEmpty()) {
            return;
        }
        for (int i = 0; i < Math.min(3, suspects.size()); i++) {
            Suspect suspect = suspects.get(i);
            if (suspect.jarPath() == null || suspect.confidence() == Confidence.LOW || suspect.confidence() == Confidence.NONE) {
                continue;
            }
            boolean selected = suspect.confidence() == Confidence.HIGH && !configCrash;
            String description = dependency
                    ? "Temporarily disable this mod if you prefer to launch without resolving its missing/incompatible dependency first."
                    : "Temporarily move this jar out of the active mods folder. This is reversible.";
            fixes.add(new FixAction(
                    FixType.DISABLE_MOD,
                    "Disable " + suspect.displayName(),
                    description,
                    suspect.confidence(),
                    suspect.id(),
                    suspect.jarPath(),
                    selected
            ));
        }
    }

    private static List<FixAction> deduplicateFixes(List<FixAction> fixes) {
        LinkedHashMap<String, FixAction> result = new LinkedHashMap<>();
        for (FixAction fix : fixes) {
            String key = fix.type() + "|" + (fix.targetPath() == null ? fix.label() : fix.targetPath().toAbsolutePath().normalize());
            FixAction previous = result.get(key);
            if (previous == null || confidenceRank(fix.confidence()) > confidenceRank(previous.confidence())) {
                result.put(key, fix);
            }
        }
        return new ArrayList<>(result.values());
    }

    private static List<Path> findConfigPaths(Path gameDir, String text) {
        List<Path> result = new ArrayList<>();
        Set<Path> seen = new HashSet<>();
        Path configDir = gameDir.resolve("config").toAbsolutePath().normalize();
        Matcher matcher = CONFIG_PATH.matcher(text == null ? "" : text);
        while (matcher.find() && result.size() < 6) {
            String raw = matcher.group(1).replace('\\', '/');
            int configIndex = raw.toLowerCase(Locale.ROOT).indexOf("config/");
            if (configIndex < 0) {
                continue;
            }
            String relative = raw.substring(configIndex + "config/".length());
            while (!relative.isEmpty() && ".,;:)]}".indexOf(relative.charAt(relative.length() - 1)) >= 0) {
                relative = relative.substring(0, relative.length() - 1);
            }
            if (relative.isBlank()) {
                continue;
            }
            Path candidate = configDir.resolve(relative).normalize();
            if (!candidate.startsWith(configDir) || !Files.isRegularFile(candidate)) {
                continue;
            }
            String lower = candidate.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".json") || lower.endsWith(".json5") || lower.endsWith(".toml") || lower.endsWith(".properties") || lower.endsWith(".yml") || lower.endsWith(".yaml") || lower.endsWith(".cfg") || lower.endsWith(".conf"))) {
                continue;
            }
            if (seen.add(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private static Confidence overallConfidence(List<Suspect> suspects, List<Issue> issues) {
        Confidence best = suspects.isEmpty() ? Confidence.NONE : suspects.get(0).confidence();
        for (Issue issue : issues) {
            if (confidenceRank(issue.confidence()) > confidenceRank(best)) {
                best = issue.confidence();
            }
        }
        return best;
    }

    private static int confidenceRank(Confidence confidence) {
        return switch (confidence) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
            case NONE -> 0;
        };
    }

    private static String suspectSummary(String prefix, List<Suspect> suspects) {
        if (suspects.isEmpty()) {
            return prefix + " No single installed mod could be identified confidently.";
        }
        Suspect first = suspects.get(0);
        if (suspects.size() == 1 || first.score() - suspects.get(1).score() >= 75) {
            return prefix + " The strongest suspect is " + first.displayName() + " (" + first.confidence() + ").";
        }
        return prefix + " Multiple mods have relevant evidence; " + first.displayName() + " currently ranks highest.";
    }

    private static Suspect firstSuspect(List<Suspect> suspects) {
        return suspects.isEmpty() ? null : suspects.get(0);
    }

    private static String buildDetails(
            String title,
            String summary,
            Confidence confidence,
            Path source,
            List<Suspect> suspects,
            List<Issue> issues,
            List<FixAction> fixes,
            List<String> suggestions,
            String rawText
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(title).append('\n');
        builder.append("Confidence: ").append(confidence).append('\n');
        if (source != null) {
            builder.append("Source: ").append(source).append('\n');
        }
        builder.append('\n').append(summary).append("\n\n");

        if (!issues.isEmpty()) {
            builder.append("Detected issues:\n");
            for (Issue issue : issues) {
                builder.append("- ")
                        .append(issue.title())
                        .append(" | ")
                        .append(issue.confidence())
                        .append(" | ")
                        .append(issue.description())
                        .append('\n');
            }
            builder.append('\n');
        }

        if (!suspects.isEmpty()) {
            builder.append("Suspected mods:\n");
            int index = 1;
            for (Suspect suspect : suspects) {
                builder.append(index++)
                        .append(". ")
                        .append(suspect.displayName())
                        .append(" [")
                        .append(suspect.id())
                        .append("] ")
                        .append(suspect.version())
                        .append(" | ")
                        .append(suspect.confidence())
                        .append(" | score ")
                        .append(suspect.score())
                        .append(" | ")
                        .append(suspect.reason())
                        .append('\n');
            }
            builder.append('\n');
        }

        if (!fixes.isEmpty()) {
            builder.append("Available fixes:\n");
            for (FixAction fix : fixes) {
                builder.append("- ")
                        .append(fix.label())
                        .append(" | ")
                        .append(fix.confidence())
                        .append(fix.autoSelected() ? " | recommended" : " | optional")
                        .append(" | ")
                        .append(fix.description())
                        .append('\n');
            }
            builder.append('\n');
        }

        if (!suggestions.isEmpty()) {
            builder.append("Suggestions:\n");
            for (String suggestion : suggestions) {
                builder.append("- ").append(suggestion).append('\n');
            }
            builder.append('\n');
        }

        if (rawText != null && !rawText.isBlank()) {
            String trimmed = rawText.length() > 28_000
                    ? rawText.substring(rawText.length() - 28_000)
                    : rawText;
            builder.append("Relevant crash context:\n").append(trimmed);
        }

        return builder.toString();
    }

    private static String focusLog(String logText) {
        if (logText == null || logText.isBlank()) {
            return "";
        }
        String lower = logText.toLowerCase(Locale.ROOT);
        int best = -1;
        String[] anchors = {
                "mixinapplyerror",
                "invalidinjectionexception",
                "critical injection failure",
                "modresolutionexception",
                "incompatible mod set",
                "incompatible mods found",
                "reported exception thrown",
                "unhandled exception",
                "uncaught exception in thread \"render thread\"",
                "uncaught exception in thread \"main\"",
                "exception in thread \"render thread\"",
                "exception in thread \"main\"",
                "exception_access_violation",
                "a fatal error has been detected"
        };
        for (String anchor : anchors) {
            int index = lower.lastIndexOf(anchor);
            if (index > best) {
                best = index;
            }
        }
        if (best < 0) {
            int error = lower.lastIndexOf("/error]");
            if (error < 0) {
                error = lower.lastIndexOf(" fatal ");
            }
            best = error;
        }
        if (best < 0) {
            return logText.length() > 24_000 ? logText.substring(logText.length() - 24_000) : logText;
        }
        int start = Math.max(0, best - 8_000);
        int end = Math.min(logText.length(), best + 36_000);
        return logText.substring(start, end);
    }

    private static Set<String> dependencyIds(String text) {
        Set<String> result = new HashSet<>();
        Matcher direct = MOD_IN_PARENS.matcher(text == null ? "" : text);
        while (direct.find()) {
            result.add(direct.group(1));
        }
        Matcher requires = REQUIRES_ID.matcher(text == null ? "" : text);
        while (requires.find()) {
            String id = requires.group(1);
            if (id.length() >= 2 && id.length() <= 80) {
                result.add(id);
            }
        }
        return result;
    }

    private static Path newestCrashReport(Path crashDir, long sessionStartedAt) {
        if (!Files.isDirectory(crashDir)) {
            return null;
        }
        try (var stream = Files.list(crashDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".txt"))
                    .filter(path -> modifiedMillis(path) >= Math.max(0L, sessionStartedAt - 2_000L))
                    .max(Comparator.comparingLong(RecoveryAnalyzer::modifiedMillis))
                    .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static long modifiedMillis(Path path) {
        try {
            FileTime time = Files.getLastModifiedTime(path);
            return time.toMillis();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private static String readText(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String readTail(Path path, int maxBytes) {
        if (path == null || !Files.isRegularFile(path)) {
            return "";
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            int start = Math.max(0, bytes.length - maxBytes);
            return new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (lower.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static int countOccurrences(String value, String needle) {
        if (needle == null || needle.isBlank()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
            if (count >= 10) {
                break;
            }
        }
        return count;
    }

    private static String suspectedModsSection(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("suspected mods:");
        if (start < 0) {
            return "";
        }
        String tail = text.substring(start);
        String[] lines = tail.split("\\R");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, 80); i++) {
            String trimmed = lines[i].trim();
            if (i > 0 && (trimmed.startsWith("-- ")
                    || trimmed.equalsIgnoreCase("Fabric Mods:")
                    || trimmed.equalsIgnoreCase("Stacktrace:")
                    || trimmed.startsWith("Details:"))) {
                break;
            }
            if (i > 0 && trimmed.isEmpty() && builder.length() > 0) {
                break;
            }
            builder.append(lines[i]).append('\n');
        }
        return builder.toString();
    }

    private static List<String> providedByIds(String text) {
        List<String> ids = new ArrayList<>();
        Matcher matcher = PROVIDED_BY.matcher(text == null ? "" : text);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String timestamp(long epochMillis) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis));
    }
}
