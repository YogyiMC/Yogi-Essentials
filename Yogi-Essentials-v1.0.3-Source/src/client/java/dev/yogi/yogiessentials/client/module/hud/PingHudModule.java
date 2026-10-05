package dev.yogi.yogiessentials.client.module.hud;

import java.util.UUID;
import net.minecraft.class_310;
import net.minecraft.class_640;

public class PingHudModule extends StyledHudModule {

    private int lastValidPing;
    private boolean hasValidPing;
    private UUID lastPlayerUuid;

    public PingHudModule() {
        super(
                "Ping",
                "Shows the latest valid server latency without flashing to zero during world transfers.",
                0.01,
                0.06
        );
    }

    public int resolvePing(class_310 client) {
        if (client == null || client.method_1562() == null) {
            return hasValidPing ? lastValidPing : 0;
        }

        if (client.field_1724 != null) {
            lastPlayerUuid = client.field_1724.method_5667();
        }

        if (lastPlayerUuid == null) {
            return hasValidPing ? lastValidPing : 0;
        }

        class_640 entry = client.method_1562().method_2871(lastPlayerUuid);
        if (entry == null) {
            return hasValidPing ? lastValidPing : 0;
        }

        int latency = Math.max(0, entry.method_2959());

        
        
        
        if (latency > 0 || !hasValidPing) {
            lastValidPing = latency;
            hasValidPing = true;
        }

        return hasValidPing ? lastValidPing : latency;
    }
    public void clearTransientState() {
        clearTransientState(false);
    }

    public void clearTransientState(
            boolean preserveConnectionPing
    ) {
        lastPlayerUuid = null;

        if (!preserveConnectionPing) {
            lastValidPing = 0;
            hasValidPing = false;
        }
    }

}
