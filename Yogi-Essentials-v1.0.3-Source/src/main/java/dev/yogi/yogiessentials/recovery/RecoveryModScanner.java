package dev.yogi.yogiessentials.recovery;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

final class RecoveryModScanner {
    private static final Set<String> BUILTIN_DEPENDENCIES = Set.of("minecraft", "java", "fabricloader");

    private RecoveryModScanner() {
    }

    static List<ModFile> scan(Path modsDir) {
        List<ModFile> mods = new ArrayList<>();
        if (modsDir == null || !Files.isDirectory(modsDir)) {
            return mods;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir, "*.jar")) {
            for (Path jar : stream) {
                ModFile mod = readMod(jar);
                if (mod != null) {
                    mods.add(mod);
                }
            }
        } catch (IOException ignored) {
        }

        mods.sort(Comparator.comparing(mod -> mod.name().toLowerCase(Locale.ROOT)));
        return mods;
    }

    static Map<String, List<ModFile>> duplicates(List<ModFile> mods) {
        Map<String, List<ModFile>> grouped = new LinkedHashMap<>();
        for (ModFile mod : mods) {
            grouped.computeIfAbsent(mod.id().toLowerCase(Locale.ROOT), ignored -> new ArrayList<>()).add(mod);
        }
        grouped.entrySet().removeIf(entry -> entry.getValue().size() < 2);
        for (List<ModFile> group : grouped.values()) {
            group.sort(RecoveryModScanner::comparePreferredVersion);
        }
        return grouped;
    }

    static boolean likelySameModCopies(List<ModFile> group) {
        if (group == null || group.size() < 2) {
            return false;
        }
        String name = normalize(group.get(0).name());
        String version = normalize(group.get(0).version());
        for (int i = 1; i < group.size(); i++) {
            ModFile mod = group.get(i);
            if (!name.equals(normalize(mod.name())) || !version.equals(normalize(mod.version()))) {
                return false;
            }
        }
        return true;
    }

    static List<CompatibilityProblem> compatibilityProblems(List<ModFile> mods) {
        List<CompatibilityProblem> result = new ArrayList<>();
        Map<String, List<ModFile>> byId = new LinkedHashMap<>();
        Map<String, List<String>> providedVersions = new LinkedHashMap<>();
        for (ModFile mod : mods) {
            byId.computeIfAbsent(mod.id().toLowerCase(Locale.ROOT), ignored -> new ArrayList<>()).add(mod);
            for (Map.Entry<String, String> entry : mod.providedVersions().entrySet()) {
                providedVersions.computeIfAbsent(entry.getKey().toLowerCase(Locale.ROOT), ignored -> new ArrayList<>()).add(entry.getValue());
            }
        }

        for (Map.Entry<String, List<ModFile>> entry : duplicates(mods).entrySet()) {
            List<ModFile> group = entry.getValue();
            boolean copy = likelySameModCopies(group);
            ModFile first = group.get(0);
            String description = copy
                    ? "Multiple top-level jar files provide the same mod id, name, and version."
                    : "Different top-level jar files declare the same Fabric mod id. Fabric cannot safely load both as separate mods.";
            result.add(new CompatibilityProblem(
                    copy ? "DUPLICATE_COPY" : "MOD_ID_COLLISION",
                    first,
                    group.size() > 1 ? group.get(1) : null,
                    entry.getKey(),
                    List.of(),
                    description,
                    RecoveryAnalysis.Confidence.HIGH
            ));
        }

        Set<String> relationKeys = new LinkedHashSet<>();
        for (ModFile mod : mods) {
            collectIncompatibilities(result, relationKeys, mod, mod.breaks(), byId, "BREAKS");
            collectIncompatibilities(result, relationKeys, mod, mod.conflicts(), byId, "CONFLICTS");

            for (Map.Entry<String, List<String>> dependency : mod.depends().entrySet()) {
                String dependencyId = dependency.getKey().toLowerCase(Locale.ROOT);
                if (BUILTIN_DEPENDENCIES.contains(dependencyId)) {
                    continue;
                }
                List<String> versions = providedVersions.get(dependencyId);
                if (versions == null || versions.isEmpty()) {
                    result.add(new CompatibilityProblem(
                            "MISSING_DEPENDENCY",
                            mod,
                            null,
                            dependencyId,
                            dependency.getValue(),
                            mod.name() + " requires " + dependencyId + " but no installed top-level or bundled Fabric mod provides it.",
                            RecoveryAnalysis.Confidence.HIGH
                    ));
                    continue;
                }
                Boolean satisfied = anyVersionMatches(versions, dependency.getValue());
                if (Boolean.FALSE.equals(satisfied)) {
                    result.add(new CompatibilityProblem(
                            "VERSION_DEPENDENCY",
                            mod,
                            findTopLevel(byId, dependencyId),
                            dependencyId,
                            dependency.getValue(),
                            mod.name() + " requires a different version of " + dependencyId + ".",
                            RecoveryAnalysis.Confidence.HIGH
                    ));
                }
            }
        }
        return result;
    }

    private static void collectIncompatibilities(
            List<CompatibilityProblem> result,
            Set<String> relationKeys,
            ModFile source,
            Map<String, List<String>> relations,
            Map<String, List<ModFile>> byId,
            String kind
    ) {
        for (Map.Entry<String, List<String>> relation : relations.entrySet()) {
            String targetId = relation.getKey().toLowerCase(Locale.ROOT);
            List<ModFile> targets = byId.get(targetId);
            if (targets == null || targets.isEmpty()) {
                continue;
            }
            for (ModFile target : targets) {
                Boolean matches = matchesAnyPredicate(target.version(), relation.getValue());
                if (!Boolean.TRUE.equals(matches)) {
                    continue;
                }
                String a = source.id().toLowerCase(Locale.ROOT);
                String b = target.id().toLowerCase(Locale.ROOT);
                String key = kind + "|" + (a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a);
                if (!relationKeys.add(key)) {
                    continue;
                }
                boolean hard = kind.equals("BREAKS");
                String description = hard
                        ? source.name() + " explicitly declares " + target.name() + " as a hard incompatibility" + predicateDescription(relation.getValue()) + "."
                        : source.name() + " declares a soft compatibility conflict with " + target.name() + predicateDescription(relation.getValue()) + ". Fabric may warn, but this alone does not block startup.";
                result.add(new CompatibilityProblem(
                        hard ? "INCOMPATIBILITY" : "SOFT_CONFLICT",
                        source,
                        target,
                        targetId,
                        relation.getValue(),
                        description,
                        hard ? RecoveryAnalysis.Confidence.HIGH : RecoveryAnalysis.Confidence.MEDIUM
                ));
            }
        }
    }

    private static String predicateDescription(List<String> predicates) {
        if (predicates == null || predicates.isEmpty() || predicates.stream().allMatch(value -> value.equals("*"))) {
            return " for the installed version";
        }
        return " for version rule " + String.join(" OR ", predicates);
    }

    private static ModFile findTopLevel(Map<String, List<ModFile>> byId, String id) {
        List<ModFile> values = byId.get(id.toLowerCase(Locale.ROOT));
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static Boolean anyVersionMatches(List<String> versions, List<String> predicates) {
        boolean sawKnown = false;
        for (String version : versions) {
            Boolean value = matchesAnyPredicate(version, predicates);
            if (Boolean.TRUE.equals(value)) {
                return true;
            }
            if (Boolean.FALSE.equals(value)) {
                sawKnown = true;
            }
        }
        return sawKnown ? Boolean.FALSE : null;
    }

    private static Boolean matchesAnyPredicate(String version, List<String> predicates) {
        if (predicates == null || predicates.isEmpty()) {
            return true;
        }
        boolean sawKnown = false;
        for (String predicate : predicates) {
            Boolean matches = matchesPredicate(version, predicate);
            if (Boolean.TRUE.equals(matches)) {
                return true;
            }
            if (Boolean.FALSE.equals(matches)) {
                sawKnown = true;
            }
        }
        return sawKnown ? Boolean.FALSE : null;
    }

    private static Boolean matchesPredicate(String version, String predicate) {
        String expression = safe(predicate).trim();
        if (expression.isEmpty() || expression.equals("*")) {
            return true;
        }
        if (expression.contains("||")) {
            boolean sawKnown = false;
            for (String part : expression.split("\\|\\|")) {
                Boolean value = matchesPredicate(version, part.trim());
                if (Boolean.TRUE.equals(value)) {
                    return true;
                }
                if (Boolean.FALSE.equals(value)) {
                    sawKnown = true;
                }
            }
            return sawKnown ? Boolean.FALSE : null;
        }

        List<String> clauses = splitVersionClauses(expression);
        if (clauses.size() > 1) {
            boolean sawKnown = false;
            for (String clause : clauses) {
                Boolean value = matchesPredicate(version, clause);
                if (Boolean.FALSE.equals(value)) {
                    return false;
                }
                if (value == null) {
                    return null;
                }
                sawKnown = true;
            }
            return sawKnown ? Boolean.TRUE : null;
        }

        String clause = expression.trim();
        if (clause.startsWith("^")) {
            String base = clause.substring(1).trim();
            int cmp = compareVersionTokens(version, base);
            String upper = caretUpperBound(base);
            return cmp >= 0 && upper != null && compareVersionTokens(version, upper) < 0;
        }
        if (clause.startsWith("~")) {
            String base = clause.substring(1).trim();
            int cmp = compareVersionTokens(version, base);
            String upper = tildeUpperBound(base);
            return cmp >= 0 && upper != null && compareVersionTokens(version, upper) < 0;
        }

        String operator = "";
        for (String candidate : List.of(">=", "<=", ">", "<", "=")) {
            if (clause.startsWith(candidate)) {
                operator = candidate;
                clause = clause.substring(candidate.length()).trim();
                break;
            }
        }
        if (clause.isEmpty()) {
            return null;
        }
        if (clause.contains("*") || clause.toLowerCase(Locale.ROOT).contains("x")) {
            if (!operator.isEmpty() && !operator.equals("=")) {
                return null;
            }
            return wildcardMatches(version, clause);
        }
        if (!clause.matches("[0-9A-Za-z][0-9A-Za-z._+\\-]*")) {
            return null;
        }
        int cmp = compareVersionTokens(version, clause);
        return switch (operator) {
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            default -> cmp == 0;
        };
    }

    private static List<String> splitVersionClauses(String expression) {
        String normalized = expression.trim().replaceAll("\\s*,\\s*", " ");
        String[] tokens = normalized.split("\\s+");
        List<String> result = new ArrayList<>();
        String pendingOperator = null;
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (token.equals(">") || token.equals(">=") || token.equals("<") || token.equals("<=") || token.equals("=")) {
                pendingOperator = token;
                continue;
            }
            if (pendingOperator != null) {
                result.add(pendingOperator + token);
                pendingOperator = null;
            } else {
                result.add(token);
            }
        }
        if (pendingOperator != null) {
            return List.of(expression);
        }
        return result;
    }

    private static boolean wildcardMatches(String version, String pattern) {
        String core = safe(version).split("\\+", 2)[0].split("-", 2)[0];
        String[] actual = core.split("\\.");
        String[] expected = pattern.split("\\.");
        for (int i = 0; i < expected.length; i++) {
            String part = expected[i];
            if (part.equals("*") || part.equalsIgnoreCase("x")) {
                return true;
            }
            if (i >= actual.length || !part.equalsIgnoreCase(actual[i])) {
                return false;
            }
        }
        return actual.length == expected.length;
    }

    private static String caretUpperBound(String base) {
        int[] parts = numericVersion(base);
        if (parts == null) {
            return null;
        }
        if (parts[0] > 0) {
            return (parts[0] + 1) + ".0.0";
        }
        if (parts[1] > 0) {
            return "0." + (parts[1] + 1) + ".0";
        }
        return "0.0." + (parts[2] + 1);
    }

    private static String tildeUpperBound(String base) {
        int[] parts = numericVersion(base);
        if (parts == null) {
            return null;
        }
        return parts[0] + "." + (parts[1] + 1) + ".0";
    }

    private static int[] numericVersion(String value) {
        String core = safe(value).split("\\+", 2)[0].split("-", 2)[0];
        String[] tokens = core.split("\\.");
        int[] result = new int[] {0, 0, 0};
        try {
            for (int i = 0; i < Math.min(3, tokens.length); i++) {
                result[i] = Integer.parseInt(tokens[i]);
            }
            return result;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int comparePreferredVersion(ModFile a, ModFile b) {
        int version = compareVersionTokens(b.version(), a.version());
        if (version != 0) {
            return version;
        }
        return Long.compare(modifiedMillis(b.jarPath()), modifiedMillis(a.jarPath()));
    }

    private static int compareVersionTokens(String left, String right) {
        String leftCore = safe(left).split("\\+", 2)[0];
        String rightCore = safe(right).split("\\+", 2)[0];
        String[] a = leftCore.split("[^0-9A-Za-z]+");
        String[] b = rightCore.split("[^0-9A-Za-z]+");
        int max = Math.max(a.length, b.length);
        for (int i = 0; i < max; i++) {
            String av = i < a.length ? a[i] : "0";
            String bv = i < b.length ? b[i] : "0";
            int cmp;
            if (av.matches("\\d+") && bv.matches("\\d+")) {
                try {
                    cmp = Long.compare(Long.parseLong(av), Long.parseLong(bv));
                } catch (NumberFormatException ignored) {
                    cmp = av.compareToIgnoreCase(bv);
                }
            } else {
                cmp = av.compareToIgnoreCase(bv);
            }
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private static ModFile readMod(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry metadataEntry = zip.getEntry("fabric.mod.json");
            if (metadataEntry == null) {
                return null;
            }

            String metadata;
            try (var input = zip.getInputStream(metadataEntry)) {
                metadata = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            Object parsed = parseJson(metadata);
            if (!(parsed instanceof Map<?, ?> root)) {
                return null;
            }

            String id = stringValue(root.get("id"), stripExtension(jar.getFileName().toString()));
            String name = stringValue(root.get("name"), id);
            String version = stringValue(root.get("version"), "unknown");
            Set<String> packagePrefixes = collectPackagePrefixes(zip);
            Set<String> mixinConfigs = collectMixinConfigs(root, zip);
            Set<String> entrypoints = collectEntrypoints(root);
            Map<String, List<String>> depends = relationMap(root.get("depends"));
            Map<String, List<String>> breaks = relationMap(root.get("breaks"));
            Map<String, List<String>> conflicts = relationMap(root.get("conflicts"));
            Map<String, String> providedVersions = new LinkedHashMap<>();
            providedVersions.put(id.toLowerCase(Locale.ROOT), version);
            collectProvidedAliases(root, version, providedVersions);
            collectBundledVersions(root, zip, providedVersions, 0);
            return new ModFile(
                    id,
                    name,
                    version,
                    jar.toAbsolutePath().normalize(),
                    packagePrefixes,
                    mixinConfigs,
                    entrypoints,
                    depends,
                    breaks,
                    conflicts,
                    providedVersions
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void collectProvidedAliases(Map<?, ?> root, String version, Map<String, String> providedVersions) {
        Object value = root.get("provides");
        if (!(value instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof String alias && !alias.isBlank()) {
                providedVersions.putIfAbsent(alias.toLowerCase(Locale.ROOT), version);
            }
        }
    }

    private static void collectBundledVersions(Map<?, ?> root, ZipFile zip, Map<String, String> providedVersions, int depth) {
        if (depth > 1) {
            return;
        }
        Object jars = root.get("jars");
        if (!(jars instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String file = stringValue(map.get("file"), "");
            if (file.isBlank()) {
                continue;
            }
            ZipEntry entry = zip.getEntry(file);
            if (entry == null) {
                continue;
            }
            try (var input = zip.getInputStream(entry)) {
                byte[] bytes = input.readAllBytes();
                Map<?, ?> nested = nestedMetadata(bytes);
                if (nested == null) {
                    continue;
                }
                String id = stringValue(nested.get("id"), "");
                String version = stringValue(nested.get("version"), "unknown");
                if (!id.isBlank()) {
                    providedVersions.putIfAbsent(id.toLowerCase(Locale.ROOT), version);
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static Map<?, ?> nestedMetadata(byte[] jarBytes) {
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals("fabric.mod.json")) {
                    Object parsed = parseJson(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                    return parsed instanceof Map<?, ?> map ? map : null;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static Set<String> collectPackagePrefixes(ZipFile zip) {
        Map<String, Integer> counts = new HashMap<>();
        var entries = zip.entries();
        int scanned = 0;
        while (entries.hasMoreElements() && scanned < 8000) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() || !name.endsWith(".class") || name.startsWith("META-INF/")) {
                continue;
            }
            scanned++;
            String[] parts = name.substring(0, name.length() - 6).split("/");
            if (parts.length < 3) {
                continue;
            }
            int maxParts = Math.min(5, parts.length - 1);
            for (int packageParts = 3; packageParts <= maxParts; packageParts++) {
                StringBuilder prefix = new StringBuilder();
                for (int i = 0; i < packageParts; i++) {
                    if (i > 0) {
                        prefix.append('.');
                    }
                    prefix.append(parts[i]);
                }
                String value = prefix.toString();
                if (!isCommonLibraryPrefix(value)) {
                    counts.merge(value, 1, Integer::sum);
                }
            }
        }
        return counts.entrySet()
                .stream()
                .sorted((a, b) -> {
                    int partsA = a.getKey().split("\\.").length;
                    int partsB = b.getKey().split("\\.").length;
                    int specific = Integer.compare(partsB, partsA);
                    return specific != 0 ? specific : Integer.compare(b.getValue(), a.getValue());
                })
                .limit(24)
                .map(Map.Entry::getKey)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }

    private static Set<String> collectMixinConfigs(Map<?, ?> root, ZipFile zip) {
        Set<String> result = new LinkedHashSet<>();
        collectStrings(root.get("mixins"), value -> {
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".json")) {
                result.add(value);
            }
        });
        var entries = zip.entries();
        while (entries.hasMoreElements() && result.size() < 40) {
            ZipEntry entry = entries.nextElement();
            String value = entry.getName();
            String lower = value.toLowerCase(Locale.ROOT);
            if (!entry.isDirectory() && (lower.endsWith(".mixins.json") || lower.endsWith(".mixin.json"))) {
                result.add(value);
                result.add(Path.of(value).getFileName().toString());
            }
        }
        return result;
    }

    private static Set<String> collectEntrypoints(Map<?, ?> root) {
        Set<String> result = new LinkedHashSet<>();
        collectStrings(root.get("entrypoints"), value -> {
            String className = value.contains("::") ? value.substring(0, value.indexOf("::")) : value;
            if (looksLikeClassName(className) && result.size() < 48) {
                result.add(className);
            }
        });
        return result;
    }

    private static void collectStrings(Object value, java.util.function.Consumer<String> consumer) {
        if (value instanceof String string) {
            consumer.accept(string);
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                collectStrings(item, consumer);
            }
        } else if (value instanceof Map<?, ?> map) {
            for (Object item : map.values()) {
                collectStrings(item, consumer);
            }
        }
    }

    private static Map<String, List<String>> relationMap(Object value) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (!(value instanceof Map<?, ?> map)) {
            return result;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            List<String> predicates = new ArrayList<>();
            collectStrings(entry.getValue(), predicates::add);
            if (predicates.isEmpty()) {
                predicates.add("*");
            }
            result.put(key, List.copyOf(predicates));
        }
        return result;
    }

    private static boolean looksLikeClassName(String value) {
        if (value.length() < 5 || value.length() > 220 || value.contains("/") || value.contains(" ")) {
            return false;
        }
        if (!value.contains(".") || value.startsWith("http") || value.matches("[0-9].*")) {
            return false;
        }
        String[] parts = value.split("\\.");
        if (parts.length < 3) {
            return false;
        }
        for (String part : parts) {
            if (part.isBlank() || !Character.isJavaIdentifierStart(part.charAt(0))) {
                return false;
            }
            for (int i = 1; i < part.length(); i++) {
                if (!Character.isJavaIdentifierPart(part.charAt(i)) && part.charAt(i) != '$') {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isCommonLibraryPrefix(String prefix) {
        return prefix.startsWith("net.minecraft.")
                || prefix.startsWith("net.fabricmc.")
                || prefix.startsWith("org.spongepowered.")
                || prefix.startsWith("org.slf4j.")
                || prefix.startsWith("org.apache.")
                || prefix.startsWith("org.jetbrains.")
                || prefix.startsWith("com.google.gson")
                || prefix.startsWith("com.google.common")
                || prefix.startsWith("com.mojang.");
    }

    private static Object parseJson(String text) {
        try {
            int[] index = {0};
            Object value = parseJsonValue(text, index);
            skipWhitespace(text, index);
            return value;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Object parseJsonValue(String text, int[] index) {
        skipWhitespace(text, index);
        if (index[0] >= text.length()) {
            throw new IllegalArgumentException();
        }
        char c = text.charAt(index[0]);
        if (c == '{') {
            return parseJsonObject(text, index);
        }
        if (c == '[') {
            return parseJsonArray(text, index);
        }
        if (c == '"') {
            return parseJsonString(text, index);
        }
        if (text.startsWith("true", index[0])) {
            index[0] += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", index[0])) {
            index[0] += 5;
            return Boolean.FALSE;
        }
        if (text.startsWith("null", index[0])) {
            index[0] += 4;
            return null;
        }
        int start = index[0];
        while (index[0] < text.length() && ",]} \t\r\n".indexOf(text.charAt(index[0])) < 0) {
            index[0]++;
        }
        return text.substring(start, index[0]);
    }

    private static Map<String, Object> parseJsonObject(String text, int[] index) {
        Map<String, Object> result = new LinkedHashMap<>();
        index[0]++;
        while (true) {
            skipWhitespace(text, index);
            if (index[0] >= text.length()) {
                throw new IllegalArgumentException();
            }
            if (text.charAt(index[0]) == '}') {
                index[0]++;
                return result;
            }
            String key = parseJsonString(text, index);
            skipWhitespace(text, index);
            if (index[0] >= text.length() || text.charAt(index[0]) != ':') {
                throw new IllegalArgumentException();
            }
            index[0]++;
            result.put(key, parseJsonValue(text, index));
            skipWhitespace(text, index);
            if (index[0] >= text.length()) {
                throw new IllegalArgumentException();
            }
            char c = text.charAt(index[0]++);
            if (c == '}') {
                return result;
            }
            if (c != ',') {
                throw new IllegalArgumentException();
            }
        }
    }

    private static List<Object> parseJsonArray(String text, int[] index) {
        List<Object> result = new ArrayList<>();
        index[0]++;
        while (true) {
            skipWhitespace(text, index);
            if (index[0] >= text.length()) {
                throw new IllegalArgumentException();
            }
            if (text.charAt(index[0]) == ']') {
                index[0]++;
                return result;
            }
            result.add(parseJsonValue(text, index));
            skipWhitespace(text, index);
            if (index[0] >= text.length()) {
                throw new IllegalArgumentException();
            }
            char c = text.charAt(index[0]++);
            if (c == ']') {
                return result;
            }
            if (c != ',') {
                throw new IllegalArgumentException();
            }
        }
    }

    private static String parseJsonString(String text, int[] index) {
        if (text.charAt(index[0]) != '"') {
            throw new IllegalArgumentException();
        }
        index[0]++;
        StringBuilder builder = new StringBuilder();
        while (index[0] < text.length()) {
            char c = text.charAt(index[0]++);
            if (c == '"') {
                return builder.toString();
            }
            if (c != '\\') {
                builder.append(c);
                continue;
            }
            if (index[0] >= text.length()) {
                throw new IllegalArgumentException();
            }
            char escape = text.charAt(index[0]++);
            switch (escape) {
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                case '/' -> builder.append('/');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    if (index[0] + 4 > text.length()) {
                        throw new IllegalArgumentException();
                    }
                    builder.append((char) Integer.parseInt(text.substring(index[0], index[0] + 4), 16));
                    index[0] += 4;
                }
                default -> throw new IllegalArgumentException();
            }
        }
        throw new IllegalArgumentException();
    }

    private static void skipWhitespace(String text, int[] index) {
        while (index[0] < text.length() && Character.isWhitespace(text.charAt(index[0]))) {
            index[0]++;
        }
    }

    private static String stringValue(Object value, String fallback) {
        return value instanceof String string && !string.isBlank() ? string : fallback;
    }

    private static String stripExtension(String value) {
        int dot = value.lastIndexOf('.');
        return dot <= 0 ? value : value.substring(0, dot);
    }

    private static long modifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private static String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    record ModFile(
            String id,
            String name,
            String version,
            Path jarPath,
            Set<String> packagePrefixes,
            Set<String> mixinConfigs,
            Set<String> entrypoints,
            Map<String, List<String>> depends,
            Map<String, List<String>> breaks,
            Map<String, List<String>> conflicts,
            Map<String, String> providedVersions
    ) {
    }

    record CompatibilityProblem(
            String kind,
            ModFile primary,
            ModFile secondary,
            String dependencyId,
            List<String> predicates,
            String description,
            RecoveryAnalysis.Confidence confidence
    ) {
    }
}
