package dev.yogi.yogiessentials.client.module.performance;
import net.minecraft.class_315;
public final class WeatherRenderBudgetModule extends OptionBackedPerformanceModule {
    private Integer previous;
    public WeatherRenderBudgetModule() { super("Weather Render Budget", "Limits expensive rain/snow rendering radius to reduce overdraw and particle-like weather work."); }
    protected void capturePrevious(class_315 o){ previous=o.method_75333().method_41753(); }
    protected void applyEnabledValue(class_315 o){ o.method_75333().method_41748(5); }
    protected void restorePrevious(class_315 o){ if(previous!=null)o.method_75333().method_41748(previous); }
    protected void clearPrevious(){ previous=null; }
}
