package dev.toma.gunsrpg.sided;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.toma.gunsrpg.api.common.data.IPlayerData;
import dev.toma.gunsrpg.client.ClientEventHandler;
import dev.toma.gunsrpg.client.ModKeybinds;
import dev.toma.gunsrpg.client.render.*;
import dev.toma.gunsrpg.client.render.debuff.DebuffRenderManager;
import dev.toma.gunsrpg.client.render.debuff.IconDebuffRenderer;
import dev.toma.gunsrpg.client.screen.*;
import dev.toma.gunsrpg.common.capability.PlayerData;
import dev.toma.gunsrpg.common.init.*;
import lib.toma.animations.AnimationEngine;
import lib.toma.animations.api.*;
import lib.toma.animations.api.lifecycle.Registries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.function.Function;

import static dev.toma.gunsrpg.client.animation.ModAnimations.*;

public class ClientSideManager {

    private static final ClientSideManager INSTANCE = new ClientSideManager();

    private final DebuffRenderManager debuffRenderManager = new DebuffRenderManager();

    public DebuffRenderManager getDebuffRenderManager() {
        return debuffRenderManager;
    }

    public static ClientSideManager instance() {
        return INSTANCE;
    }

    public void animationSetup() {
        Registries.ANIMATION_TYPES.addCallback(this::gatherAnimationTypes);
        Registries.ANIMATION_STAGES.addCallback(this::gatherAnimationStages);
    }

    public void clientSetup(FMLClientSetupEvent event) {

        // entity renderers
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.AIRDROP.get(),             AirdropRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.EXPLOSIVE_SKELETON.get(),  ExplosiveSkeletonRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.EXPLOSIVE_ARROW.get(),     ExplosiveArrowRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.ZOMBIE_GUNNER.get(),       ZombieGunnerRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.BLOODMOON_GOLEM.get(),     BloodmoonGolemRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.CROSSBOW_BOLT.get(),       CrossbowBoltRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.GRENADE.get(),             GrenadeRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.ROCKET_ANGEL.get(),        RocketAngelRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.GOLD_DRAGON.get(),         GoldenDragonRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.SHOTGUN_PELLET.get(),      NoOpRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.BULLET.get(),              NoOpRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.FLARE.get(),               NoOpRenderer::new);

        // keybinds
        ModKeybinds.registerKeybinds();
        MinecraftForge.EVENT_BUS.register(new ModKeybinds());

        // screens
        event.enqueueWork(this::screenSetup);

        // debuff renderers
        debuffRenderManager.registerRenderer(Debuffs.POISON,    new IconDebuffRenderer<>(IconDebuffRenderer.POISON_ICON));
        debuffRenderManager.registerRenderer(Debuffs.INFECTION, new IconDebuffRenderer<>(IconDebuffRenderer.INFECTION_ICON));
        debuffRenderManager.registerRenderer(Debuffs.FRACTURE,  new IconDebuffRenderer<>(IconDebuffRenderer.FRACTURE_ICON));
        debuffRenderManager.registerRenderer(Debuffs.BLEED,     new IconDebuffRenderer<>(IconDebuffRenderer.BLEED_ICON));

        // animation setup
        setupRenderPipeline();
    }

    public void playDelayedSound(BlockPos pos, float volume, float pitch, SoundEvent event, SoundCategory category, int tickDelay) {
        Minecraft mc = Minecraft.getInstance();
        SoundHandler handler = mc.getSoundManager();
        handler.playDelayed(new SimpleSound(event, category, volume, pitch, pos), tickDelay);
    }

    public IPlayerData.ISynchCallback onDataSync() {
        return () -> {
            Minecraft mc = Minecraft.getInstance();
            if (!PlayerData.getUnsafe(mc.player).getAimInfo().startedAiming()) {
                ClientEventHandler.preAimFov.ifPresent(value -> mc.options.fov = value);
                ClientEventHandler.preAimSens.ifPresent(value -> mc.options.sensitivity = value);
            }
        };
    }

    private void screenSetup() {
        ScreenManager.register(ModContainers.AIRDROP.get(), AirdropScreen::new);
        ScreenManager.register(ModContainers.BLAST_FURNACE.get(), BlastFurnaceScreen::new);
        ScreenManager.register(ModContainers.DEATH_CRATE.get(), DeathCrateScreen::new);
        ScreenManager.register(ModContainers.SMITHING_TABLE.get(), SmithingTableScreen::new);
        ScreenManager.register(ModContainers.LUNCH_BOX.get(), LunchBoxScreen::new);
        ScreenManager.register(ModContainers.AMMO_CASE.get(), AmmoCaseScreen::new);
        ScreenManager.register(ModContainers.GRENADE_CASE.get(), GrenadeCaseScreen::new);
        ScreenManager.register(ModContainers.MEDS_CASE.get(), MedsCaseScreen::new);
        ScreenManager.register(ModContainers.ITEM_CASE.get(), ItemCaseScreen::new);
    }

    private void setupRenderPipeline() {
        IRenderPipeline pipeline = AnimationEngine.get().renderPipeline();
        pipeline.setPostAnimateCallback(this::animateDualWield);
    }

    private void animateDualWield(MatrixStack poseStack, IRenderTypeBuffer buffer, int light, float swing, float equip, Function<HandSide, IRenderConfig> selector,
                                  IAnimationPipeline pipeline, FirstPersonRenderer fpRenderer, PlayerEntity player, ItemStack stack, ItemCameraTransforms.TransformType type,
                                  boolean mainHand) {

        IRenderPipeline renderPipeline = AnimationEngine.get().renderPipeline();
        IItemRenderer itemRenderer = renderPipeline.getItemRenderer();
        if (stack.getItem() == ModItems.M1911 && PlayerData.hasActiveSkill(player, Skills.M1911_DUAL_WIELD)) {
            poseStack.pushPose();
            {
                pipeline.animateStage(DUAL_WIELD_ITEM, poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
                itemRenderer.renderItem(fpRenderer, player, stack, ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, true, poseStack, buffer, light, swing, equip);
            }
            poseStack.popPose();
        }
    }

    private AnimationType<?>[] gatherAnimationTypes() {
        return new AnimationType[] {
                AIM_ANIMATION, SPRINT, CHAMBER, HEAL, RELOAD, RELOAD_BULLET, FIREMODE, BULLET_EJECTION, RECOIL
        };
    }

    private AnimationStage[] gatherAnimationStages() {
        return new AnimationStage[] {
                DUAL_WIELD_ITEM, MAGAZINE, SLIDE, CHARGING_HANDLE, BULLET, BOLT, BOLT_CARRIER, BARRELS, BULLET_2
        };
    }
}
