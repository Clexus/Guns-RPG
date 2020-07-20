package dev.toma.gunsrpg.util;

import dev.toma.gunsrpg.common.ModRegistry;
import dev.toma.gunsrpg.common.capability.PlayerDataFactory;
import dev.toma.gunsrpg.common.capability.object.PlayerSkills;
import dev.toma.gunsrpg.common.skills.CraftingSkill;
import dev.toma.gunsrpg.common.skills.core.SkillType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;

@SuppressWarnings("unchecked")
public class SkillUtil {

    public static int getGunpowderCraftAmount(EntityPlayer player) {
        return getCraftingAmount(PlayerDataFactory.get(player).getSkills(), new SkillType[] {ModRegistry.Skills.GUNPOWDER_MASTER, ModRegistry.Skills.GUNPOWDER_EXPERT, ModRegistry.Skills.GUNPOWDER_NOVICE});
    }

    public static float getAxeSpeedModifier(float input, ItemAxe axe, EntityPlayer player) {
        return input;
    }

    public static float getPickaxeSpeedModifier(float input, ItemPickaxe pickaxe, EntityPlayer player) {
        return input;
    }

    private static int getCraftingAmount(PlayerSkills skills, SkillType<? extends CraftingSkill>[] array) {
        for (SkillType<? extends CraftingSkill> skillType : array) {
            int count = getOutputAmount(skills, skillType);
            if (count > 0) {
                return count;
            }
        }
        return 0;
    }

    private static int getOutputAmount(PlayerSkills skills, SkillType<? extends CraftingSkill> type) {
        if(skills.hasSkill(type)) {
            return skills.getSkill(type).getOutputAmount();
        }
        return -1;
    }
}
