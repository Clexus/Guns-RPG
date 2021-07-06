package dev.toma.gunsrpg.common.entity;

import dev.toma.gunsrpg.common.init.GRPGEntityTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.EnumSet;

public class RocketAngelEntity extends MonsterEntity {

    public RocketAngelEntity(World world) {
        this(GRPGEntityTypes.ROCKET_ANGEL.get(), world);
    }

    public RocketAngelEntity(EntityType<? extends MonsterEntity> type, World world) {
        super(type, world);
        xpReward = 8;
        moveControl = new MoveController(this);
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return createMonsterAttributes().add(Attributes.MAX_HEALTH, 45.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
        noPhysics = false;
        setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(4, new ArrowAttack(this));
        this.goalSelector.addGoal(8, new MoveRandom());
        this.goalSelector.addGoal(9, new LookAtGoal(this, PlayerEntity.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtGoal(this, LivingEntity.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, false));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    public boolean doHurtTarget(Entity entityIn) {
        float f = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean flag = entityIn.hurt(DamageSource.mobAttack(this), f);
        if (flag) {
            Vector3d look = getLookAngle();
            Vector3d knockback = look.scale(3);
            entityIn.setDeltaMovement(knockback.x, Math.abs(knockback.y), knockback.z);
        }
        return flag;
    }

    static class ArrowAttack extends Goal {

        private final RocketAngelEntity entity;
        private int toFire;
        private int cooldown;

        public ArrowAttack(RocketAngelEntity entity) {
            this.entity = entity;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = entity.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            toFire = 4;
            cooldown = 15;
        }

        @Override
        public void stop() {
            cooldown = Integer.MAX_VALUE;
        }

        @Override
        public void tick() {
            --cooldown;
            LivingEntity target = entity.getTarget();
            double distance = entity.distanceToSqr(target);
            if (distance < 4.0D) {
                if (cooldown <= 0) {
                    cooldown = 10;
                    entity.doHurtTarget(target);
                }
            } else if (distance < (getFollowDistance() * getFollowDistance()) / 1.5F) {
                this.entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
                int heightDiff = (int)(entity.getY() - target.getY());
                if(heightDiff < 10) {
                    Vector3d currentMovement = entity.getDeltaMovement();
                    entity.setDeltaMovement(currentMovement.x, 0.2, currentMovement.z);
                }
                if(cooldown <= 0) {
                    cooldown = 6;
                    EntityExplosiveArrow arrow = new EntityExplosiveArrow(entity.level, entity, 1);
                    double x = target.getX() - entity.getX();
                    double y = target.getY() - arrow.getY();
                    double z = target.getZ() - entity.getZ();
                    double dist = MathHelper.sqrt(x * x + z * z);
                    arrow.shoot(x, y + dist * 0.2D, z, 1.6F, (float) (23 - entity.level.getDifficulty().getId() * 4));
                    entity.level.addFreshEntity(arrow);
                    --toFire;
                    if(toFire < 0) {
                        toFire = 4;
                        cooldown = 100;
                    }
                }
            } else {
                entity.getNavigation().stop();
                this.entity.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 0.4D);
            }
        }

        private double getFollowDistance() {
            double dist = this.entity.getAttributeValue(Attributes.FOLLOW_RANGE);
            return dist == 0 ? 16.0D : dist;
        }
    }

    class MoveRandom extends Goal {

        public MoveRandom() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !RocketAngelEntity.this.getMoveControl().hasWanted() && RocketAngelEntity.this.random.nextInt(7) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void tick() {
            BlockPos blockpos = RocketAngelEntity.this.getOnPos();
            for (int i = 0; i < 3; ++i) {
                BlockPos blockpos1 = blockpos.offset(RocketAngelEntity.this.random.nextInt(15) - 7, RocketAngelEntity.this.random.nextInt(11) - 5, RocketAngelEntity.this.random.nextInt(15) - 7);
                if (RocketAngelEntity.this.level.isEmptyBlock(blockpos1)) {
                    RocketAngelEntity.this.moveControl.setWantedPosition((double) blockpos1.getX() + 0.5D, (double) blockpos1.getY() + 0.5D, (double) blockpos1.getZ() + 0.5D, 0.25D);
                    if (RocketAngelEntity.this.getTarget() == null) {
                        RocketAngelEntity.this.getLookControl().setLookAt((double) blockpos1.getX() + 0.5D, (double) blockpos1.getY() + 0.5D, (double) blockpos1.getZ() + 0.5D, 180.0F, 20.0F);
                    }
                    break;
                }
            }
        }
    }

    class MoveController extends MovementController {

        public MoveController(RocketAngelEntity angel) {
            super(angel);
        }

        @Override
        public void tick() {
            if (this.operation == Action.MOVE_TO) {
                double x = this.getWantedX() - RocketAngelEntity.this.getX();
                double y = this.getWantedY() - RocketAngelEntity.this.getY();
                double z = this.getWantedZ() - RocketAngelEntity.this.getZ();
                double dist = x * x + y * y + z * z;
                dist = MathHelper.sqrt(dist);

                if (dist < RocketAngelEntity.this.getBoundingBox().getSize()) {
                    this.operation = Action.WAIT;
                    RocketAngelEntity.this.setDeltaMovement(RocketAngelEntity.this.getDeltaMovement().scale(0.5D));
                } else {
                    RocketAngelEntity.this.setDeltaMovement(RocketAngelEntity.this.getDeltaMovement().add(x / dist * 0.05 * speedModifier, y / dist * 0.05 * speedModifier, z / dist * 0.05 * speedModifier));

                    if (RocketAngelEntity.this.getTarget() == null) {
                        Vector3d vector3d1 = RocketAngelEntity.this.getDeltaMovement();
                        RocketAngelEntity.this.yRot = -((float)MathHelper.atan2(vector3d1.x, vector3d1.z)) * (180F / (float)Math.PI);
                    } else {
                        double px = RocketAngelEntity.this.getTarget().getX() - RocketAngelEntity.this.getX();
                        double pz = RocketAngelEntity.this.getTarget().getZ() - RocketAngelEntity.this.getZ();
                        RocketAngelEntity.this.yRot = -((float)MathHelper.atan2(px, pz)) * (180F / (float)Math.PI);
                    }
                    RocketAngelEntity.this.yBodyRot = RocketAngelEntity.this.yRot;
                }
            }
        }
    }
}
