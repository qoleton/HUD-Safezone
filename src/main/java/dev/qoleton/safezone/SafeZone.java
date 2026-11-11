package dev.qoleton.safezone;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = SafeZone.MODID, dist = Dist.CLIENT)
public final class SafeZone {
    public static final String MODID = "safezone";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SafeZone(IEventBus modBus, ModContainer container) {
        SafeZoneConfig.load();
        NeoForge.EVENT_BUS.register(SafeZoneEvents.class);
        // "Config" button in the Mods list opens the editor too.
        container.registerExtensionPoint(IConfigScreenFactory.class, new IConfigScreenFactory() {
            @Override
            public Screen createScreen(ModContainer modContainer, Screen parent) {
                return new SafeZoneScreen(parent);
            }
        });
    }
}
