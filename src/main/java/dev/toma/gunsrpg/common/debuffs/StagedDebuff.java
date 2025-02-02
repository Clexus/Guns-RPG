package dev.toma.gunsrpg.common.debuffs;

import dev.toma.gunsrpg.api.common.attribute.IAttributeProvider;
import dev.toma.gunsrpg.api.common.data.DataFlags;
import dev.toma.gunsrpg.api.common.data.IDebuffs;
import dev.toma.gunsrpg.common.capability.PlayerData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

import java.util.Objects;

public class StagedDebuff implements IStagedDebuff, ProgressingDebuff {

    private final StagedDebuffType<?> type;
    private StagedDebuffType.LinkedStage current;
    private int progression; // debuff pct
    private int progressionCounter; // pct incr delay

    // render variables
    private int ticksSinceAdded;
    private int ticksSinceProgressed;
    private int ticksSinceHealed;

    public StagedDebuff(StagedDebuffType<?> type) {
        this.type = type;
        this.current = type.firstStage();
        this.ticksSinceHealed = 100;
    }

    @Override
    public void tick(PlayerEntity player) {
        PlayerData.get(player).ifPresent(data -> {
            this.updateRenderCounters();
            int delay = type.getDelay(data);
            if (this.canSpread() && ++progressionCounter >= delay) {
                this.incrementProgression(1);
                if (!player.level.isClientSide) data.sync(DataFlags.DEBUFF);
            }
            if (!isFrozen(data.getAttributes())) {
                current.getStageEvent().accept(player);
            }
        });
    }

    @Override
    public void incrementProgression(int count) {
        int prevProgression = this.progression;
        this.progression = Math.min(this.getProgressionLimit(), this.progression + count);
        if (prevProgression != progression) {
            this.progressionCounter = 0;
            this.ticksSinceProgressed = 0;
            this.updateStage();
        }
    }

    @Override
    public float getBlockingProgress(IAttributeProvider provider) {
        return StagedDebuffType.getBuffedProgress(provider, this.type.getBlockingAttribute());
    }

    @Override
    public boolean isFrozen(IAttributeProvider attributes) {
        return type.isTemporarilyDisabled(attributes);
    }

    @Override
    public void heal(int amount, IDebuffs data) {
        if ((progression -= amount) < 0) {
            data.clearDebuff(type);
        }
        ticksSinceHealed = 0;
    }

    @Override
    public int getCurrentProgress() {
        return progression;
    }

    @Override
    public IDebuffType<?> getType() {
        return type;
    }

    @Override
    public boolean canSpread() {
        return progression < 100;
    }

    @Override
    public int ticksSinceAdded() {
        return ticksSinceAdded;
    }

    @Override
    public int ticksSinceHealed() {
        return ticksSinceHealed;
    }

    @Override
    public int ticksSinceProgressed() {
        return ticksSinceProgressed;
    }

    protected int getProgressionLimit() {
        return 100;
    }

    @Override
    public CompoundNBT toNbt() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("progression", progression);
        nbt.putInt("progressionCounter", progressionCounter);
        nbt.putInt("stageLimit", current.getLimit());
        saveNonZeroInt(nbt, ticksSinceAdded, "ticksSinceAdded");
        saveNonZeroInt(nbt, ticksSinceProgressed, "ticksSinceProgressed");
        saveNonZeroInt(nbt, ticksSinceHealed, "ticksSinceHealed");
        return nbt;
    }

    @Override
    public void fromNbt(CompoundNBT nbt) {
        progression = nbt.getInt("progression");
        progressionCounter = nbt.getInt("progressionCounter");
        advanceStage(type.firstStage(), nbt.getInt("stageLimit"));
        ticksSinceAdded = nbt.getInt("ticksSinceAdded");
        ticksSinceProgressed = nbt.getInt("ticksSinceProgressed");
        ticksSinceHealed = nbt.getInt("ticksSinceHealed");
    }

    @Override
    public boolean shouldRemove() {
        return progression < 0;
    }

    private void updateStage() {
        int limit = current.getLimit();
        if (progression > limit) {
            current = Objects.requireNonNull(current.getNext(), "Next stage was null");
        }
    }

    private void advanceStage(StagedDebuffType.LinkedStage stage, int limit) {
        current = stage;
        if (current.getLimit() < limit && current.getNext() != null) {
            advanceStage(current.getNext(), limit);
        }
    }

    private void updateRenderCounters() {
        ++ticksSinceAdded;
        ++ticksSinceProgressed;
        ++ticksSinceHealed;
    }

    private void saveNonZeroInt(CompoundNBT nbt, int value, String name) {
        if (value != 0)
            nbt.putInt(name, value);
    }
}
