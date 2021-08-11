package dev.toma.gunsrpg.client.animation;

import dev.toma.gunsrpg.GunsRPG;
import dev.toma.gunsrpg.common.item.guns.GunItem;
import lib.toma.animations.Animation;
import lib.toma.animations.AnimationEngine;
import lib.toma.animations.pipeline.AnimationType;
import lib.toma.animations.pipeline.IAnimation;
import lib.toma.animations.serialization.AnimationLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class GRPGAnimations {
    private GRPGAnimations() {}

    public static final AnimationType<AimAnimation> AIM_ANIMATION = new AnimationType<>(GunsRPG.makeResource("aim_animation"));
    public static final AnimationType<SprintAnimation> SPRINT = new AnimationType<>(GunsRPG.makeResource("sprinting"), SprintAnimation::new);
    public static final AnimationType<IAnimation> CHAMBER = new AnimationType<>(GunsRPG.makeResource("chamber"));
    public static final AnimationType<Animation> HEAL = new AnimationType<>(GunsRPG.makeResource("heal"));
    public static final AnimationType<Animation> RELOAD = new AnimationType<>(GunsRPG.makeResource("reload"), GRPGAnimations::createReloadAnimation);

    public static final ResourceLocation INJECTION = GunsRPG.makeResource("injection");
    public static final ResourceLocation PILLS = GunsRPG.makeResource("pills");
    public static final ResourceLocation BANDAGE = GunsRPG.makeResource("bandage");
    public static final ResourceLocation SPLINT = GunsRPG.makeResource("splint");

    public static Animation createReloadAnimation(PlayerEntity player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem) {
            GunItem item = (GunItem) stack.getItem();
            ResourceLocation animationPath = item.getReloadAnimation(player);
            AnimationEngine engine = AnimationEngine.get();
            AnimationLoader loader = engine.loader();
            return new Animation(loader.getProvider(animationPath), item.getReloadTime(player));
        }
        return null;
    }
}
