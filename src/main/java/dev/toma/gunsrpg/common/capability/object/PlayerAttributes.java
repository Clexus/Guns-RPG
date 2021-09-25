package dev.toma.gunsrpg.common.capability.object;

import dev.toma.gunsrpg.api.common.data.DataFlags;
import dev.toma.gunsrpg.api.common.data.IPlayerCapEntry;
import dev.toma.gunsrpg.common.attribute.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerAttributes implements IAttributeProvider, IPlayerCapEntry {

    private final Map<IAttributeId, IAttribute> attributeMap = new HashMap<>();
    private final AttributeListener listener = new AttributeListener();
    private IClientSynchReq syncRequest;

    @Override
    public IAttribute getAttribute(IAttributeId id) {
        IAttribute attribute = attributeMap.get(id);
        if (attribute == null) {
            addAttribute(id);
            return attributeMap.get(id);
        }
        return attribute;
    }

    @Override
    public void addAttribute(IAttributeId id) {
        IAttribute attribute = attributeMap.put(id, id.createNewInstance());
        attribute.addAttributeListener(listener);
        safeSync();
    }

    @Override
    public void removeAttribute(IAttributeId id) {
        IAttribute attribute = attributeMap.remove(id);
        if (attribute != null) {
            attribute.removeListener(listener);
        }
        safeSync();
    }

    @Override
    public void removeAttribute(IAttribute attribute) {
        removeAttribute(attribute.getId());
    }

    @Override
    public void tick() {
        attributeMap.values().forEach(IAttribute::tickAttributes);
    }

    @Override
    public void setAttributeValue(IAttributeId id, double value) {
        IAttribute attribute = getAttribute(id);
        if (attribute != null) {
            attribute.setValue(value);
        }
        safeSync();
    }

    @Override
    public double getAttributeValue(IAttributeId id) {
        IAttribute attribute = getAttribute(id);
        return attribute != null ? attribute.value() : id.getBaseValue();
    }

    @Override
    public boolean hasAttribute(IAttributeId id) {
        return getAttribute(id) != null;
    }

    @Override
    public void toNbt(CompoundNBT nbt) {
        CompoundNBT cnbt = new CompoundNBT();
        nbt.put("attributes", cnbt);
    }

    @Override
    public void fromNbt(CompoundNBT nbt) {
        CompoundNBT cnbt = nbt.contains("attributes", Constants.NBT.TAG_COMPOUND) ? nbt.getCompound("attributes") : new CompoundNBT();

    }

    @Override
    public void setClientSynch(IClientSynchReq request) {
        this.syncRequest = request;
    }

    @Override
    public int getFlag() {
        return DataFlags.ATTRIB;
    }

    private void safeSync() {
        if (syncRequest != null)
            syncRequest.makeSyncRequest();
    }

    public class AttributeListener implements IAttributeListener {

        private final UUID uuid = UUID.randomUUID();

        @Override
        public UUID getListenerUid() {
            return uuid;
        }

        @Override
        public void onModifierAdded(IAttributeModifier modifier) {
            sendSyncRequest();
        }

        @Override
        public void onModifierRemoved(IAttributeModifier modifier) {
            sendSyncRequest();
        }

        @Override
        public void onValueChanged(double value) {
            sendSyncRequest();
        }

        private void sendSyncRequest() {
            PlayerAttributes.this.safeSync();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AttributeListener that = (AttributeListener) o;
            return uuid.equals(that.uuid);
        }

        @Override
        public int hashCode() {
            return uuid.hashCode();
        }
    }
}
