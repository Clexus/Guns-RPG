package dev.toma.gunsrpg;

import com.mojang.brigadier.CommandDispatcher;
import dev.toma.gunsrpg.api.common.data.IPlayerData;
import dev.toma.gunsrpg.api.common.data.IWorldData;
import dev.toma.gunsrpg.client.screen.skills.SkillTreePlacement;
import dev.toma.gunsrpg.common.capability.PlayerData;
import dev.toma.gunsrpg.common.capability.PlayerDataStorage;
import dev.toma.gunsrpg.common.command.GunsrpgCommand;
import dev.toma.gunsrpg.common.init.*;
import dev.toma.gunsrpg.config.ModConfig;
import dev.toma.gunsrpg.network.NetworkManager;
import dev.toma.gunsrpg.sided.ClientSideManager;
import dev.toma.gunsrpg.util.Lifecycle;
import dev.toma.gunsrpg.world.MobSpawnManager;
import dev.toma.gunsrpg.world.cap.WorldData;
import dev.toma.gunsrpg.world.cap.WorldDataStorage;
import lib.toma.animations.AnimationEngine;
import lib.toma.animations.api.IRenderPipeline;
import net.minecraft.command.CommandSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GunsRPG.MODID)
public class GunsRPG {

    public static final String MODID = "gunsrpg";
    public static Logger log = LogManager.getLogger();
    private static final Lifecycle modLifecycle = new Lifecycle();

    public GunsRPG() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // deferred registries
        ModEntities.subscribe(eventBus);
        ModEffects.subscribe(eventBus);
        ModBlockEntities.subscribe(eventBus);
        ModContainers.subscribe(eventBus);
        ModFeatures.subscribe(eventBus);
        ModFeaturePlacements.subscribe(eventBus);
        ModRecipeSerializers.subscribe(eventBus);
        // lifecycle events
        eventBus.addListener(this::clientSetup);
        eventBus.addListener(this::commonSetup);
        // other events
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

        ThreadGroup group = Thread.currentThread().getThreadGroup();
        if (group.getName().equals("main")) {
            ClientSideManager.instance().animationSetup();
            IRenderPipeline renderPipeline = AnimationEngine.get().renderPipeline();
            renderPipeline.register(MinecraftForge.EVENT_BUS);
            AnimationEngine.get().startEngine(ModConfig.clientConfig.developerMode.get());
        }

        modLifecycle.modInit();

        // TODO
        // GameRules.GAME_RULE_TYPES.put(GameRules.RULE_NATURAL_REGENERATION, GameRules.BooleanValue.create(false));
    }

    public static Lifecycle getModLifecycle() {
        return modLifecycle;
    }

    public static ResourceLocation makeResource(String path) {
        return new ResourceLocation(MODID, path);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        ClientSideManager.instance().clientSetup(event);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        NetworkManager.init();
        CapabilityManager.INSTANCE.register(IPlayerData.class, new PlayerDataStorage(), PlayerData::new);
        CapabilityManager.INSTANCE.register(IWorldData.class, new WorldDataStorage(), WorldData::new);
        SkillTreePlacement.generatePlacement();
        modLifecycle.commonInit();
        MobSpawnManager.instance().initialize();
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        GunsrpgCommand.register(dispatcher);
    }
}
