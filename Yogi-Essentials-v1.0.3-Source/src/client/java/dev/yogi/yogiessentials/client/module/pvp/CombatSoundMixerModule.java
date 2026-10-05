package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.SectionSetting;
import net.minecraft.class_1113;
import net.minecraft.class_2960;
import net.minecraft.class_310;

public final class CombatSoundMixerModule extends Module {
    private final NumberSetting master = volume("All Vanilla Sounds");
    private final NumberSetting explosions = volume("Other Explosions (TNT/Creepers)");
    private final NumberSetting blockBreak = volume("Block Breaking");
    private final NumberSetting blockPlace = volume("Block Placing");
    private final NumberSetting crystalExplosion = volume("Crystal Explosions");
    private final NumberSetting anchorCharge = volume("Anchor Charging");
    private final NumberSetting anchorExplosion = volume("Anchor Explosions");
    private final NumberSetting anchorOther = volume("Anchor Ambient/Spawn");
    private final NumberSetting mace = volume("Mace Impacts");
    private final NumberSetting wind = volume("Wind Charges");
    private final NumberSetting critical = volume("Critical Hits");
    private final NumberSetting attacks = volume("Attacks and Sweeps");
    private final NumberSetting damage = volume("Player Damage");
    private final NumberSetting shield = volume("Shield Blocking and Breaking");
    private final NumberSetting totem = volume("Totem Activations");
    private final NumberSetting itemBreak = volume("Item Breaking");
    private final NumberSetting pearl = volume("Ender Pearls and Teleports");
    private final NumberSetting potions = volume("Potions and Drinking");
    private final NumberSetting bow = volume("Bows and Crossbows");
    private final NumberSetting trident = volume("Tridents and Spears");
    private final NumberSetting projectiles = volume("Projectile Impacts");
    private final NumberSetting footsteps = volume("Footsteps");
    private final NumberSetting weather = volume("Rain and Thunder");
    private final NumberSetting other = volume("Other Vanilla Sounds");

    public CombatSoundMixerModule() {
        super("Combat Sound Mixer",
                "Adjust combat sounds. Shared explosion sounds are classified by nearby crystal and anchor activity.",
                Category.PVP);
        addSetting(new SectionSetting("0% mutes; 100% keeps vanilla volume."));
        addSetting(master);
        addSetting(new SectionSetting("Combat"));
        addSetting(explosions);
        addSetting(crystalExplosion);
        addSetting(anchorCharge);
        addSetting(anchorExplosion);
        addSetting(anchorOther);
        addSetting(mace);
        addSetting(wind);
        addSetting(critical);
        addSetting(attacks);
        addSetting(damage);
        addSetting(shield);
        addSetting(totem);
        addSetting(itemBreak);
        addSetting(pearl);
        addSetting(potions);
        addSetting(bow);
        addSetting(trident);
        addSetting(projectiles);
        addSetting(new SectionSetting("World and building"));
        addSetting(blockBreak);
        addSetting(blockPlace);
        addSetting(footsteps);
        addSetting(weather);
        addSetting(other);
    }

    private static NumberSetting volume(String name) {
        return new NumberSetting(name + " Volume (%)", 100, 0, 100, 5);
    }

    public float volume(class_1113 sound) {
        if (!isEnabled() || sound == null) return 1.0F;
        class_2960 id = sound.method_4775();
        if (id == null) return 1.0F;
        if (!"minecraft".equals(id.method_12836())) return 1.0F;
        String path = id.method_12832();
        NumberSetting group;
        if (path.contains("explode") || path.contains("explosion")) {
            class_310 client = class_310.method_1551();
            CombatSoundContext.Source source = CombatSoundContext.explosionAt(client.field_1687,
                    sound.method_4784(), sound.method_4779(), sound.method_4778());
            group = source == CombatSoundContext.Source.CRYSTAL ? crystalExplosion
                    : source == CombatSoundContext.Source.ANCHOR ? anchorExplosion : explosions;
        }
        else if (path.equals("block.respawn_anchor.charge")) group = anchorCharge;
        else if (path.startsWith("block.respawn_anchor.")) group = anchorOther;
        else if (path.contains("mace.")) group = mace;
        else if (path.contains("wind_charge") || path.contains("wind_burst")) group = wind;
        else if (path.contains("player.attack.crit")) group = critical;
        else if (path.contains("player.attack.") || path.contains("player.sweep")) group = attacks;
        else if (path.contains("shield.")) group = shield;
        else if (path.contains("totem.")) group = totem;
        else if (path.contains("item.break")) group = itemBreak;
        else if (path.contains("ender_pearl") || path.contains("enderman.teleport")) group = pearl;
        else if (path.contains("potion") || path.contains("bottle.drink")
                || path.contains("generic.drink")) group = potions;
        else if (path.contains("crossbow") || path.contains("bow.") || path.equals("entity.arrow.shoot")) group = bow;
        else if (path.contains("trident") || path.contains("spear.")) group = trident;
        else if (path.contains("arrow.hit") || path.contains("snowball.throw")
                || path.contains("egg.throw")) group = projectiles;
        else if (path.contains("player.hurt") || path.contains("player.death")) group = damage;
        else if (path.startsWith("block.") && path.endsWith(".break")) group = blockBreak;
        else if (path.startsWith("block.") && path.endsWith(".place")) group = blockPlace;
        else if (path.endsWith(".step") || path.contains("player.step")) group = footsteps;
        else if (path.contains("weather.rain") || path.contains("weather.thunder")
                || path.contains("lightning_bolt.thunder") || path.contains("lightning_bolt.impact")) group = weather;
        else group = other;
        return (float) (master.get() * group.get() / 10000.0);
    }
}
