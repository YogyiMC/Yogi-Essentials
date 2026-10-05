package dev.yogi.yogiessentials.client.module.performance;
import net.minecraft.class_315;
public final class AnisotropicSamplingOptimizerModule extends OptionBackedPerformanceModule {
    private Integer previous;
    public AnisotropicSamplingOptimizerModule() { super("Low Anisotropic Sampling", "Reduces optional anisotropic texture sampling work at long view distances."); }
    protected void capturePrevious(class_315 o){ previous=o.method_76247().method_41753(); }
    protected void applyEnabledValue(class_315 o){ o.method_76247().method_41748(1); }
    protected void restorePrevious(class_315 o){ if(previous!=null)o.method_76247().method_41748(previous); }
    protected void clearPrevious(){ previous=null; }
}
