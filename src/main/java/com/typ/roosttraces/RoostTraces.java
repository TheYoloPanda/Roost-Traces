package com.typ.roosttraces;

import com.mojang.logging.LogUtils;
import com.typ.roosttraces.compat.TraceHostDataMap;
import com.typ.roosttraces.placement.RoostBackfillScanner;
import com.typ.roosttraces.placement.RoostTracePlacementScheduler;
import com.typ.roosttraces.pool.RoostTraceNodePoolResolver;
import org.slf4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RoostTraces.MODID)
public class RoostTraces {
    public static final String MODID = "roosttraces";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RoostTraces(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, RoostTracesConfig.SPEC);
        modEventBus.addListener(RoostTracesConfig::onLoad);
        modEventBus.addListener(TraceHostDataMap::register);

        NeoForge.EVENT_BUS.addListener(RoostTraceNodePoolResolver::onAddReloadListener);
        NeoForge.EVENT_BUS.register(RoostTracePlacementScheduler.class);
        NeoForge.EVENT_BUS.register(RoostBackfillScanner.class);
    }
}
