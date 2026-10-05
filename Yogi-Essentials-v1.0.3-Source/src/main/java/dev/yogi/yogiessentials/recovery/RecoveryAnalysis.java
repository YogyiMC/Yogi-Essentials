package dev.yogi.yogiessentials.recovery;

import java.nio.file.Path;
import java.util.List;

public record RecoveryAnalysis(
        String title,
        String summary,
        String details,
        Confidence confidence,
        Path sourcePath,
        List<Suspect> suspects,
        List<Issue> issues,
        List<FixAction> fixes,
        List<String> suggestions,
        boolean testMode
) {
    public enum Confidence {
        HIGH,
        MEDIUM,
        LOW,
        NONE
    }

    public enum IssueType {
        MIXIN,
        DEPENDENCY,
        INCOMPATIBILITY,
        DUPLICATE_MOD,
        CONFIG,
        DIRECT_EXCEPTION,
        LINKAGE,
        NATIVE,
        UNKNOWN
    }

    public enum FixType {
        DISABLE_MOD,
        DISABLE_DUPLICATE,
        RESET_CONFIG
    }

    public record Suspect(
            String id,
            String name,
            String version,
            Path jarPath,
            Confidence confidence,
            int score,
            String reason
    ) {
        public String displayName() {
            if (name == null || name.isBlank()) {
                return id == null || id.isBlank() ? "Unknown mod" : id;
            }
            return name;
        }
    }

    public record Issue(
            IssueType type,
            String title,
            String description,
            Confidence confidence,
            String modId,
            Path targetPath
    ) {
    }

    public record FixAction(
            FixType type,
            String label,
            String description,
            Confidence confidence,
            String modId,
            Path targetPath,
            boolean autoSelected,
            String choiceGroup
    ) {
        public FixAction(
                FixType type,
                String label,
                String description,
                Confidence confidence,
                String modId,
                Path targetPath,
                boolean autoSelected
        ) {
            this(type, label, description, confidence, modId, targetPath, autoSelected, "");
        }
    }
}
