package dev.toma.gunsrpg.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.toma.gunsrpg.GunsRPG;
import dev.toma.gunsrpg.client.animation.Animations;
import dev.toma.gunsrpg.client.animation.impl.SprintingAnimation;
import dev.toma.gunsrpg.common.capability.IPlayerData;
import dev.toma.gunsrpg.common.capability.PlayerData;
import dev.toma.gunsrpg.common.capability.object.AimInfo;
import dev.toma.gunsrpg.common.capability.object.DebuffData;
import dev.toma.gunsrpg.common.capability.object.GunData;
import dev.toma.gunsrpg.common.capability.object.PlayerSkills;
import dev.toma.gunsrpg.common.debuffs.Debuff;
import dev.toma.gunsrpg.common.init.ModItems;
import dev.toma.gunsrpg.common.init.Skills;
import dev.toma.gunsrpg.common.item.guns.GunItem;
import dev.toma.gunsrpg.common.item.guns.ammo.AmmoItem;
import dev.toma.gunsrpg.common.item.guns.ammo.IAmmoProvider;
import dev.toma.gunsrpg.common.item.guns.util.Firemode;
import dev.toma.gunsrpg.common.skills.core.ISkill;
import dev.toma.gunsrpg.common.skills.core.SkillCategory;
import dev.toma.gunsrpg.common.skills.interfaces.IOverlayRender;
import dev.toma.gunsrpg.config.ModConfig;
import dev.toma.gunsrpg.config.util.ScopeRenderer;
import dev.toma.gunsrpg.network.NetworkManager;
import dev.toma.gunsrpg.network.packet.SPacketSetAiming;
import dev.toma.gunsrpg.network.packet.SPacketShoot;
import dev.toma.gunsrpg.sided.ClientSideManager;
import dev.toma.gunsrpg.util.ModUtils;
import dev.toma.gunsrpg.util.SkillUtil;
import dev.toma.gunsrpg.util.object.OptionalObject;
import dev.toma.gunsrpg.util.object.ShootingManager;
import lib.toma.animations.*;
import lib.toma.animations.pipeline.AnimationStage;
import lib.toma.animations.pipeline.IAnimationPipeline;
import net.minecraft.client.GameSettings;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunsRPG.MODID)
public class ClientEventHandler {

    public static final ResourceLocation SCOPE = GunsRPG.makeResource("textures/icons/scope_overlay.png");
    public static final ResourceLocation SCOPE_OVERLAY = GunsRPG.makeResource("textures/icons/scope_full.png");
    private static final ChangeDetector startSprintListener = new ChangeDetector(() -> {
        PlayerEntity player = Minecraft.getInstance().player;
        ItemStack stack = player.getMainHandItem();
        LazyOptional<IPlayerData> optional = PlayerData.get(player);
        optional.ifPresent(data -> {
            if (stack.getItem() instanceof GunItem && !data.getReloadInfo().isReloading())
                ClientSideManager.instance().processor().play(Animations.SPRINT, new SprintingAnimation());
        });
    }, PlayerEntity::isSprinting);
    public static OptionalObject<Double> preAimFov = OptionalObject.empty();
    public static OptionalObject<Double> preAimSens = OptionalObject.empty();
    public static int shootDelay;
    static boolean burst;
    static int shotsLeft = 2;

    @SubscribeEvent
    public static void cancelOverlays(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            Minecraft mc = Minecraft.getInstance();
            PlayerEntity player = mc.player;
            MainWindow window = event.getWindow();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem) {
                event.setCanceled(true);
                MatrixStack matrixStack = event.getMatrixStack();
                PlayerData.get(player).ifPresent(data -> {
                    int windowWidth = window.getGuiScaledWidth();
                    int windowHeight = window.getGuiScaledHeight();
                    if (data.getAimInfo().progress >= 0.9F) {
                        if (stack.getItem() == ModItems.SNIPER_RIFLE && PlayerData.hasActiveSkill(player, Skills.SR_SCOPE) || stack.getItem() == ModItems.CROSSBOW && PlayerData.hasActiveSkill(player, Skills.CROSSBOW_SCOPE)) {
                            Matrix4f pose = matrixStack.last().pose();
                            if (ModConfig.clientConfig.scopeRenderer.get() == ScopeRenderer.TEXTURE) {
                                ModUtils.renderTexture(pose, 0, 0, windowWidth, windowHeight, SCOPE_OVERLAY);
                            } else {
                                int left = window.getGuiScaledWidth() / 2 - 16;
                                int top = window.getGuiScaledHeight() / 2 - 16;
                                ModUtils.renderTexture(pose, left, top, left + 32, top + 32, SCOPE);
                            }
                        } else if ((PlayerData.hasActiveSkill(player, Skills.SMG_RED_DOT) && stack.getItem() == ModItems.SMG) || (PlayerData.hasActiveSkill(player, Skills.AR_RED_DOT) && stack.getItem() == ModItems.ASSAULT_RIFLE)) {
                            float left = windowWidth / 2f - 8f;
                            float top = windowHeight / 2f - 8f;
                            float x2 = left + 16;
                            float y2 = top + 16;
                            //draw red dot
                            int color = ModConfig.clientConfig.reticleColor.getColor();
                            float alpha = ModUtils.alpha(color);
                            float red = ModUtils.red(color);
                            float green = ModUtils.green(color);
                            float blue = ModUtils.blue(color);
                            mc.getTextureManager().bind(ModConfig.clientConfig.reticleVariants.getAsResource());
                            RenderSystem.enableBlend();
                            Tessellator tessellator = Tessellator.getInstance();
                            BufferBuilder builder = tessellator.getBuilder();
                            builder.begin(7, DefaultVertexFormats.POSITION_COLOR_TEX);
                            builder.vertex(left, y2, 0).color(red, green, blue, alpha).uv(0.0F, 1.0F).endVertex();
                            builder.vertex(x2, y2, 0).color(red, green, blue, alpha).uv(1.0F, 1.0F).endVertex();
                            builder.vertex(x2, top, 0).color(red, green, blue, alpha).uv(1.0F, 0.0F).endVertex();
                            builder.vertex(left, top, 0).color(red, green, blue, alpha).uv(0.0F, 0.0F).endVertex();
                            tessellator.end();
                            RenderSystem.disableBlend();
                        }
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            Minecraft mc = Minecraft.getInstance();
            PlayerEntity player = mc.player;
            MatrixStack matrixStack = event.getMatrixStack();
            LazyOptional<IPlayerData> optional = PlayerData.get(player);
            optional.ifPresent(data -> {
                FontRenderer renderer = mc.font;
                long day = player.level.getGameTime() / 24000L;
                int cycle = ModConfig.worldConfig.bloodmoonCycle.get();
                MainWindow window = event.getWindow();
                if (cycle >= 0) {
                    boolean b = day % cycle == 0 && day > 0;
                    long l = b ? 0 : cycle - day % cycle;
                    String remainingDays = l + "";
                    mc.font.draw(matrixStack, remainingDays, window.getGuiScaledWidth() - 10 - mc.font.width(remainingDays) / 2f, 6, b ? 0xff0000 : l > 0 && l < 3 ? 0xffff00 : 0xffffff);
                }
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                ItemStack stack = player.getMainHandItem();
                int width = 26;
                int x = window.getGuiScaledWidth() - width - 34;
                int y = window.getGuiScaledHeight() - 22;
                PlayerSkills skills = data.getSkills();
                if (stack.getItem() instanceof GunItem) {
                    GunItem gun = (GunItem) stack.getItem();
                    GunData gunData = skills.getGunData((GunItem) stack.getItem());
                    int gunKills = gunData.getKills();
                    int gunRequiredKills = gunData.getRequiredKills();
                    int ammo = gun.getAmmo(stack);
                    int max = gun.getMaxAmmo(player);
                    float f = gunData.isAtMaxLevel() ? 1.0F : gunKills / (float) gunRequiredKills;
                    AmmoItem itemAmmo = AmmoItem.getAmmoFor(gun, stack);
                    if (itemAmmo != null) {
                        int c = 0;
                        for (int i = 0; i < player.inventory.getContainerSize(); i++) {
                            ItemStack itemStack = player.inventory.getItem(i);
                            if (itemStack.getItem() instanceof IAmmoProvider) {
                                IAmmoProvider ammoProvider = (IAmmoProvider) itemStack.getItem();
                                if (ammoProvider.getMaterial() == itemAmmo.getMaterial() && ammoProvider.getAmmoType() == itemAmmo.getAmmoType()) {
                                    c += itemStack.getCount();
                                }
                            }
                        }
                        String text = ammo + " / " + c;
                        width = renderer.width(text);
                        x = window.getGuiScaledWidth() - width - 34;
                        Matrix4f pose = matrixStack.last().pose();
                        ModUtils.renderColor(pose, x, y, x + width + 22, y + 7, 0.0F, 0.0F, 0.0F, 1.0F);
                        ModUtils.renderColor(pose, x + 2, y + 2, x + (int) (f * (width + 20)), y + 5, 1.0F, 1.0F, 0.0F, 1.0F);
                        mc.getItemRenderer().renderGuiItem(new ItemStack(itemAmmo), x, y - 18);
                        mc.font.draw(matrixStack, text, x + 19, y - 14, 0xffffff);
                    }
                }
                int kills = skills.getKills();
                int required = skills.getRequiredKills();
                float levelProgress = skills.isMaxLevel() ? 1.0F : kills / (float) required;
                Matrix4f pose = matrixStack.last().pose();
                ModUtils.renderColor(pose, x, y + 10, x + width + 22, y + 17, 0.0F, 0.0F, 0.0F, 1.0F);
                ModUtils.renderColor(pose, x + 2, y + 12, x + (int) (levelProgress * (width + 20)), y + 15, 0.0F, 1.0F, 1.0F, 1.0F);
                if (data != null) {
                    DebuffData debuffData = data.getDebuffData();
                    int offset = 0;
                    for (Debuff debuff : debuffData.getDebuffs()) {
                        if (debuff == null) continue;
                        int yStart = window.getGuiScaledHeight() + ModConfig.clientConfig.debuffOverlay.getY() - 50;
                        debuff.draw(matrixStack, ModConfig.clientConfig.debuffOverlay.getX(), yStart + offset * 18, 50, 18, event.getPartialTicks(), renderer);
                        ++offset;
                    }
                }
                int renderIndex = 0;
                List<ISkill> list = skills.getUnlockedSkills().get(SkillCategory.SURVIVAL);
                if (list == null) return;
                int left = 5;
                int top = window.getGuiScaledHeight() - 25;
                List<ISkill> renderSkills = new ArrayList<>();
                for (ISkill skill : list) {
                    if (skill instanceof IOverlayRender) {
                        skill = SkillUtil.getBestSkillFromOverrides(skill, player);
                        if (!renderSkills.contains(skill)) renderSkills.add(skill);
                    }
                }
                for (ISkill skill : renderSkills) {
                    IOverlayRender overlayRenderer = (IOverlayRender) skill;
                    if (skill.apply(player) && overlayRenderer.shouldRenderOnHUD()) {
                        overlayRenderer.renderInHUD(matrixStack, skill, renderIndex, left, top);
                        ++renderIndex;
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void mouseInputEvent(InputEvent.MouseInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        GameSettings settings = mc.options;
        if (player != null) {
            GunItem item = ShootingManager.getGunFrom(player);
            ItemStack stack = player.getMainHandItem();
            if (item != null) {
                if (settings.keyAttack.isDown()) {
                    Firemode firemode = item.getFiremode(stack);
                    if (firemode == Firemode.SINGLE && ShootingManager.canShoot(player, stack)) {
                        shoot(player, stack);
                    } else if (firemode == Firemode.BURST) {
                        if (!burst) {
                            burst = true;
                            shotsLeft = 2;
                        }
                    }
                } else if (settings.keyUse.isDown() && ClientSideManager.instance().processor().getByID(Animations.REBOLT) == null && !player.isSprinting()) {
                    LazyOptional<IPlayerData> optional = PlayerData.get(player);
                    boolean aim = optional.isPresent() && optional.orElse(null).getAimInfo().aiming;
                    if (!aim) {
                        preAimFov.map(settings.fov);
                        preAimSens.map(settings.sensitivity);
                        if (item == ModItems.SNIPER_RIFLE && PlayerData.hasActiveSkill(player, Skills.SR_SCOPE)) {
                            settings.sensitivity = preAimSens.get() * 0.3F;
                            settings.fov = 15.0F;
                        } else if (item == ModItems.CROSSBOW && PlayerData.hasActiveSkill(player, Skills.CROSSBOW_SCOPE)) {
                            settings.sensitivity = preAimSens.get() * 0.4F;
                            settings.fov = 25.0F;
                        }
                        ClientSideManager.instance().processor().play(Animations.AIMING, item.createAimAnimation());
                    } else {
                        preAimFov.ifPresent(value -> settings.fov = value);
                        preAimSens.ifPresent(value -> settings.sensitivity = value);
                    }
                    NetworkManager.sendServerPacket(new SPacketSetAiming(!aim));
                }
            }
        }
    }

    @SubscribeEvent
    public static void renderHandEvent(RenderHandEvent event) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        ItemStack stack = event.getItemStack();
        if (event.getHand() == Hand.OFF_HAND) {
            if (event.getItemStack().getItem() == Items.SHIELD && player.getMainHandItem().getItem() instanceof GunItem) {
                event.setCanceled(true);
            }
        }
        LazyOptional<IPlayerData> optional = PlayerData.get(player);
        if (!optional.isPresent()) {
            event.setCanceled(true);
            return;
        } else {
            IPlayerData data = optional.orElse(null);
            AimInfo info = data.getAimInfo();
            ScopeRenderer renderer = ModConfig.clientConfig.scopeRenderer.get();
            Item item = stack.getItem();
            if (info.isAiming() && renderer == ScopeRenderer.TEXTURE && (PlayerData.hasActiveSkill(player, Skills.SR_SCOPE) && item == ModItems.SNIPER_RIFLE || PlayerData.hasActiveSkill(player, Skills.CROSSBOW_SCOPE) && item == ModItems.CROSSBOW)) {
                event.setCanceled(true);
                return;
            }
        }
        if (stack.getItem() instanceof IAnimationEntry) {
            AnimationCompatLayer animLib = AnimationCompatLayer.instance();
            ClientSideManager client = ClientSideManager.instance();
            IHandRenderAPI handAPI = animLib.getHandRenderAPI();
            IAnimationEntry animationEntry = (IAnimationEntry) stack.getItem();
            IAnimationPipeline pipeline = animLib.pipeline();
            boolean devMode = handAPI.isDevMode();
            IHandTransformer transformer = devMode ? handAPI : animationEntry;
            Function<HandSide, IRenderConfig> configSelector = side -> side == HandSide.RIGHT ? transformer.right() : transformer.left();
            event.setCanceled(true);
            Minecraft mc = Minecraft.getInstance();
            FirstPersonRenderer fpRenderer = mc.getItemInHandRenderer();
            boolean mainHand = event.getHand() == Hand.MAIN_HAND;
            ItemCameraTransforms.TransformType transformType = mainHand ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND;
            boolean disableVanillaAnimations = animationEntry.disableVanillaAnimations();
            float equip = disableVanillaAnimations ? 0.0F : event.getEquipProgress();
            float swing = disableVanillaAnimations ? 0.0F : event.getSwingProgress();
            MatrixStack matrix = event.getMatrixStack();
            IRenderTypeBuffer buffer = event.getBuffers();
            int packedLight = event.getLight();

            matrix.pushPose();
            {
                client.setDualWieldRender(false);
                pipeline.animateStage(AnimationStage.ITEM_AND_HANDS);
                matrix.pushPose();
                {
                    pipeline.animateStage(AnimationStage.HANDS);
                    renderAnimatedItemFP(matrix, buffer, packedLight, equip, configSelector, pipeline);
                }
                matrix.popPose();
                pipeline.animateStage(AnimationStage.HELD_ITEM);
                if (!pipeline.isItemRenderBlocked())
                    renderItem(fpRenderer, player, stack, transformType, !mainHand, matrix, buffer, packedLight, swing, equip);
            }
            matrix.popPose();

            if (stack.getItem() == ModItems.PISTOL && PlayerData.hasActiveSkill(player, Skills.PISTOL_DUAL_WIELD)) {
                matrix.pushPose();
                {
                    client.setDualWieldRender(true);
                    pipeline.animateStage(AnimationStage.ITEM_AND_HANDS);
                    matrix.pushPose();
                    {
                        pipeline.animateStage(AnimationStage.HANDS);
                        renderAnimatedItemFP(matrix, buffer, packedLight, equip, configSelector, pipeline);
                    }
                    matrix.popPose();
                    pipeline.animateStage(AnimationStage.HELD_ITEM);
                    renderItem(fpRenderer, player, stack, ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, true, matrix, buffer, packedLight, swing, equip);
                }
                matrix.popPose();
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if (event.phase == TickEvent.Phase.END && player != null) {
            if (shootDelay > 0)
                --shootDelay;
            AnimationCompatLayer.instance().pipeline().handleGameTick();
            startSprintListener.update(player);
            GameSettings settings = mc.options;
            if (burst) {
                if (shotsLeft > 0) {
                    ItemStack stack = player.getMainHandItem();
                    if (stack.getItem() instanceof GunItem) {
                        if (shootDelay == 0) {
                            if (ShootingManager.canShoot(player, stack)) {
                                shoot(player, stack);
                                shotsLeft--;
                            } else burst = false;
                        }
                    }
                } else burst = false;
            } else if (settings.keyAttack.isDown()) {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof GunItem) {
                    GunItem gun = (GunItem) stack.getItem();
                    if (gun.getFiremode(stack) == Firemode.FULL_AUTO && shootDelay == 0 && ShootingManager.canShoot(player, stack)) {
                        shoot(player, stack);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START)
            AnimationCompatLayer.instance().pipeline().processFrame(event.renderTickTime);
    }

    private static void renderItem(FirstPersonRenderer renderer, PlayerEntity player, ItemStack stack, ItemCameraTransforms.TransformType transform, boolean offHand, MatrixStack matrix, IRenderTypeBuffer buffer, int light, float swingProgress, float equipProgress) {
        HandSide handside = offHand ? HandSide.LEFT : HandSide.RIGHT;
        float f5 = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float)Math.PI);
        float f6 = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float)Math.PI * 2F));
        float f10 = -0.2F * MathHelper.sin(swingProgress * (float)Math.PI);
        int l = !offHand ? 1 : -1;
        matrix.translate(l * f5, f6, f10);
        applyItemArmTransform(matrix, handside, equipProgress);
        applyItemArmAttackTransform(matrix, handside, swingProgress);
        renderer.renderItem(player, stack, transform, offHand, matrix, buffer, light);
    }

    private static void applyItemArmTransform(MatrixStack matrix, HandSide side, float equipProgress) {
        int offset = side == HandSide.RIGHT ? 1 : -1;
        matrix.translate(offset * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private static void applyItemArmAttackTransform(MatrixStack matrix, HandSide side, float swingProgress) {
        int i = side == HandSide.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrix.mulPose(Vector3f.YP.rotationDegrees(i * (45.0F + f * -20.0F)));
        float f1 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrix.mulPose(Vector3f.ZP.rotationDegrees(i * f1 * -20.0F));
        matrix.mulPose(Vector3f.XP.rotationDegrees(f1 * -80.0F));
        matrix.mulPose(Vector3f.YP.rotationDegrees(i * -45.0F));
    }

    private static void renderAnimatedItemFP(MatrixStack stack, IRenderTypeBuffer buffer, int packedLight, float equipProgress, Function<HandSide, IRenderConfig> function, IAnimationPipeline pipeline) {
        float yOff = -0.5F * equipProgress;
        RenderSystem.disableCull();
        stack.pushPose();
        {
            stack.translate(0, yOff, 0);
            stack.pushPose();
            {
                pipeline.animateStage(AnimationStage.RIGHT_HAND);
                renderHand(stack, HandSide.RIGHT, function, buffer, packedLight);
            }
            stack.popPose();
            stack.pushPose();
            {
                pipeline.animateStage(AnimationStage.LEFT_HAND);
                renderHand(stack, HandSide.LEFT, function, buffer, packedLight);
            }
            stack.popPose();
        }
        stack.popPose();
        RenderSystem.enableCull();
    }

    private static void shoot(PlayerEntity player, ItemStack stack) {
        GunItem gun = (GunItem) stack.getItem();
        player.xRot -= gun.getVerticalRecoil(player);
        player.yRot += gun.getHorizontalRecoil(player);
        NetworkManager.sendServerPacket(new SPacketShoot());
        gun.onShoot(player, stack);
        shootDelay = gun.getFirerate(player);
    }

    private static void renderHand(MatrixStack stack, HandSide side, Function<HandSide, IRenderConfig> configFunction, IRenderTypeBuffer buffer, int packedLight) {
        boolean rightArm = side == HandSide.RIGHT;
        IRenderConfig config = configFunction.apply(side);
        config.applyTo(stack);
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(mc.player.getSkinTextureLocation());
        EntityRenderer<? super ClientPlayerEntity> entityRenderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
        PlayerRenderer playerRenderer = (PlayerRenderer) entityRenderer;

        stack.pushPose();
        {
            stack.mulPose(Vector3f.YP.rotationDegrees(40.0F));
            stack.mulPose(Vector3f.XP.rotationDegrees(-90.0F));
            if (rightArm) {
                stack.translate(0.8F, -0.3F, -0.4F);
                playerRenderer.renderRightHand(stack, buffer, packedLight, mc.player);
            } else {
                stack.translate(-0.5F, 0.6F, -0.36F);
                playerRenderer.renderLeftHand(stack, buffer, packedLight, mc.player);
            }
            stack.mulPose(Vector3f.ZP.rotationDegrees(-41.0F));
        }
        stack.popPose();
    }

    private static class ChangeDetector {

        private final Function<PlayerEntity, Boolean> stateGetter;
        private final Runnable onChange;
        private boolean lastState;

        public ChangeDetector(Runnable onChange, Function<PlayerEntity, Boolean> stateGetter) {
            this.onChange = onChange;
            this.stateGetter = stateGetter;
        }

        public void update(PlayerEntity player) {
            boolean current = stateGetter.apply(player);
            if (!lastState && current) {
                onChange.run();
            }
            lastState = current;
        }
    }
}
