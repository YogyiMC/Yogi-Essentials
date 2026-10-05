package dev.yogi.yogiessentials.client.module.performance;
import net.minecraft.class_12393;
import net.minecraft.class_315;
public final class TextureFilteringOptimizerModule extends OptionBackedPerformanceModule {
    private class_12393 previous;
    public TextureFilteringOptimizerModule() { super("Fast Texture Filtering", "Uses the lowest-overhead optional texture filtering mode to reduce sampling cost."); }
    protected void capturePrevious(class_315 o){ previous=o.method_76747().method_41753(); }
    protected void applyEnabledValue(class_315 o){ o.method_76747().method_41748(class_12393.field_64663); }
    protected void restorePrevious(class_315 o){ if(previous!=null)o.method_76747().method_41748(previous); }
    protected void clearPrevious(){ previous=null; }
}
