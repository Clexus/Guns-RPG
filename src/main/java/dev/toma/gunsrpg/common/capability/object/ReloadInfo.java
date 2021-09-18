package dev.toma.gunsrpg.common.capability.object;

import dev.toma.gunsrpg.api.common.IAmmoMaterial;
import dev.toma.gunsrpg.api.common.IReloader;
import dev.toma.gunsrpg.api.common.data.IReloadInfo;
import dev.toma.gunsrpg.common.item.guns.GunItem;
import dev.toma.gunsrpg.common.item.guns.ammo.AmmoType;
import dev.toma.gunsrpg.util.AmmoLocator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class ReloadInfo implements IReloadInfo {

    private IReloader activeReloadManager = IReloader.EMPTY;
    private int reloadingSlot;

    @Override
    public void enqueueCancel() {
        activeReloadManager.enqueueCancel();
    }

    @Override
    public void tick(PlayerEntity player) {
        int equippedSlot = player.inventory.selected;
        if (equippedSlot != reloadingSlot) {
            activeReloadManager.forceCancel();
            activeReloadManager = IReloader.EMPTY;
        }
        activeReloadManager.tick(player);
    }

    @Override
    public void startReloading(PlayerEntity player, GunItem gun, ItemStack stack, int slot) {
        AmmoLocator locator = new AmmoLocator();
        AmmoType ammoType = gun.getAmmoType();
        IAmmoMaterial material = gun.getMaterialFromNBT(stack);
        if (material != null && (player.isCreative() || locator.hasAmmo(player.inventory, AmmoLocator.ISearchConstraint.typeAndMaterial(ammoType, material)))) {
            activeReloadManager = gun.getReloadManager(player).createReloadHandler();
            activeReloadManager.initiateReload(player, gun, stack);
            reloadingSlot = slot;
        }
    }

    @Override
    public boolean isReloading() {
        return activeReloadManager.isReloading();
    }
}
