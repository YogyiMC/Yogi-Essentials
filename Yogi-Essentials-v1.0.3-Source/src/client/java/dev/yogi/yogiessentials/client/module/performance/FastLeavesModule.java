package dev.yogi.yogiessentials.client.module.performance;
import net.minecraft.class_315;
public final class FastLeavesModule extends OptionBackedPerformanceModule {
    private Boolean previous;
    public FastLeavesModule() { super("Fast Leaves", "Uses the cheaper leaf rendering path to reduce geometry and transparency cost in forests."); }
    protected void capturePrevious(class_315 o){ previous=o.method_75334().method_41753(); }
    protected void applyEnabledValue(class_315 o){ o.method_75334().method_41748(false); }
    protected void restorePrevious(class_315 o){ if(previous!=null)o.method_75334().method_41748(previous); }
    protected void clearPrevious(){ previous=null; }
}
