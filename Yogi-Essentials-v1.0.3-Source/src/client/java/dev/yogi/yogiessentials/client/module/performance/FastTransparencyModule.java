package dev.yogi.yogiessentials.client.module.performance;
import net.minecraft.class_315;
public final class FastTransparencyModule extends OptionBackedPerformanceModule {
    private Boolean previous;
    public FastTransparencyModule() { super("Fast Transparency", "Disables Minecraft's more expensive improved-transparency path for lower GPU and sorting overhead."); }
    protected void capturePrevious(class_315 o){ previous=o.method_75337().method_41753(); }
    protected void applyEnabledValue(class_315 o){ o.method_75337().method_41748(false); }
    protected void restorePrevious(class_315 o){ if(previous!=null)o.method_75337().method_41748(previous); }
    protected void clearPrevious(){ previous=null; }
}
