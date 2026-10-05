package dev.yogi.yogiessentials.recovery;

import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.FixType;
import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.IssueType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

final class RecoverySelfTest {
    private RecoverySelfTest() {
    }

    static String run(Path realGameDir) {
        Path root = realGameDir.resolve("yogiessentials").resolve("recovery").resolve("diagnosis-tests").resolve(Long.toString(System.currentTimeMillis()));
        List<TestResult> results = new ArrayList<>();
        try {
            Files.createDirectories(root);
            results.add(testMixin(root.resolve("mixin")));
            results.add(testDependency(root.resolve("dependency")));
            results.add(testDuplicate(root.resolve("duplicate")));
            results.add(testModIdCollision(root.resolve("mod-id-collision")));
            results.add(testExplicitIncompatibility(root.resolve("explicit-incompatibility")));
            results.add(testHardConflictHeadlinePriority(root.resolve("hard-conflict-headline")));
            results.add(testSoftConflict(root.resolve("soft-conflict")));
            results.add(testProvidedAlias(root.resolve("provided-alias")));
            results.add(testMissingDependencyPreflight(root.resolve("missing-dependency-preflight")));
            results.add(testVersionDependencyPreflight(root.resolve("version-dependency-preflight")));
            results.add(testRootMetadataParsing(root.resolve("root-metadata")));
            results.add(testBrokenConfig(root.resolve("config")));
            results.add(testDirectException(root.resolve("direct")));
            results.add(testLinkage(root.resolve("linkage")));
            results.add(testUnknown(root.resolve("unknown")));
            results.add(testNonFatalNetworkWarnings(root.resolve("network-warning")));
            results.add(testBenignConfigMentions(root.resolve("benign-config")));
            results.add(testForcedShutdownWithWarnings(root.resolve("forced-warning")));
            results.add(testOptionalMixinMissingClassWarnings(root.resolve("optional-mixin-warning")));
        } catch (IOException exception) {
            return "Crash Assistant diagnosis tests could not start:\n" + exception;
        }

        long passed = results.stream().filter(TestResult::passed).count();
        StringBuilder report = new StringBuilder();
        report.append("Yogi Essentials Crash Assistant — Diagnosis Test Suite\n\n");
        report.append("Result: ").append(passed).append('/').append(results.size()).append(" passed\n");
        report.append("Sandbox: ").append(root).append("\n\n");
        for (TestResult result : results) {
            report.append(result.passed() ? "PASS  " : "FAIL  ")
                    .append(result.name())
                    .append("\n      Expected: ")
                    .append(result.expected())
                    .append("\n      Detected: ")
                    .append(result.detected())
                    .append("\n\n");
        }
        report.append("All scenarios use generated sandbox files. No real mod or Minecraft config is modified.");
        return report.toString();
    }

    private static TestResult testMixin(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "testmixin", "Test Mixin Mod", "1.0.0", "com.example.testmixin", "testmixin.mixins.json");
        long started = System.currentTimeMillis() - 100L;
        writeCrash(gameDir, "---- Minecraft Crash Report ----\nMixinApplyError: Mixin [testmixin.mixins.json:FooMixin] from phase [DEFAULT] FAILED\nCaused by: org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException\n at com.example.testmixin.FooMixin.apply(FooMixin.java:20)\nSuspected Mods:\n\tTest Mixin Mod (testmixin)\n");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyze(gameDir, started);
        boolean pass = analysis.title().toLowerCase().contains("mixin")
                && topId(analysis).equals("testmixin")
                && analysis.confidence() == RecoveryAnalysis.Confidence.HIGH;
        return result("Mixin ownership", "testmixin / HIGH", analysis, pass);
    }

    private static TestResult testDependency(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "needymod", "Needy Mod", "1.0.0", "com.example.needy", null);
        long started = System.currentTimeMillis() - 100L;
        writeLog(gameDir, "[main/ERROR]: net.fabricmc.loader.impl.FormattedException: Some of your mods are incompatible with the game or each other!\nMod 'Needy Mod' (needymod) 1.0.0 requires version 9.9.9 or later of fabric-api, which is missing!\nModResolutionException: Could not resolve mod set\n");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyze(gameDir, started);
        boolean pass = analysis.title().toLowerCase().contains("dependency") && topId(analysis).equals("needymod");
        return result("Missing dependency", "needymod / dependency", analysis, pass);
    }

    private static TestResult testDuplicate(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "dupetest", "Duplicate Test", "1.0.0", "com.example.dupetest", null, "dupetest-copy-a.jar");
        createMod(gameDir, "dupetest", "Duplicate Test", "1.0.0", "com.example.dupetest", null, "dupetest-copy-b.jar");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyzeStartupEnvironment(gameDir);
        boolean issue = analysis.issues().stream().anyMatch(value -> value.type() == IssueType.DUPLICATE_MOD && value.title().toLowerCase().contains("duplicate mod file"));
        boolean optionalFix = analysis.fixes().stream().anyMatch(value -> value.type() == FixType.DISABLE_DUPLICATE && !value.autoSelected());
        boolean warning = analysis.title().toLowerCase().contains("warning");
        return result("Duplicate mod copies", "duplicate copy warning + optional reversible cleanup", analysis, issue && optionalFix && warning);
    }

    private static TestResult testModIdCollision(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "sharedid", "Optimizer Alpha", "1.0.0", "com.example.alpha", null, "optimizer-alpha.jar");
        createMod(gameDir, "sharedid", "Optimizer Beta", "0.2.0", "com.example.beta", null, "optimizer-beta.jar");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyzeStartupEnvironment(gameDir);
        boolean collision = analysis.issues().stream().anyMatch(value -> value.type() == IssueType.DUPLICATE_MOD && value.title().toLowerCase().contains("collision"));
        boolean noAutomaticDeletionChoice = analysis.fixes().stream().filter(value -> value.type() == FixType.DISABLE_MOD).noneMatch(RecoveryAnalysis.FixAction::autoSelected);
        return result("Mod-id collision distinction", "collision, not duplicate copy / no automatic side chosen", analysis, collision && noAutomaticDeletionChoice);
    }

    private static TestResult testExplicitIncompatibility(Path gameDir) throws IOException {
        setup(gameDir);
        createModWithRelations(gameDir, "sodiumtest", "Sodium Test", "0.8.12", "com.example.sodiumtest", null, "{\"vulkantest\":\"*\"}", null, "sodium-test.jar");
        createMod(gameDir, "vulkantest", "Vulkan Test", "0.6.8", "com.example.vulkantest", null, "vulkan-test.jar");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyzeStartupEnvironment(gameDir);
        boolean issue = analysis.issues().stream().anyMatch(value -> value.type() == IssueType.INCOMPATIBILITY);
        long alternatives = analysis.fixes().stream().filter(value -> value.type() == FixType.DISABLE_MOD).count();
        boolean noneSelected = analysis.fixes().stream().noneMatch(RecoveryAnalysis.FixAction::autoSelected);
        return result("Explicit renderer incompatibility", "two conflicting mods / HIGH / choose one", analysis, issue && alternatives >= 2 && noneSelected);
    }

    private static TestResult testHardConflictHeadlinePriority(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "sharedid", "Optimizer Alpha", "1.0.0", "com.example.alpha", null, "optimizer-alpha.jar");
        createMod(gameDir, "sharedid", "Optimizer Beta", "0.2.0", "com.example.beta", null, "optimizer-beta.jar");
        createModWithRelations(gameDir, "sodiumtest", "Sodium Test", "0.8.12", "com.example.sodiumtest", null, "{\"vulkantest\":\"*\"}", null, "sodium-test.jar");
        createMod(gameDir, "vulkantest", "Vulkan Test", "0.6.8", "com.example.vulkantest", null, "vulkan-test.jar");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyzeStartupEnvironment(gameDir);
        String summary = analysis.summary().toLowerCase();
        boolean pass = analysis.title().toLowerCase().contains("startup blocked")
                && summary.contains("sodium test")
                && summary.contains("vulkan test")
                && !summary.contains("optimizer alpha and optimizer beta have an explicit hard incompatibility")
                && !analysis.suspects().isEmpty()
                && (analysis.suspects().get(0).id().equals("sodiumtest") || analysis.suspects().get(0).id().equals("vulkantest"));
        return result("Hard-conflict headline priority", "actual hard conflict outranks unrelated mod-id collision", analysis, pass);
    }

    private static TestResult testSoftConflict(Path gameDir) throws IOException {
        setup(gameDir);
        createModWithRelations(gameDir, "softa", "Soft A", "1.0.0", "com.example.softa", null, null, "{\"softb\":\"*\"}", "soft-a.jar");
        createMod(gameDir, "softb", "Soft B", "1.0.0", "com.example.softb", null, "soft-b.jar");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyzeStartupEnvironment(gameDir);
        boolean warning = analysis.title().toLowerCase().contains("warning");
        boolean medium = analysis.issues().stream().anyMatch(value -> value.type() == IssueType.INCOMPATIBILITY && value.confidence() == RecoveryAnalysis.Confidence.MEDIUM);
        boolean noAutomatic = analysis.fixes().stream().noneMatch(RecoveryAnalysis.FixAction::autoSelected);
        return result("Soft conflict restraint", "warning only / not startup blocked", analysis, warning && medium && noAutomatic);
    }

    private static TestResult testProvidedAlias(Path gameDir) throws IOException {
        setup(gameDir);
        createModWithProvides(gameDir, "provider", "Provider Mod", "2.0.0", "virtual-api", "provider.jar");
        createModWithRelations(gameDir, "consumer", "Consumer Mod", "1.0.0", "com.example.consumer", "{\"virtual-api\":\">=2.0.0\"}", null, null, "consumer.jar");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyzeStartupEnvironment(gameDir);
        boolean missing = analysis.issues().stream().anyMatch(value -> value.type() == IssueType.DEPENDENCY);
        return result("Fabric provides alias", "provided alias satisfies dependency", analysis, !missing);
    }

    private static TestResult testMissingDependencyPreflight(Path gameDir) throws IOException {
        setup(gameDir);
        createModWithRelations(gameDir, "needslib", "Needs Lib", "1.0.0", "com.example.needslib", "{\"missinglib\":\">=1.2.0\"}", null, null, "needs-lib.jar");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyzeStartupEnvironment(gameDir);
        boolean issue = analysis.issues().stream().anyMatch(value -> value.type() == IssueType.DEPENDENCY && value.title().toLowerCase().contains("missing"));
        return result("Startup missing dependency", "missinglib identified before Minecraft init", analysis, issue && topId(analysis).equals("needslib"));
    }

    private static TestResult testVersionDependencyPreflight(Path gameDir) throws IOException {
        setup(gameDir);
        createModWithRelations(gameDir, "needsnewlib", "Needs New Lib", "1.0.0", "com.example.needsnew", "{\"sharedlib\":\">=2.0.0 <3.0.0\"}", null, null, "needs-new-lib.jar");
        createMod(gameDir, "sharedlib", "Shared Lib", "1.5.0", "com.example.sharedlib", null, "shared-lib.jar");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyzeStartupEnvironment(gameDir);
        boolean issue = analysis.issues().stream().anyMatch(value -> value.type() == IssueType.DEPENDENCY && value.title().toLowerCase().contains("version"));
        return result("Startup dependency version mismatch", ">=2.0.0 <3.0.0 rejects 1.5.0", analysis, issue);
    }

    private static TestResult testRootMetadataParsing(Path gameDir) throws IOException {
        setup(gameDir);
        Path jar = gameDir.resolve("mods").resolve("root-parser.jar");
        String metadata = "{\n"
                + "  \"schemaVersion\": 1,\n"
                + "  \"custom\": {\"id\": \"wrong_nested_id\", \"name\": \"Wrong Nested Name\"},\n"
                + "  \"id\": \"correctroot\",\n"
                + "  \"version\": \"1.0.0\",\n"
                + "  \"name\": \"Correct Root\"\n"
                + "}";
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("fabric.mod.json"));
            output.write(metadata.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        List<RecoveryModScanner.ModFile> mods = RecoveryModScanner.scan(gameDir.resolve("mods"));
        boolean pass = mods.size() == 1 && mods.get(0).id().equals("correctroot") && mods.get(0).name().equals("Correct Root");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyzeStartupEnvironment(gameDir);
        return result("Root fabric.mod.json parsing", "root id/name used instead of nested id/name", analysis, pass);
    }

    private static TestResult testBrokenConfig(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "configmod", "Config Mod", "1.0.0", "com.example.configmod", null);
        Files.writeString(gameDir.resolve("config").resolve("configmod.json"), "{ broken", StandardCharsets.UTF_8);
        long started = System.currentTimeMillis() - 100L;
        writeCrash(gameDir, "---- Minecraft Crash Report ----\njava.lang.RuntimeException: Failed to load configuration file config/configmod.json\nCaused by: com.google.gson.JsonSyntaxException: malformed json\n at com.example.configmod.Config.load(Config.java:42)\n");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyze(gameDir, started);
        boolean issue = analysis.issues().stream().anyMatch(value -> value.type() == IssueType.CONFIG);
        boolean fix = analysis.fixes().stream().anyMatch(value -> value.type() == FixType.RESET_CONFIG && value.autoSelected());
        return result("Broken config", "config issue + backed-up config reset", analysis, issue && fix);
    }

    private static TestResult testDirectException(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "directmod", "Direct Crash Mod", "1.0.0", "com.example.directmod", null);
        long started = System.currentTimeMillis() - 100L;
        writeCrash(gameDir, "---- Minecraft Crash Report ----\njava.lang.RuntimeException: Test crash\n at com.example.directmod.Core.tick(Core.java:17)\n at net.minecraft.client.MinecraftClient.run(MinecraftClient.java:1)\n");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyze(gameDir, started);
        boolean pass = topId(analysis).equals("directmod")
                && analysis.suspects().get(0).confidence() != RecoveryAnalysis.Confidence.LOW;
        return result("Direct mod exception", "directmod / MEDIUM or HIGH", analysis, pass);
    }

    private static TestResult testLinkage(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "apimod", "API Crash Mod", "1.0.0", "com.example.apimod", null);
        long started = System.currentTimeMillis() - 100L;
        writeCrash(gameDir, "---- Minecraft Crash Report ----\njava.lang.NoSuchMethodError: com.example.apimod.Api.newMethod()V\n at com.example.apimod.Client.start(Client.java:9)\n");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyze(gameDir, started);
        boolean issue = analysis.issues().stream().anyMatch(value -> value.type() == IssueType.LINKAGE);
        return result("Missing/incompatible API", "linkage issue + apimod", analysis, issue && topId(analysis).equals("apimod"));
    }

    private static TestResult testUnknown(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "innocent", "Innocent Mod", "1.0.0", "com.example.innocent", null);
        long started = System.currentTimeMillis() - 100L;
        writeCrash(gameDir, "---- Minecraft Crash Report ----\njava.lang.RuntimeException: Unknown synthetic crash\n at net.minecraft.client.MinecraftClient.run(MinecraftClient.java:1)\n at java.base/java.lang.Thread.run(Thread.java:1)\n");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyze(gameDir, started);
        boolean pass = analysis.suspects().isEmpty()
                && analysis.fixes().stream().noneMatch(value -> value.type() == FixType.DISABLE_MOD);
        return result("Unknown crash restraint", "no mod blamed and no mod-disable fix", analysis, pass);
    }

    private static TestResult testNonFatalNetworkWarnings(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "normalmod", "Normal Mod", "1.0.0", "com.example.normalmod", null);
        long started = System.currentTimeMillis() - 100L;
        writeLog(gameDir, "[Download-29/WARN]: Failed to load texture for profile abc\n"
                + "java.util.concurrent.CompletionException: java.io.UncheckedIOException: java.net.ConnectException: Connection timed out: connect\n"
                + "\tat java.base/java.util.concurrent.CompletableFuture.encodeThrowable(CompletableFuture.java:315)\n"
                + "Caused by: java.io.UncheckedIOException: java.net.ConnectException: Connection timed out: connect\n"
                + "\tat knot//net.minecraft.class_10538.method_65866(class_10538.java:46)\n"
                + "Caused by: java.net.ConnectException: Connection timed out: connect\n"
                + "[Render thread/INFO]: [System] [CHAT] hello\n");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyzeCurrentLogs(gameDir, started);
        boolean pass = analysis.title().equals("No crash detected in current logs")
                && analysis.suspects().isEmpty()
                && analysis.issues().stream().noneMatch(value -> value.type() == IssueType.DIRECT_EXCEPTION || value.type() == IssueType.CONFIG);
        return result("Non-fatal network warning restraint", "healthy session / no crash", analysis, pass);
    }

    private static TestResult testBenignConfigMentions(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "configok", "Config OK Mod", "1.0.0", "com.example.configok", null);
        Files.writeString(gameDir.resolve("config").resolve("configok.json"), "{}", StandardCharsets.UTF_8);
        long started = System.currentTimeMillis() - 100L;
        writeLog(gameDir, "[main/INFO]: Loaded configuration file config/configok.json\n"
                + "[main/INFO]: Initialized com.example.configok.Client\n"
                + "[Render thread/INFO]: Minecraft started normally\n");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyzeCurrentLogs(gameDir, started);
        boolean pass = analysis.title().equals("No crash detected in current logs")
                && analysis.issues().stream().noneMatch(value -> value.type() == IssueType.CONFIG)
                && analysis.suspects().isEmpty();
        return result("Benign config mention restraint", "healthy config load / no crash", analysis, pass);
    }

    private static TestResult testOptionalMixinMissingClassWarnings(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "appleskintest", "AppleSkin Test", "1.0.0", "com.example.appleskintest", "appleskintest.jei.mixins.json");
        long started = System.currentTimeMillis() - 100L;
        writeLog(gameDir,
                "[main/WARN]: Error loading class: mezz/jei/fabric/platform/RenderHelper (java.lang.ClassNotFoundException: mezz/jei/fabric/platform/RenderHelper)\n"
                        + "[main/WARN]: @Mixin target mezz.jei.fabric.platform.RenderHelper was not found appleskintest.jei.mixins.json:JEIRenderHelperMixin from mod appleskintest\n"
                        + "[Render thread/INFO]: Setting user: TestPlayer\n"
                        + "[Render thread/INFO]: Backend library: LWJGL version test\n");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyze(gameDir, started);
        boolean pass = analysis.confidence() == RecoveryAnalysis.Confidence.NONE
                && analysis.suspects().isEmpty()
                && analysis.fixes().stream().noneMatch(value -> value.type() == FixType.DISABLE_MOD);
        return result("Optional mixin missing-class restraint", "healthy startup warning / no crash / no mod disable", analysis, pass);
    }

    private static TestResult testForcedShutdownWithWarnings(Path gameDir) throws IOException {
        setup(gameDir);
        createMod(gameDir, "normalmod", "Normal Mod", "1.0.0", "com.example.normalmod", null);
        long started = System.currentTimeMillis() - 100L;
        writeLog(gameDir, "[Download-1/WARN]: Failed to load texture\n"
                + "java.util.concurrent.CompletionException: java.net.ConnectException: Connection timed out\n"
                + "Caused by: java.net.ConnectException: Connection timed out\n"
                + "[Render thread/INFO]: Game is still running\n");
        RecoveryAnalysis analysis = RecoveryAnalyzer.analyze(gameDir, started);
        boolean pass = analysis.title().equals("Unexpected Minecraft shutdown detected")
                && analysis.suspects().isEmpty()
                && analysis.issues().stream().noneMatch(value -> value.type() == IssueType.DIRECT_EXCEPTION || value.type() == IssueType.CONFIG);
        return result("Forced shutdown warning restraint", "unexpected shutdown / no fake crash", analysis, pass);
    }

    private static void setup(Path gameDir) throws IOException {
        Files.createDirectories(gameDir.resolve("mods"));
        Files.createDirectories(gameDir.resolve("logs"));
        Files.createDirectories(gameDir.resolve("crash-reports"));
        Files.createDirectories(gameDir.resolve("config"));
    }

    private static void createMod(Path gameDir, String id, String name, String version, String packageName, String mixin) throws IOException {
        createMod(gameDir, id, name, version, packageName, mixin, id + "-" + version + ".jar");
    }

    private static void createMod(Path gameDir, String id, String name, String version, String packageName, String mixin, String fileName) throws IOException {
        Path jar = gameDir.resolve("mods").resolve(fileName);
        String mixinPart = mixin == null ? "" : ",\n  \"mixins\": [\"" + mixin + "\"]";
        String metadata = "{\n"
                + "  \"schemaVersion\": 1,\n"
                + "  \"id\": \"" + id + "\",\n"
                + "  \"version\": \"" + version + "\",\n"
                + "  \"name\": \"" + name + "\",\n"
                + "  \"entrypoints\": {\"client\": [\"" + packageName + ".Client\"]}"
                + mixinPart
                + "\n}";
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("fabric.mod.json"));
            output.write(metadata.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            String classPath = packageName.replace('.', '/') + "/Client.class";
            output.putNextEntry(new JarEntry(classPath));
            output.write(new byte[]{0});
            output.closeEntry();
            if (mixin != null) {
                output.putNextEntry(new JarEntry(mixin));
                output.write("{}".getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
    }

    private static void createModWithProvides(Path gameDir, String id, String name, String version, String alias, String fileName) throws IOException {
        Path jar = gameDir.resolve("mods").resolve(fileName);
        String metadata = "{\n"
                + "  \"schemaVersion\": 1,\n"
                + "  \"id\": \"" + id + "\",\n"
                + "  \"version\": \"" + version + "\",\n"
                + "  \"name\": \"" + name + "\",\n"
                + "  \"provides\": [\"" + alias + "\"]\n"
                + "}";
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("fabric.mod.json"));
            output.write(metadata.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }

    private static void createModWithRelations(
            Path gameDir,
            String id,
            String name,
            String version,
            String packageName,
            String depends,
            String breaks,
            String conflicts,
            String fileName
    ) throws IOException {
        Path jar = gameDir.resolve("mods").resolve(fileName);
        StringBuilder metadata = new StringBuilder("{\n")
                .append("  \"schemaVersion\": 1,\n")
                .append("  \"id\": \"").append(id).append("\",\n")
                .append("  \"version\": \"").append(version).append("\",\n")
                .append("  \"name\": \"").append(name).append("\",\n")
                .append("  \"entrypoints\": {\"client\": [\"").append(packageName).append(".Client\"]}");
        if (depends != null) {
            metadata.append(",\n  \"depends\": ").append(depends);
        }
        if (breaks != null) {
            metadata.append(",\n  \"breaks\": ").append(breaks);
        }
        if (conflicts != null) {
            metadata.append(",\n  \"conflicts\": ").append(conflicts);
        }
        metadata.append("\n}");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("fabric.mod.json"));
            output.write(metadata.toString().getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            String classPath = packageName.replace('.', '/') + "/Client.class";
            output.putNextEntry(new JarEntry(classPath));
            output.write(new byte[]{0});
            output.closeEntry();
        }
    }

    private static void writeCrash(Path gameDir, String text) throws IOException {
        Path file = gameDir.resolve("crash-reports").resolve("crash-test.txt");
        Files.writeString(file, text, StandardCharsets.UTF_8);
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
    }

    private static void writeLog(Path gameDir, String text) throws IOException {
        Path file = gameDir.resolve("logs").resolve("latest.log");
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static String topId(RecoveryAnalysis analysis) {
        return analysis.suspects().isEmpty() ? "" : analysis.suspects().get(0).id();
    }

    private static TestResult result(String name, String expected, RecoveryAnalysis analysis, boolean passed) {
        String detected = analysis.title()
                + " | suspect=" + (topId(analysis).isBlank() ? "none" : topId(analysis))
                + " | confidence=" + analysis.confidence()
                + " | fixes=" + analysis.fixes().size();
        return new TestResult(name, expected, detected, passed);
    }

    private record TestResult(String name, String expected, String detected, boolean passed) {
    }
}
