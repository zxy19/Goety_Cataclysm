package com.Polarice3.goety_cataclysm.common.entities.projectiles;

import com.Polarice3.Goety.utils.MobUtil;
import com.Polarice3.goety_cataclysm.common.entities.GCEntityType;
import com.github.L_Ender.cataclysm.client.particle.CircleLightningParticle;
import com.github.L_Ender.cataclysm.client.particle.Not_Spin_TrailParticle;
import com.github.L_Ender.cataclysm.entity.effect.ScreenShake_Entity;
import com.github.L_Ender.cataclysm.init.ModEffect;
import com.github.L_Ender.cataclysm.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.*;

public class StormSerpent extends Entity {
    public int warmupDelayTicks;
    private boolean sentSpikeEvent;
    public int lifeTicks;
    private boolean clientSideAttackStarted;
    private LivingEntity caster;
    private UUID casterUuid;
    @Nullable
    public Entity finalTarget;
    @Nullable
    public UUID targetId;
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(StormSerpent.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(StormSerpent.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RIGHT = SynchedEntityData.defineId(StormSerpent.class, EntityDataSerializers.BOOLEAN);
    public AnimationState SpawnAnimationState = new AnimationState();
    public AnimationState Spawn2AnimationState = new AnimationState();

    public StormSerpent(EntityType<? extends StormSerpent> p_i50170_1_, Level p_i50170_2_) {
        super(p_i50170_1_, p_i50170_2_);
    }

    public StormSerpent(Level worldIn, double x, double y, double z, float p_i47276_8_, int p_i47276_9_, LivingEntity casterIn, float damage, @Nullable LivingEntity finalTarget, boolean right) {
        this(GCEntityType.STORM_SERPENT.get(), worldIn);
        this.warmupDelayTicks = p_i47276_9_;
        this.setCaster(casterIn);
        this.finalTarget = finalTarget;
        this.setYRot(p_i47276_8_ * (180F / (float)Math.PI));
        this.setDamage(damage);
        this.setPos(x, y, z);
        this.setRight(right);
    }

    protected Component getTypeName() {
        return ModEntities.STORM_SERPENT.get().getDescription();
    }

    protected void defineSynchedData() {
        this.entityData.define(STATE,0);
        this.entityData.define(DAMAGE,0f);
        this.entityData.define(RIGHT,false);
    }

    public AnimationState getAnimationState(String input) {
        if (Objects.equals(input, "spawn")) {
            return this.SpawnAnimationState;
        }else if (Objects.equals(input, "spawn2")) {
            return this.Spawn2AnimationState;
        }else {
            return new AnimationState();
        }
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> p_21104_) {
        if (STATE.equals(p_21104_)) {
            if (this.level().isClientSide)
                switch (this.getState()) {
                    case 0 -> this.stopAllAnimationStates();
                    case 1 -> {
                        stopAllAnimationStates();
                        this.SpawnAnimationState.startIfStopped(this.tickCount);
                    }
                    case 2 -> {
                        stopAllAnimationStates();
                        this.Spawn2AnimationState.startIfStopped(this.tickCount);
                    }

                }
        }

        super.onSyncedDataUpdated(p_21104_);
    }

    public void stopAllAnimationStates() {
        this.SpawnAnimationState.stop();
        this.Spawn2AnimationState.stop();
    }

    public int getState() {
        return entityData.get(STATE);
    }

    public void setState(int state) {
        entityData.set(STATE, state);
    }

    public boolean getRight() {
        return entityData.get(RIGHT);
    }

    public void setRight(boolean right) {
        entityData.set(RIGHT, right);
    }

    public float getDamage() {
        return entityData.get(DAMAGE);
    }

    public void setDamage(float damage) {
        entityData.set(DAMAGE, damage);
    }

    public void setCaster(@Nullable LivingEntity p_190549_1_) {
        this.caster = p_190549_1_;
        this.casterUuid = p_190549_1_ == null ? null : p_190549_1_.getUUID();
    }

    @Nullable
    public LivingEntity getCaster() {
        if (this.caster == null && this.casterUuid != null && this.level() instanceof ServerLevel) {
            Entity entity = ((ServerLevel)this.level()).getEntity(this.casterUuid);
            if (entity instanceof LivingEntity) {
                this.caster = (LivingEntity)entity;
            }
        }

        return this.caster;
    }

    protected void readAdditionalSaveData(CompoundTag compound) {
        this.warmupDelayTicks = compound.getInt("Warmup");
        if (compound.hasUUID("Owner")) {
            this.casterUuid = compound.getUUID("Owner");
        }
        if (compound.hasUUID("Target")) {
            this.targetId = compound.getUUID("Target");
        }
        this.setDamage(compound.getFloat("Damage"));
    }

    protected void addAdditionalSaveData(CompoundTag compound) {
        if (this.finalTarget != null) {
            compound.putUUID("Target", this.finalTarget.getUUID());
        }
        compound.putInt("Warmup", this.warmupDelayTicks);
        if (this.casterUuid != null) {
            compound.putUUID("Owner", this.casterUuid);
        }
        compound.putFloat("Damage", this.getDamage());
    }

    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            if (this.clientSideAttackStarted) {
                ++this.lifeTicks;
                if (this.lifeTicks == 10) {
                    for (int i = 0; i < 12; i++) {
                        float angle = (0.01745329251F * this.getYRot()) + i;
                        float r = 0.5F + random.nextFloat() * 0.9F;
                        float velocity = r + random.nextFloat() * 4F;
                        float x = (float) ( r * Mth.sin((float) (Math.PI + angle)));
                        float z = (float) (r * Mth.cos(angle));

                        double d0 =this.getX() + x;
                        double d1 =this.getY() + 0.1;
                        double d2 =this.getZ() + z;
                        double extraX = d0 + velocity * 0.4F * Mth.sin((float) (Math.PI + angle));
                        double extraY = d1+ 0.3F + random.nextFloat() * 2F;
                        double extraZ = d2 + velocity * 0.4F * Mth.cos(angle);

                        this.level().addParticle(new Not_Spin_TrailParticle.NSTData(113/255F, 194/255F, 240/255F,0.05F,0.75F,0.5F, 0,60 + random.nextInt(40)), d0, d1, d2, extraX, extraY, extraZ);
                    }
                }

                if (this.lifeTicks > 12 && this.lifeTicks < 18) {
                    for (int i = 0; i < 5; i++) {
                        this.level().addParticle((new CircleLightningParticle.CircleData(113,194,240)), this.getX(), this.getY() + 0.1, this.getZ(),   this.getX() + (random.nextFloat() - 0.5F) * 7, this.getY() + 0.1,  this.getZ() + (random.nextFloat() - 0.5F) * 7);
                    }
                }

                if (this.lifeTicks == 52 ) {
                    for (int i = 0; i < 12; i++) {
                        double theta = Math.toRadians(this.getYRot());
                        double vecX = Math.cos(theta) * 8;
                        double vecZ = Math.sin(theta) * 8;

                        float angle = (0.01745329251F * this.getYRot()) + i;
                        float r = 0.5F + random.nextFloat() * 0.9F;
                        float velocity = r + random.nextFloat() * 4F;
                        float x = (float) (vecX + r * Mth.sin((float) (Math.PI + angle)));
                        float z = (float) (vecZ + r * Mth.cos(angle));

                        double d0 =this.getX() + x;
                        double d1 =this.getY() + 0.1;
                        double d2 =this.getZ() + z;
                        double extraX = d0 + velocity * 0.6F * Mth.sin((float) (Math.PI + angle));
                        double extraY = d1+ 0.3F + random.nextFloat() * 1.2F;
                        double extraZ = d2 + velocity * 0.6F * Mth.cos(angle);

                        this.level().addParticle(new Not_Spin_TrailParticle.NSTData(113/255F, 194/255F, 240/255F,0.05F,0.75F,0.5F, 0,80 + random.nextInt(40)), d0, d1, d2, extraX, extraY, extraZ);
                    }
                }

                if (this.lifeTicks > 52 && this.lifeTicks < 56) {
                    for (int i = 0; i < 5; i++) {
                        double theta = Math.toRadians(this.getYRot());
                        double vecX = Math.cos(theta) * 8;
                        double vecZ = Math.sin(theta) * 8;

                        this.level().addParticle((new CircleLightningParticle.CircleData(113,194,240)), this.getX() +vecX, this.getY() + 0.1, this.getZ()+vecZ,this.getX() + vecX + (random.nextFloat() - 0.5F) * 7, this.getY() + 0.1,  this.getZ() + vecZ + (random.nextFloat() - 0.5F) * 7);
                    }
                }
            }
        } else if (--this.warmupDelayTicks < 0) {
            if (this.warmupDelayTicks == -10) {
                if(getState() == 0) {
                    this.setState(!getRight() ?1 : 2);
                }
            }
            if (this.finalTarget == null || !this.finalTarget.isAlive()) {
                if (this.targetId != null) {
                    this.finalTarget = ((ServerLevel) this.level()).getEntity(this.targetId);
                    if (this.finalTarget == null) {
                        this.targetId = null;
                    }
                } else {
                    List<LivingEntity> list = new ArrayList<>();
                    for (Entity entity1 : this.level().getEntitiesOfClass(Entity.class, this.getBoundingBox().inflate(16.0F))) {
                        LivingEntity livingEntity = null;
                        if (entity1 instanceof PartEntity<?> partEntity && partEntity.getParent() instanceof LivingEntity living){
                            livingEntity = living;
                        } else if (entity1 instanceof LivingEntity living){
                            livingEntity = living;
                        }
                        if (livingEntity != null) {
                            if (MobUtil.ownedPredicate(this).test(livingEntity)){
                                list.add(livingEntity);
                            }
                        }
                    }
                    list.sort(Comparator.comparingDouble(this::distanceTo));
                    if (list.stream().findFirst().isPresent()){
                        LivingEntity livingEntity = list.stream().findFirst().get();
                        this.finalTarget = livingEntity;
                        this.targetId = livingEntity.getUUID();
                    }
                }
            }
            if (this.finalTarget != null && this.finalTarget.isAlive() && this.warmupDelayTicks > -48) {
                this.lookAt(this.finalTarget, 30, 0);
            }

            if (this.warmupDelayTicks == -52 || this.warmupDelayTicks == -53 || this.warmupDelayTicks == -54 || this.warmupDelayTicks == -55) {
                double theta = Math.toRadians(this.getYRot());

                double vecX = this.getX() + Math.cos(theta) * 8;
                double vecZ = this.getZ() + Math.sin(theta) * 8;
                AABB selection = new AABB(vecX - 1.5,this.getY()- 2,vecZ - 1.5, vecX + 1.5,this.getY() + this.getBbHeight(),vecZ + 1.5);
                for(LivingEntity livingentity : this.level().getEntitiesOfClass(LivingEntity.class, selection)) {
                    this.damage(livingentity);
                }
                Vec3 vec3 = new Vec3(vecX, this.getY(), vecZ);
                ScreenShake_Entity.ScreenShake(level(), vec3, 10, 0.07f, 0, 20);
            }

            if (!this.sentSpikeEvent) {
                this.level().broadcastEntityEvent(this, (byte)4);
                this.sentSpikeEvent = true;
            }

            if (++this.lifeTicks > 70) {
                this.discard();
            }
        }

    }

    protected void lookAt(Entity entity, float maxYRotIncrease, float maxXRotIncrease) {
        double d0 = entity.getX() - this.getX();
        double d2 = entity.getZ() - this.getZ();
        double d1;
        if (entity instanceof LivingEntity livingentity) {
            d1 = livingentity.getEyeY() - this.getEyeY();
        } else {
            d1 = (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0 - this.getEyeY();
        }

        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        float f = (float)(Mth.atan2(d2, d0) * 180.0F / (float)Math.PI) ;
        float f1 = (float)(-(Mth.atan2(d1, d3) * 180.0F / (float)Math.PI));
        this.setXRot(this.rotlerp(this.getXRot(), f1, maxXRotIncrease));
        this.setYRot(this.rotlerp(this.getYRot(), f, maxYRotIncrease));
    }

    private float rotlerp(float angle, float targetAngle, float maxIncrease) {
        float f = Mth.wrapDegrees(targetAngle - angle);
        if (f > maxIncrease) {
            f = maxIncrease;
        }

        if (f < -maxIncrease) {
            f = -maxIncrease;
        }

        return angle + f;
    }


    protected void damage(LivingEntity Hitentity) {
        LivingEntity livingentity = this.getCaster();
        if (Hitentity.isAlive() && !Hitentity.isInvulnerable() && Hitentity != livingentity) {

            if (livingentity == null) {
                Hitentity.hurt(this.damageSources().magic(), getDamage());
            } else {
                if (livingentity.isAlliedTo(Hitentity)) {
                    return;
                }
                boolean flag = Hitentity.hurt(this.damageSources().indirectMagic(this,livingentity), getDamage());
                if(flag ){
                    MobEffectInstance effectinstance = new MobEffectInstance(ModEffect.EFFECTWETNESS.get(), 150, 4, false, true, true);
                    Hitentity.addEffect(effectinstance);
                }
            }

        }
    }

    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        if (id == 4) {
            this.clientSideAttackStarted = true;
        }

    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
