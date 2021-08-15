package dev.toma.gunsrpg.common.item.guns;

import dev.toma.gunsrpg.GunsRPG;
import dev.toma.gunsrpg.client.render.item.SksRenderer;
import dev.toma.gunsrpg.common.capability.PlayerData;
import dev.toma.gunsrpg.common.init.ModSounds;
import dev.toma.gunsrpg.common.init.Skills;
import dev.toma.gunsrpg.common.item.guns.ammo.AmmoMaterial;
import dev.toma.gunsrpg.common.item.guns.util.Firemode;
import dev.toma.gunsrpg.common.item.guns.util.GunType;
import dev.toma.gunsrpg.common.skills.core.SkillType;
import dev.toma.gunsrpg.config.ModConfig;
import dev.toma.gunsrpg.config.gun.IWeaponConfig;
import dev.toma.gunsrpg.util.SkillUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;

public class SksItem extends GunItem {

    private static final ResourceLocation[] AIM_ANIMATIONS = {
            GunsRPG.makeResource("sks/aim"),
            GunsRPG.makeResource("sks/aim_red_dot")
    };
    private static final ResourceLocation RELOAD_ANIMATION = GunsRPG.makeResource("sks/reload");

    public SksItem(String name) {
        super(name, GunType.AR, new Properties().setISTER(() -> SksRenderer::new));
    }

    @Override
    public IWeaponConfig getWeaponConfig() {
        return ModConfig.weaponConfig.sks;
    }

    @Override
    public void fillAmmoMaterialData(Map<AmmoMaterial, Integer> data) {
        data.put(AmmoMaterial.WOOD, 0);
        data.put(AmmoMaterial.STONE, 2);
        data.put(AmmoMaterial.IRON, 4);
        data.put(AmmoMaterial.GOLD, 6);
        data.put(AmmoMaterial.DIAMOND, 9);
        data.put(AmmoMaterial.EMERALD, 11);
        data.put(AmmoMaterial.AMETHYST, 14);
    }

    @Override
    protected SoundEvent getShootSound(PlayerEntity entity) {
        return this.isSilenced(entity) ? ModSounds.SKS_SILENT : ModSounds.SKS;
    }

    @Override
    protected SoundEvent getEntityShootSound(LivingEntity entity) {
        return ModSounds.SLR;
    }

    @Override
    public SoundEvent getReloadSound(PlayerEntity player) {
        return ModSounds.AR_RELOAD;
    }

    @Override
    public boolean isSilenced(PlayerEntity player) {
        return PlayerData.hasActiveSkill(player, Skills.AR_SUPPRESSOR);
    }

    @Override
    public int getMaxAmmo(PlayerEntity player) {
        return PlayerData.hasActiveSkill(player, Skills.AR_EXTENDED) ? 20 : 10;
    }

    @Override
    public int getFirerate(PlayerEntity player) {
        IWeaponConfig cfg = getWeaponConfig();
        int firerate = PlayerData.hasActiveSkill(player, Skills.AR_TOUGH_SPRING) ? cfg.getUpgradedFirerate() : cfg.getFirerate();
        if (PlayerData.hasActiveSkill(player, Skills.AR_ADAPTIVE_CHAMBERING)) {
            firerate -= 2;
        }
        return Math.max(firerate, 1);
    }

    @Override
    public int getReloadTime(PlayerEntity player) {
        return (int) (32 * SkillUtil.getReloadTimeMultiplier(player));
    }

    @Override
    public float getVerticalRecoil(PlayerEntity player) {
        float f = super.getVerticalRecoil(player);
        float mod = PlayerData.hasActiveSkill(player, Skills.AR_VERTICAL_GRIP) ? ModConfig.weaponConfig.general.verticalGrip.floatValue() : 1.0F;
        float mod2 = PlayerData.hasActiveSkill(player, Skills.AR_CHEEKPAD) ? ModConfig.weaponConfig.general.cheekpad.floatValue() : 1.0F;
        return mod * mod2 * f;
    }

    @Override
    public float getHorizontalRecoil(PlayerEntity player) {
        float f = super.getHorizontalRecoil(player);
        float mod = PlayerData.hasActiveSkill(player, Skills.AR_CHEEKPAD) ? ModConfig.weaponConfig.general.cheekpad.floatValue() : 1.0F;
        return mod * f;
    }

    @Override
    public boolean switchFiremode(ItemStack stack, PlayerEntity player) {
        Firemode firemode = this.getFiremode(stack);
        int newMode = 0;
        if (firemode == Firemode.SINGLE && PlayerData.hasActiveSkill(player, Skills.AR_ADAPTIVE_CHAMBERING)) {
            newMode = 2;
        }
        stack.getTag().putInt("firemode", newMode);
        return firemode.ordinal() != newMode;
    }

    @Override
    public SkillType<?> getRequiredSkill() {
        return Skills.ASSAULT_RIFLE_ASSEMBLY;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ResourceLocation getAimAnimationPath(ItemStack stack, PlayerEntity player) {
        boolean scoped = PlayerData.hasActiveSkill(player, Skills.AR_RED_DOT);
        return AIM_ANIMATIONS[scoped ? 1 : 0];
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ResourceLocation getReloadAnimation(PlayerEntity player) {
        return RELOAD_ANIMATION;
    }
}
