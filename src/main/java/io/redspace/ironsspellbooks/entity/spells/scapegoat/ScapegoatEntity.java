package io.redspace.ironsspellbooks.entity.spells.scapegoat;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

public class ScapegoatEntity extends PathfinderMob implements AntiMagicSusceptible, GeoEntity {
    class RunToTargetGoal extends Goal {
        int pathingDelay;

        @Override
        public boolean canUse() {
            return targetPos != null && ScapegoatEntity.this.distanceToSqr(ScapegoatEntity.this.targetPos.getCenter()) > 1.5 * 1.5;
        }

        @Override
        public void tick() {
            if (targetPos == null) {
                // logically impossible
                return;
            }
            var path = ScapegoatEntity.this.navigation.getPath();
            if (path != null && path.isDone() || (path == null && (pathingDelay-- <= 0 || tickCount < 20))) {
                pathingDelay = 30;
                Vec3 flee = ScapegoatEntity.this.targetPos.getCenter();
//                Vec3 flee = Optional.ofNullable(DefaultRandomPos.getPosTowards(ScapegoatEntity.this, 16, 7, ScapegoatEntity.this.targetPos.getCenter(), 1f)).orElse(ScapegoatEntity.this.targetPos.getCenter());
                ScapegoatEntity.this.getNavigation().moveTo(flee.x, flee.y, flee.z, 1);
            }
        }

        @Override
        public void stop() {
            ScapegoatEntity.this.navigation.stop();
        }
    }

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private Entity cachedOwner;

    @Nullable
    BlockPos targetPos;

    int jumpDelay;

    public ScapegoatEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public void setTargetPos(BlockPos targetPos) {
        this.targetPos = targetPos;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new RunToTargetGoal());
    }

    public void setOwner(@Nullable Entity owner) {
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
            this.cachedOwner = owner;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (jumpDelay > 0) {
            jumpDelay--;
        } else if (jumpDelay == 0) {
            jumpDelay = 20;
            this.playSound(SoundEvents.GOAT_DEATH, 2f, 1f + random.nextFloat() * .2f);
            if (level.isClientSide) {
                animationToPlay = RawAnimation.begin().thenPlay("scapegoat_run_accent");
            }
        }
    }

    @Nullable
    public Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        } else if (this.ownerUUID != null && this.level instanceof ServerLevel serverlevel) {
            this.cachedOwner = serverlevel.getEntity(this.ownerUUID);
            return this.cachedOwner;
        } else {
            return null;
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        //todo: poof
        // custom sparkly particles would be funny
    }

    @Override
    public void die(DamageSource damageSource) {
        //todo: poof
        super.die(damageSource);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.ownerUUID != null) {
            compound.putUUID("Owner", this.ownerUUID);
        }
        if (this.targetPos != null) {
            compound.put("scapegoat_target", NbtUtils.writeBlockPos(this.targetPos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("Owner")) {
            this.ownerUUID = compound.getUUID("Owner");
            this.cachedOwner = null;
        }
        NbtUtils.readBlockPos(compound, "scapegoat_target").ifPresent(p_325838_ -> this.targetPos = p_325838_);
    }

    /*
     Geckolib Impl
     */
    RawAnimation animationToPlay = null;

    private PlayState animationPredicate(AnimationState<ScapegoatEntity> event) {
        var controller = event.getController();
        if (this.animationToPlay != null) {
            controller.forceAnimationReset();
            controller.setAnimation(animationToPlay);
            animationToPlay = null;
        }
        return PlayState.CONTINUE;
    }

    private final RawAnimation ANIMATION = RawAnimation.begin().thenPlay("emerge");

    private final AnimationController controller = new AnimationController(this, "controller", 0, this::animationPredicate);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
}
