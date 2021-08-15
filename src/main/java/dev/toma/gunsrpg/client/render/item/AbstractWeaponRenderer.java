package dev.toma.gunsrpg.client.render.item;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.toma.gunsrpg.GunsRPG;
import dev.toma.gunsrpg.client.model.AbstractAttachmentModel;
import dev.toma.gunsrpg.client.model.AbstractWeaponModel;
import dev.toma.gunsrpg.client.model.ScopeModel;
import dev.toma.gunsrpg.client.model.WeaponModels;
import dev.toma.gunsrpg.common.capability.IPlayerData;
import dev.toma.gunsrpg.common.capability.PlayerData;
import lib.toma.animations.IRenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public abstract class AbstractWeaponRenderer extends ItemStackTileEntityRenderer {

    public static final ResourceLocation ATTACHMENTS = GunsRPG.makeResource("textures/item/attachments.png");
    private final ResourceLocation gunTexture;

    public AbstractWeaponRenderer() {
        this.gunTexture = createGunTextureInstance();
    }

    @Override
    public final void renderByItem(ItemStack stack, ItemCameraTransforms.TransformType transformType, MatrixStack matrix, IRenderTypeBuffer renderBuffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        PlayerData.get(mc.player).ifPresent(data -> {
            matrix.pushPose();
            {
                positionModel(matrix, transformType);
                setupAndRender(stack, matrix, transformType, data, renderBuffer, light, overlay);
                if (hasCustomAttachments() && canRenderAttachments(transformType)) {
                    matrix.pushPose();
                    float aimProgress = data.getAimInfo().getProgress();
                    renderAttachments(data, matrix, renderBuffer, ATTACHMENTS, light, overlay, aimProgress);
                    matrix.popPose();
                }
            }
            matrix.popPose();
        });
    }

    public abstract AbstractWeaponModel getWeaponModel();

    public abstract ResourceLocation createGunTextureInstance();

    protected boolean hasCustomAttachments() {
        return false;
    }

    protected void transformUI(MatrixStack matrix) {

    }

    protected void positionModel(MatrixStack stack, ItemCameraTransforms.TransformType transform) {
    }

    protected float scaleForTransform(ItemCameraTransforms.TransformType transform) {
        return 0.4F;
    }

    protected void renderAttachments(IPlayerData data, MatrixStack matrix, IRenderTypeBuffer typeBuffer, ResourceLocation texture, int light, int overlay, float progress) {

    }

    protected void renderScope(IRenderConfig config, MatrixStack poseStack, IRenderTypeBuffer buffer, int light, int overlay, float progress) {
        doConfiguredRender(WeaponModels.SCOPE, config, poseStack, buffer, light, overlay, progress);
    }

    protected void renderScope(IRenderConfig config, MatrixStack poseStack, IRenderTypeBuffer buffer, int light, int overlay, float progress, ResourceLocation reticleTexture) {
        ScopeModel.prepare(reticleTexture);
        doConfiguredRender(WeaponModels.SCOPE, config, poseStack, buffer, light, overlay, progress);
    }

    private void doConfiguredRender(AbstractAttachmentModel model, IRenderConfig config, MatrixStack pose, IRenderTypeBuffer buffer, int light, int overlay, float aimProgress) {
        pose.pushPose();
        config.applyTo(pose);
        model.renderAttachment(pose, buffer, light, overlay, aimProgress);
        pose.popPose();
    }

    private void defaultUITransform(MatrixStack matrix) {
        transformUI(matrix);
        matrix.translate(-0.25, -0.3, 0.0);
        matrix.scale(0.7F, 0.7F, 0.7F);
        matrix.mulPose(Vector3f.ZN.rotationDegrees(45));
        matrix.mulPose(Vector3f.YN.rotationDegrees(90));
    }

    private boolean canRenderAttachments(ItemCameraTransforms.TransformType type) {
        return type != ItemCameraTransforms.TransformType.GUI;
    }

    private void setupAndRender(ItemStack stack, MatrixStack matrix, ItemCameraTransforms.TransformType transformType, IPlayerData data, IRenderTypeBuffer renderBuffer, int light, int overlay) {
        matrix.translate(0.7, 0.5, 0.05);
        matrix.mulPose(Vector3f.XP.rotationDegrees(180));
        matrix.mulPose(Vector3f.YP.rotationDegrees(180));
        float scaleFactor = scaleForTransform(transformType);
        matrix.scale(scaleFactor, scaleFactor, scaleFactor);
        if (transformType == ItemCameraTransforms.TransformType.GUI) {
            defaultUITransform(matrix);
        }
        AbstractWeaponModel weaponModel = getWeaponModel();
        weaponModel.renderWeapon(stack, data, matrix, renderBuffer.getBuffer(weaponModel.renderType(gunTexture)), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
