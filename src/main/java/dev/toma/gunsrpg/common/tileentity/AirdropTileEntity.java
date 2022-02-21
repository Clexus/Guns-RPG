package dev.toma.gunsrpg.common.tileentity;

import dev.toma.gunsrpg.GunsRPG;
import dev.toma.gunsrpg.common.init.ModBlockEntities;
import dev.toma.gunsrpg.resource.crate.ILootContentProvider;
import dev.toma.gunsrpg.resource.crate.LootManager;
import dev.toma.gunsrpg.util.Lifecycle;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

public class AirdropTileEntity extends InventoryTileEntity implements ILootGenerator {

    public static final ResourceLocation AIRDROP_CONFIGURATION = GunsRPG.makeResource("airdrops");

    public AirdropTileEntity() {
        this(ModBlockEntities.AIRDROP.get());
    }

    protected AirdropTileEntity(TileEntityType<? extends AirdropTileEntity> type) {
        super(type);
    }

    @Override
    public IItemHandlerModifiable createInventory() {
        return new ItemStackHandler(9);
    }

    @Override
    public boolean isEmptyInventory() {
        return isEmpty();
    }

    @Override
    public void generateLoot() {
        Lifecycle lifecycle = GunsRPG.getModLifecycle();
        LootManager lootManager = lifecycle.getAirdropLootManager();
        ILootContentProvider contentProvider = lootManager.getGeneratedContent(AIRDROP_CONFIGURATION);
        ItemStack[] itemStacks = contentProvider.getItems();
        for (int i = 0; i < itemStacks.length; i++) {
            itemHandler.setStackInSlot(i, itemStacks[i]);
        }
    }
}
