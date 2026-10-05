package dev.yogi.yogiessentials.client.module.performance;
import net.minecraft.class_315;
public final class MenuBlurOptimizerModule extends OptionBackedPerformanceModule {
    private Integer previous;
    public MenuBlurOptimizerModule() { super("No Menu Blur", "Removes background blur passes from menus to avoid unnecessary framebuffer work."); }
    protected void capturePrevious(class_315 o){ previous=o.method_57702().method_41753(); }
    protected void applyEnabledValue(class_315 o){ o.method_57702().method_41748(0); }
    protected void restorePrevious(class_315 o){ if(previous!=null)o.method_57702().method_41748(previous); }
    protected void clearPrevious(){ previous=null; }
}
