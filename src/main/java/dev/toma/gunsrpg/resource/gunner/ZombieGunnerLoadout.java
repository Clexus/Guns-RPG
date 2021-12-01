package dev.toma.gunsrpg.resource.gunner;

import com.google.common.base.Preconditions;
import dev.toma.gunsrpg.api.common.IAmmoMaterial;
import dev.toma.gunsrpg.common.entity.ZombieGunnerEntity;
import dev.toma.gunsrpg.common.item.guns.GunItem;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.Difficulty;

public final class ZombieGunnerLoadout {

    // general
    private final int weight;
    // weapon
    private final GunItem weapon;
    private final IDifficultyProperty<IAmmoMaterial> ammo;
    private final int magCapacity;
    private final IDifficultyProperty<Integer> reloadTime;
    private final IDifficultyProperty<Integer> firerate;
    // ai
    private final IDifficultyProperty<Float> damageMultiplier;
    private final float baseInaccuracy;
    private final float accuracyBonus;
    private final int burstSize;
    private final IDifficultyProperty<Integer> burstDelay;
    // cosmetic
    private final int capColor;

    private ZombieGunnerLoadout(Builder builder) {
        weight = builder.weight;
        weapon = builder.weapon;
        ammo = builder.ammo;
        magCapacity = builder.magCapacity;
        reloadTime = builder.reloadTime;
        firerate = builder.firerate;
        damageMultiplier = builder.damageMultiplier;
        baseInaccuracy = builder.baseInaccuracy;
        accuracyBonus = builder.accuracyBonus;
        burstSize = builder.burstSize;
        burstDelay = builder.burstDelay;
        capColor = builder.capColor;
    }

    public void applyGear(ZombieGunnerEntity entity, Difficulty difficulty) {
        ItemStack stack = new ItemStack(weapon);
        IAmmoMaterial material = ammo.getProperty(difficulty);
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("ammo", Integer.MAX_VALUE);
        nbt.putString("material", material.getMaterialID().toString());
        stack.setTag(nbt);
        entity.setItemSlot(EquipmentSlotType.MAINHAND, stack);
        entity.setFirerate(firerate.getProperty(difficulty));
    }

    public int getWeight() {
        return weight;
    }

    public static class Builder {

        // general
        private int weight;
        // weapon
        private GunItem weapon;
        private IDifficultyProperty<IAmmoMaterial> ammo;
        private int magCapacity;
        private IDifficultyProperty<Integer> reloadTime;
        private IDifficultyProperty<Integer> firerate;
        // ai
        private IDifficultyProperty<Float> damageMultiplier;
        private float baseInaccuracy;
        private float accuracyBonus;
        private int burstSize;
        private IDifficultyProperty<Integer> burstDelay;
        // cosmetic
        private int capColor;

        public Builder chance(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder weapon(GunItem weapon, IDifficultyProperty<IAmmoMaterial> ammo) {
            this.weapon = weapon;
            this.ammo = ammo;
            return this;
        }

        public Builder weaponProps(int magCapacity, IDifficultyProperty<Integer> reloadTime, IDifficultyProperty<Integer> firerate) {
            this.magCapacity = magCapacity;
            this.reloadTime = reloadTime;
            this.firerate = firerate;
            return this;
        }

        public Builder AI(IDifficultyProperty<Float> damageMultiplier, float baseInaccuracy, float accuracyBonus, int burstSize, IDifficultyProperty<Integer> burstDelay) {
            this.damageMultiplier = damageMultiplier;
            this.baseInaccuracy = baseInaccuracy;
            this.accuracyBonus = accuracyBonus;
            this.burstSize = burstSize;
            this.burstDelay = burstDelay;
            return this;
        }

        public Builder cosmetics(int capColor) {
            this.capColor = capColor;
            return this;
        }

        public ZombieGunnerLoadout buildLoadout() {
            Preconditions.checkState(weight > 0, "Weight must be bigger than 0");
            Preconditions.checkNotNull(weapon, "Weapon cannot be null");
            Preconditions.checkNotNull(ammo, "Ammo cannot be null");
            Preconditions.checkState(magCapacity > 0, "Magazine capacity must be bigger than 0");
            Preconditions.checkNotNull(reloadTime, "Reload time cannot be null");
            Preconditions.checkNotNull(firerate, "Firerate cannot be null");
            Preconditions.checkNotNull(damageMultiplier, "Damage multiplier cannot be null");
            Preconditions.checkState(baseInaccuracy >= 0, "Inaccuracy cannot be smaller than 0");
            Preconditions.checkState(accuracyBonus >= 0, "Accuracy bonus cannot be smaller than 0");
            Preconditions.checkState((baseInaccuracy - (3 * accuracyBonus)) >= 0, "(Inaccuracy-3*bonus) cannot be smaller than 0");
            Preconditions.checkState(burstSize >= 0, "Burst size cannot be smaller than 0");
            Preconditions.checkNotNull(burstDelay, "Burst delay cannot be null");
            return new ZombieGunnerLoadout(this);
        }
    }
}
