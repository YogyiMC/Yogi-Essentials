package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.chat.ChatHistoryLimitModule;
import dev.yogi.yogiessentials.client.module.chat.ChatPingsModule;
import dev.yogi.yogiessentials.client.module.chat.ClickToCopyModule;
import dev.yogi.yogiessentials.client.module.chat.MessageHighlightingModule;
import dev.yogi.yogiessentials.client.module.chat.RepeatedMessageStackingModule;
import dev.yogi.yogiessentials.client.module.performance.AsyncChatIndexModule;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_2558;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3419;
import net.minecraft.class_5250;

public final class ChatEnhancementManager {
    private static final int ABSOLUTE_MAX_HISTORY = 1000;
    private static final Deque<String> RECENT_MESSAGES = new ArrayDeque<>();
    private static final ConcurrentLinkedDeque<IndexedMessage> SEARCH_INDEX = new ConcurrentLinkedDeque<>();
    private static final ExecutorService CHAT_INDEX_WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Yogi Essentials Chat Index");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private static String lastPlainMessage = null;
    private static int repeatCount = 1;

    private ChatEnhancementManager() {
    }

    public static void initialize() {
        RECENT_MESSAGES.clear();
        SEARCH_INDEX.clear();
        lastPlainMessage = null;
        repeatCount = 1;
    }

    public static ProcessedMessage process(class_2561 original, boolean playerChat) {
        if (original == null) {
            return new ProcessedMessage(class_2561.method_43473(), false);
        }

        String raw = original.getString();
        remember(raw);
        maybePlayPing(raw, playerChat);

        boolean replacePrevious = false;
        RepeatedMessageStackingModule stacking = module(RepeatedMessageStackingModule.class);
        if (stacking != null && stacking.isEnabled() && raw.equals(lastPlainMessage)) {
            repeatCount++;
            replacePrevious = true;
        } else {
            lastPlainMessage = raw;
            repeatCount = 1;
        }

        class_5250 result = class_2561.method_43473();

        MessageHighlightingModule highlighting = module(MessageHighlightingModule.class);
        boolean highlighted = highlighting != null
                && highlighting.isEnabled()
                && shouldHighlight(raw, highlighting);

        if (highlighted) {
            int rgb = highlighting.getColor().getRgb();
            result.method_10852(class_2561.method_43470("▌ ").method_27694(style -> style.method_36139(rgb)));
            result.method_10852(original.method_27661().method_27694(style -> style.method_36139(rgb)));
        } else {
            result.method_10852(original.method_27661());
        }

        if (repeatCount > 1) {
            result.method_10852(class_2561.method_43470("  ×" + repeatCount)
                    .method_27694(style -> style.method_36139(0xFF9A5A)));
        }

        ClickToCopyModule clickToCopy = module(ClickToCopyModule.class);
        if (clickToCopy != null && clickToCopy.isEnabled()) {
            result.method_27694(style -> style.method_10958(new class_2558.class_10606(raw)));
        }

        return new ProcessedMessage(result, replacePrevious);
    }

    public static int configuredHistoryLimit() {
        ChatHistoryLimitModule module = module(ChatHistoryLimitModule.class);
        if (module == null || !module.isEnabled()) {
            return 100;
        }
        return Math.max(25, Math.min(ABSOLUTE_MAX_HISTORY, module.getLimit().get().intValue()));
    }

    public static List<String> search(String query, int maxResults, boolean caseSensitive) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String needle = caseSensitive ? query : query.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();

        AsyncChatIndexModule asyncIndex = module(AsyncChatIndexModule.class);
        if (!caseSensitive && asyncIndex != null && asyncIndex.backgroundIndexingEnabled() && !SEARCH_INDEX.isEmpty()) {
            for (var iterator = SEARCH_INDEX.descendingIterator(); iterator.hasNext();) {
                IndexedMessage indexed = iterator.next();
                if (indexed.normalized().contains(needle)) {
                    matches.add(indexed.raw());
                    if (matches.size() >= Math.max(1, maxResults)) break;
                }
            }
            if (!matches.isEmpty()) return matches;
        }

        List<String> snapshot = new ArrayList<>(RECENT_MESSAGES);
        Collections.reverse(snapshot);
        for (String message : snapshot) {
            String haystack = caseSensitive ? message : message.toLowerCase(Locale.ROOT);
            if (haystack.contains(needle)) {
                matches.add(message);
                if (matches.size() >= Math.max(1, maxResults)) break;
            }
        }
        return matches;
    }

    public static void resetRepeatState() {
        lastPlainMessage = null;
        repeatCount = 1;
    }

    private static boolean shouldHighlight(String raw, MessageHighlightingModule module) {
        String lower = raw.toLowerCase(Locale.ROOT);
        class_310 client = class_310.method_1551();

        if (module.getMentions().get() && client != null && client.field_1724 != null) {
            String username = client.method_1548().method_1676();
            if (username != null && !username.isBlank()
                    && lower.contains(username.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        String keywords = module.getKeywords().get();
        if (keywords == null || keywords.isBlank()) {
            return false;
        }
        for (String keyword : keywords.split(",")) {
            String cleaned = keyword.trim();
            if (!cleaned.isEmpty() && lower.contains(cleaned.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static void remember(String raw) {
        RECENT_MESSAGES.addLast(raw);
        int limit = configuredHistoryLimit();
        while (RECENT_MESSAGES.size() > limit) {
            RECENT_MESSAGES.removeFirst();
        }

        AsyncChatIndexModule asyncIndex = module(AsyncChatIndexModule.class);
        if (asyncIndex != null && asyncIndex.backgroundIndexingEnabled()) {
            final String immutable = raw;
            final int capturedLimit = limit;
            CHAT_INDEX_WORKER.execute(() -> {
                SEARCH_INDEX.addLast(new IndexedMessage(immutable, immutable.toLowerCase(Locale.ROOT)));
                while (SEARCH_INDEX.size() > capturedLimit) {
                    SEARCH_INDEX.pollFirst();
                }
            });
        } else {
            SEARCH_INDEX.clear();
        }
    }

    private static void maybePlayPing(String raw, boolean playerChat) {
        ChatPingsModule module = module(ChatPingsModule.class);
        if (module == null || !module.isEnabled()) {
            return;
        }
        if (!playerChat && !module.getIncludeSystemMessages().get()) {
            return;
        }

        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null || client.method_1483() == null) {
            return;
        }

        String username = client.method_1548().method_1676();
        if (username == null || username.isBlank()
                || !raw.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            return;
        }

        playPingSound(module);
    }

    public static void previewPingSound(ChatPingsModule module) {
        if (module == null) {
            return;
        }
        playPingSound(module);
    }

    private static void playPingSound(ChatPingsModule module) {
        class_310 client = class_310.method_1551();
        if (client == null || client.method_1483() == null) {
            return;
        }

        class_3414 event;
        float pitch;
        switch (module.getSound().get()) {
            case LEVEL_UP -> {
                event = class_3417.field_14709;
                pitch = 1.0F;
            }
            case AMETHYST -> {
                event = class_3417.field_43154;
                pitch = 1.15F;
            }
            case BELL -> {
                event = class_3417.field_17265;
                pitch = 1.0F;
            }
            case EXPERIENCE -> {
                event = class_3417.field_14627;
                pitch = 1.15F;
            }
            default -> {
                return;
            }
        }

        float volume = Math.max(0.0F, Math.min(1.0F, module.getVolume().get().floatValue() / 100.0F));
        if (volume <= 0.0F) {
            return;
        }

        class_1109 sound = new class_1109(
                event.comp_3319(),
                class_3419.field_15250,
                volume,
                pitch,
                class_1113.method_43221(),
                false,
                0,
                class_1113.class_1114.field_5478,
                0.0,
                0.0,
                0.0,
                true
        );
        client.method_1483().method_4873(sound);
    }

    private static <T extends dev.yogi.yogiessentials.client.module.Module> T module(Class<T> type) {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return null;
        }
        return YogiEssentialsClient.getModuleManager().getModule(type);
    }

    private record IndexedMessage(String raw, String normalized) {
    }

    public record ProcessedMessage(class_2561 text, boolean replacePrevious) {
    }
}
