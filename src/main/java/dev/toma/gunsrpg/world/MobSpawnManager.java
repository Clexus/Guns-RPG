package dev.toma.gunsrpg.world;

import dev.toma.gunsrpg.ai.BeAngryDuringBloodmoonGoal;
import dev.toma.gunsrpg.common.entity.BloodmoonGolemEntity;
import dev.toma.gunsrpg.common.entity.RocketAngelEntity;
import dev.toma.gunsrpg.common.init.ModEntities;
import dev.toma.gunsrpg.config.ModConfig;
import dev.toma.gunsrpg.util.ModUtils;
import dev.toma.gunsrpg.util.object.Pair;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.monster.BlazeEntity;
import net.minecraft.entity.monster.CaveSpiderEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.WitherSkeletonEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.*;
import java.util.function.BiFunction;

public class MobSpawnManager {

    private static final UUID HEALTH_BOOST_UUID = UUID.fromString("80096B27-0A64-47FF-A22A-06146FC42448");
    private static final MobSpawnManager INSTANCE = new MobSpawnManager();
    private final List<EntityType<?>> healthExlusions = new ArrayList<>();
    private final Map<EntityType<?>, BooleanConsumer<? extends Entity>> postSpawn = new HashMap<>();
    private final Map<EntityType<?>, List<Pair<Integer, BiFunction<ServerWorld, Vector3d, LivingEntity>>>> bloodmoonEntries = new HashMap<>();
    private final AttributeModifier health2x = new AttributeModifier(HEALTH_BOOST_UUID, "health2x", 1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private final AttributeModifier health3x = new AttributeModifier(HEALTH_BOOST_UUID, "health3x", 2.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private final AttributeModifier health4x = new AttributeModifier(HEALTH_BOOST_UUID, "health4x", 3.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private final AttributeModifier bloodmoon_mobSpeed = new AttributeModifier(UUID.fromString("83B9F7EF-7A89-4C66-A62A-51AC71DB27E8"), "gunsrpg:bloodmoon_mobSpeed", 0.30D, AttributeModifier.Operation.MULTIPLY_TOTAL);

    public static MobSpawnManager instance() {
        return INSTANCE;
    }

    public void initialize() {
        healthExlusions.add(ModEntities.ROCKET_ANGEL.get());
        healthExlusions.add(ModEntities.BLOODMOON_GOLEM.get());
        healthExlusions.add(EntityType.ENDER_DRAGON);
        healthExlusions.add(ModEntities.GOLD_DRAGON.get());
        healthExlusions.add(EntityType.WITHER);
        healthExlusions.add(EntityType.IRON_GOLEM);
        registerBloodmoonEntry(EntityType.SPIDER, 7, (world, vec3d) -> {
            CaveSpiderEntity spider = new CaveSpiderEntity(EntityType.CAVE_SPIDER, world);
            spider.setPos(vec3d.x, vec3d.y, vec3d.z);
            return spider;
        });
        registerBloodmoonEntry(EntityType.SPIDER, ModConfig.worldConfig.rocketAngelSpawnChance.get(), (world, vec3d) -> {
            RocketAngelEntity rocketAngel = new RocketAngelEntity(world);
            rocketAngel.setPos(vec3d.x, vec3d.y, vec3d.z);
            return rocketAngel;
        });
        registerBloodmoonEntry(EntityType.ZOMBIE, 4, (world, vec3d) -> {
            BlazeEntity blaze = new BlazeEntity(EntityType.BLAZE, world);
            blaze.setPos(vec3d.x, vec3d.y, vec3d.z);
            return blaze;
        });
        registerBloodmoonEntry(EntityType.ZOMBIE, 4, (world, vec3d) -> {
            BloodmoonGolemEntity golem = new BloodmoonGolemEntity(world);
            golem.setPos(vec3d.x, vec3d.y, vec3d.z);
            return golem;
        });
        registerBloodmoonEntry(EntityType.SKELETON, 10, (world, vec3d) -> {
            WitherSkeletonEntity witherSkeleton = new WitherSkeletonEntity(EntityType.WITHER_SKELETON, world);
            witherSkeleton.setPos(vec3d.x, vec3d.y, vec3d.z);
            witherSkeleton.finalizeSpawn(world, world.getCurrentDifficultyAt(new BlockPos(vec3d.x, vec3d.y, vec3d.z)), SpawnReason.NATURAL, null, null);
            return witherSkeleton;
        });
        registerPostSpawnAction(EntityType.CREEPER, (bloodmoon, creeper) -> {
            if (creeper.level.isClientSide || !bloodmoon) return;
            if (creeper.getRandom().nextFloat() <= 0.2F) {
                LightningBoltEntity lightningBolt = new LightningBoltEntity(EntityType.LIGHTNING_BOLT, creeper.level);
                lightningBolt.setDamage(0);
                creeper.thunderHit((ServerWorld) creeper.level, lightningBolt);
                creeper.clearFire();
                creeper.setHealth(creeper.getMaxHealth());
            }
            creeper.maxSwell = 10;
        });
    }

    @SuppressWarnings("unchecked")
    public boolean processSpawn(LivingEntity entity, ServerWorld world, boolean isBloodmoon) {
        AttributeModifierManager manager = entity.getAttributes();
        boolean enemyEntity = entity instanceof MonsterEntity || entity instanceof IAngerable;
        if (isBloodmoon) {
            List<Pair<Integer, BiFunction<ServerWorld, Vector3d, LivingEntity>>> list = ModUtils.getNonnullFromMap(bloodmoonEntries, entity.getType(), Collections.emptyList());
            Random random = world.getRandom();
            Vector3d vec3d = entity.position();
            for (Pair<Integer, BiFunction<ServerWorld, Vector3d, LivingEntity>> pair : list) {
                if (random.nextInt(20) < pair.getLeft()) {
                    entity.remove();
                    LivingEntity replacement = pair.getRight().apply(world, vec3d);
                    world.addFreshEntity(replacement);
                    return false;
                }
            }

            if (enemyEntity) {
                ModifiableAttributeInstance speedAttr = manager.getInstance(Attributes.MOVEMENT_SPEED);
                speedAttr.addTransientModifier(bloodmoon_mobSpeed);
            }
        }
        if (isExluded(entity)) {
            return true;
        }
        ModifiableAttributeInstance instance = manager.getInstance(Attributes.MAX_HEALTH);
        instance.removeModifier(HEALTH_BOOST_UUID);
        AttributeModifier modifier = getRandomModifier(world.getRandom());
        if (modifier != null) {
            instance.addTransientModifier(modifier);
            entity.setHealth(entity.getMaxHealth());
        }
        BooleanConsumer<Entity> consumer = (BooleanConsumer<Entity>) postSpawn.get(entity.getType());
        if (consumer != null) {
            consumer.acceptBoolean(isBloodmoon, entity);
        }
        if (entity instanceof MobEntity && entity instanceof IAngerable) {
            addBloodmoonAggroGoal((MobEntity & IAngerable) entity);
        }
        return true;
    }

    private boolean isExluded(Entity entity) {
        return healthExlusions.contains(entity.getType());
    }

    private <T extends Entity> void registerPostSpawnAction(EntityType<T> type, BooleanConsumer<T> action) {
        postSpawn.put(type, action);
    }

    private void registerBloodmoonEntry(EntityType<?> type, int chance, BiFunction<ServerWorld, Vector3d, LivingEntity> replacement) {
        List<Pair<Integer, BiFunction<ServerWorld, Vector3d, LivingEntity>>> list = bloodmoonEntries.computeIfAbsent(type, cl -> new ArrayList<>());
        list.add(Pair.of(Math.max(1, chance), replacement));
    }

    private AttributeModifier getRandomModifier(Random random) {
        float f = random.nextFloat();
        return f <= 0.04F ? health4x : f <= 0.20F ? health3x : f <= 0.50F ? health2x : null;
    }

    private <E extends MobEntity & IAngerable> void addBloodmoonAggroGoal(E entity) {
        entity.targetSelector.addGoal(1, new BeAngryDuringBloodmoonGoal(entity));
    }

    @FunctionalInterface
    public interface BooleanConsumer<T> {
        void acceptBoolean(boolean bool, T t);
    }
}
