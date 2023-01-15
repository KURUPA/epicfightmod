package yesman.epicfight.world.capabilities.entitypatch;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.ServerAnimator;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.AttackResult.ResultType;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPPlayAnimation;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributeSupplier;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;
import yesman.epicfight.world.gamerule.EpicFightGamerules;

public abstract class LivingEntityPatch<T extends LivingEntity> extends HurtableEntityPatch<T> {
	public static final EntityDataAccessor<Float> STUN_SHIELD = new EntityDataAccessor<Float> (251, EntityDataSerializers.FLOAT);
	public static final EntityDataAccessor<Float> MAX_STUN_SHIELD = new EntityDataAccessor<Float> (252, EntityDataSerializers.FLOAT);
	
	private ResultType lastResultType;
	private float lastDealDamage;
	
	protected Entity lastTryHurtEntity;
	protected Entity entityBeingInteract;
	protected Armature armature;
	protected EntityState state = EntityState.DEFAULT;
	protected Animator animator;
	protected Vec3 lastAttackPosition;
	protected EpicFightDamageSource animationDamageSource;
	protected boolean airborne;
	protected boolean isLastAttackSuccess;
	protected List<LivingEntity> currentlyAttackedEntity;
	
	public LivingMotion currentLivingMotion = LivingMotions.IDLE;
	public LivingMotion currentCompositeMotion = LivingMotions.IDLE;
	
	@Override
	public void onConstructed(T entityIn) {
		super.onConstructed(entityIn);
		
		this.armature = Armatures.getArmatureFor(this);
		this.animator = EpicFightMod.getAnimator(this);
		this.animator.init();
		this.currentlyAttackedEntity = Lists.newArrayList();
		this.original.getEntityData().define(STUN_SHIELD, Float.valueOf(0.0F));
		this.original.getEntityData().define(MAX_STUN_SHIELD, Float.valueOf(0.0F));
	}
	
	@Override
	public void onJoinWorld(T entityIn, EntityJoinWorldEvent event) {
		super.onJoinWorld(entityIn, event);
		this.original.getAttributes().supplier = new EpicFightAttributeSupplier(this.original.getAttributes().supplier);
		this.initAttributes();
	}
	
	@OnlyIn(Dist.CLIENT)
	public abstract void initAnimator(ClientAnimator clientAnimator);
	public abstract void updateMotion(boolean considerInaction);
	
	public Armature getArmature() {
		return this.armature;
	}
	
	protected void initAttributes() {
		this.original.getAttribute(EpicFightAttributes.WEIGHT.get()).setBaseValue(this.original.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * 2.0D);
		this.original.getAttribute(EpicFightAttributes.MAX_STRIKES.get()).setBaseValue(1.0D);
		this.original.getAttribute(EpicFightAttributes.ARMOR_NEGATION.get()).setBaseValue(0.0D);
		this.original.getAttribute(EpicFightAttributes.IMPACT.get()).setBaseValue(0.5D);
	}
	
	@Override
	public void tick(LivingUpdateEvent event) {
		this.animator.tick();
		
		super.tick(event);
		
		if (this.original.deathTime == 19) {
			this.aboutToDeath();
		}
	}
	
	public void onFall(LivingFallEvent event) {
		if (!this.getOriginal().level.isClientSide() && (this.airborne || (this.getOriginal().level.getGameRules().getBoolean(EpicFightGamerules.HAS_FALL_ANIMATION) && event.getDamageMultiplier() > 0.0F)
				  && !this.getEntityState().inaction())) {
			
			if (this.airborne || event.getDistance() > 5.0F) {
				StaticAnimation fallAnimation = this.getAnimator().getLivingAnimation(LivingMotions.LANDING_RECOVERY, this.getHitAnimation(StunType.FALL));
				
				if (fallAnimation != null) {
					this.playAnimationSynchronized(fallAnimation, 0);
				}
			}
		}
		
		this.airborne = false;
	}
	
	public void onDeath() {
		this.getAnimator().playDeathAnimation();
		this.currentLivingMotion = LivingMotions.DEATH;
	}
	
	public void updateEntityState() {
		this.state = this.animator.getEntityState();
	}
	
	public void cancelUsingItem() {
		this.original.stopUsingItem();
		ForgeEventFactory.onUseItemStop(this.original, this.original.getUseItem(), this.original.getUseItemRemainingTicks());
	}
	
	public CapabilityItem getHoldingItemCapability(InteractionHand hand) {
		return EpicFightCapabilities.getItemStackCapability(this.original.getItemInHand(hand));
	}
	
	/**
	 * Returns an empty capability if the item in mainhand is incompatible with the item in offhand 
	 */
	public CapabilityItem getAdvancedHoldingItemCapability(InteractionHand hand) {
		if (hand == InteractionHand.MAIN_HAND) {
			return getHoldingItemCapability(hand);
		} else {
			return this.isOffhandItemValid() ? this.getHoldingItemCapability(hand) : CapabilityItem.EMPTY;
		}
	}
	
	public EpicFightDamageSource getDamageSource(StaticAnimation animation, InteractionHand hand) {
		EpicFightDamageSource damagesource = EpicFightDamageSource.commonEntityDamageSource("mob", this.original, animation);
		damagesource.setImpact(this.getImpact(hand));
		damagesource.setArmorNegation(this.getArmorNegation(hand));
		damagesource.setHurtItem(this.original.getItemInHand(hand));
		
		return damagesource;
	}
	
	public AttackResult tryHurt(DamageSource damageSource, float amount) {
		if (this.getEntityState().invulnerableTo(damageSource)) {
			return AttackResult.failed(amount);
		}
		
		return AttackResult.success(amount);
	}
	
	public AttackResult tryHarm(Entity target, EpicFightDamageSource damagesource, float amount) {
		LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
		AttackResult result = (entitypatch != null) ? entitypatch.tryHurt((DamageSource)damagesource, amount) : AttackResult.success(amount);
		
		return result;
	}
	
	public void setLastAttackResult(Entity tryHurtEntity, AttackResult attackResult) {
		this.lastTryHurtEntity = tryHurtEntity;
		this.lastResultType = attackResult.resultType;
		this.lastDealDamage = attackResult.damage;
	}
	
	@Nullable
	public EpicFightDamageSource getAnimationDamageSource() {
		return this.animationDamageSource;
	}
	
	public boolean checkAttackSuccess(Entity target) {
		boolean success = target.is(this.lastTryHurtEntity);
		this.lastTryHurtEntity = null;
		
		if (success && !this.isLastAttackSuccess) {
			this.setLastAttackSuccess(true);
		}
		
		return success;
	}
	
	/**
	 * This method swap the ATTACK_DAMAGE and OFFHAND_ATTACK_DAMAGE in a very unsafe way.
	 * You must call this method again after finishing the damaging process.
	 */
	protected void swapHandAttackDamage(boolean shouldSwap) {
		if (!shouldSwap) {
			return;
		}
		
		AttributeInstance mainhandDamage = this.original.getAttribute(Attributes.ATTACK_DAMAGE);
		AttributeInstance offhandDamage = this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_DAMAGE.get());
		
		this.original.getAttributes().attributes.put(Attributes.ATTACK_DAMAGE, offhandDamage);
		this.original.getAttributes().attributes.put(EpicFightAttributes.OFFHAND_ATTACK_DAMAGE.get(), mainhandDamage);
	}
	
	public AttackResult getLastAttackResult() {
		return new AttackResult(this.lastResultType, this.lastDealDamage);
	}
	
	public AttackResult attack(EpicFightDamageSource damageSource, Entity target, InteractionHand hand) {
		return this.checkAttackSuccess(target) ? this.getLastAttackResult() : AttackResult.failed(0.0F);
	}
	
	public float getModifiedBaseDamage(@Nullable Entity targetEntity, @Nullable EpicFightDamageSource source, float baseDamage) {
		return baseDamage;
	}
	
	public boolean onDrop(LivingDropsEvent event) {
		return false;
	}
	
	public void gatherDamageDealt(EpicFightDamageSource source, float amount) {}
	
	@Override
	public float getStunArmor() {
		AttributeInstance stunArmor = this.original.getAttribute(EpicFightAttributes.STUN_ARMOR.get());
		return (float) (stunArmor == null ? 0 : stunArmor.getValue());
	}
	
	@Override
	public float getStunShield() {
		return this.original.getEntityData().get(STUN_SHIELD).floatValue();
	}
	
	@Override
	public void setStunShield(float value) {
		value = Math.max(value, 0);
		this.original.getEntityData().set(STUN_SHIELD, value);
	}
	
	public float getMaxStunShield() {
		return this.original.getEntityData().get(MAX_STUN_SHIELD).floatValue();
	}
	
	public void setMaxStunShield(float value) {
		value = Math.max(value, 0);
		this.original.getEntityData().set(MAX_STUN_SHIELD, value);
	}
	
	@Override
	public float getWeight() {
		return (float)this.original.getAttributeValue(EpicFightAttributes.WEIGHT.get());
	}
	
	public void rotateTo(float degree, float limit, boolean synchronizeOld) {
		LivingEntity entity = this.getOriginal();
		float amount = degree - entity.getYRot();
		
        while (amount < -180.0F) {
        	amount += 360.0F;
        }
        
        while (amount > 180.0F) {
        	amount -= 360.0F;
        }
        
        amount = Mth.clamp(amount, -limit, limit);
        float f1 = entity.getYRot() + amount;
        
		if (synchronizeOld) {
			entity.yRotO = f1;
			entity.yHeadRotO = f1;
			entity.yBodyRotO = f1;
		}
		
		entity.setYRot(f1);
		entity.yHeadRot = f1;
		entity.yBodyRot = f1;
	}
	
	public void rotateTo(Entity target, float limit, boolean partialSync) {
		double d0 = target.getX() - this.original.getX();
        double d1 = target.getZ() - this.original.getZ();
        float degree = -(float)Math.toDegrees(Mth.atan2(d0, d1));
    	this.rotateTo(degree, limit, partialSync);
	}
	
	public void playSound(SoundEvent sound, float pitchModifierMin, float pitchModifierMax) {
		this.playSound(sound, 1.0F, pitchModifierMin, pitchModifierMax);
	}
	
	public void playSound(SoundEvent sound, float volume, float pitchModifierMin, float pitchModifierMax) {
		float pitch = (this.original.getRandom().nextFloat() * 2.0F - 1.0F) * (pitchModifierMax - pitchModifierMin);
		
		if (!this.isLogicalClient()) {
			this.original.level.playSound(null, this.original.getX(), this.original.getY(), this.original.getZ(), sound, this.original.getSoundSource(), volume, 1.0F + pitch);
		} else {
			this.original.level.playLocalSound(this.original.getX(), this.original.getY(), this.original.getZ(), sound, this.original.getSoundSource(), volume, 1.0F + pitch, false);
		}
	}
	
	public LivingEntity getTarget() {
		return this.original.getLastHurtMob();
	}
	
	public float getAttackDirectionPitch() {
		float partialTicks = EpicFightMod.isPhysicalClient() ? Minecraft.getInstance().getFrameTime() : 1.0F;
		float pitch = -this.getOriginal().getViewXRot(partialTicks);
		float correct = (pitch > 0) ? 0.03333F * (float)Math.pow(pitch, 2) : -0.03333F * (float)Math.pow(pitch, 2);
		return Mth.clamp(correct, -30.0F, 30.0F);
	}
	
	@OnlyIn(Dist.CLIENT)
	public OpenMatrix4f getHeadMatrix(float partialTicks) {
        float f2;
        
		if (this.state.inaction()) {
			f2 = 0;
		} else {
			float f = MathUtils.lerpBetween(this.original.yBodyRotO, this.original.yBodyRot, partialTicks);
			float f1 = MathUtils.lerpBetween(this.original.yHeadRotO, this.original.yHeadRot, partialTicks);
			f2 = f1 - f;
			
			if (this.original.getVehicle() != null) {
				if (f2 > 45.0F) {
					f2 = 45.0F;
				} else if (f2 < -45.0F) {
					f2 = -45.0F;
				}
			}
		}
		
		
		return MathUtils.getModelMatrixIntegral(0, 0, 0, 0, 0, 0, this.original.xRotO, this.original.getXRot(), f2, f2, partialTicks, 1, 1, 1);
	}
	
	@Override
	public OpenMatrix4f getModelMatrix(float partialTicks) {
		float prevYRot;
		float yRot;
		float scale = this.original.isBaby() ? 0.5F : 1.0F;
		
		if (this.original.getVehicle() instanceof LivingEntity) {
			LivingEntity ridingEntity = (LivingEntity) this.original.getVehicle();
			prevYRot = ridingEntity.yBodyRotO;
			yRot = ridingEntity.yBodyRot;
		} else {
			prevYRot = this.isLogicalClient() ? this.original.yBodyRotO : this.original.getYRot();
			yRot = this.isLogicalClient() ? this.original.yBodyRot : this.original.getYRot();
		}
		
		return MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, prevYRot, yRot, partialTicks, scale, scale, scale);
	}
	
	public void reserveAnimation(StaticAnimation animation) {
		this.animator.reserveAnimation(animation);
		EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(new SPPlayAnimation(animation, this.original.getId(), 0.0F), this.original);
	}
	
	public void playAnimationSynchronized(StaticAnimation animation, float convertTimeModifier) {
		this.playAnimationSynchronized(animation, convertTimeModifier, SPPlayAnimation::new);
	}
	
	public void playAnimationSynchronized(StaticAnimation animation, float convertTimeModifier, AnimationPacketProvider packetProvider) {
		this.animator.playAnimation(animation, convertTimeModifier);
		EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(packetProvider.get(animation, convertTimeModifier, this), this.original);
	}
	
	@FunctionalInterface
	public static interface AnimationPacketProvider {
		public SPPlayAnimation get(StaticAnimation animation, float convertTimeModifier, LivingEntityPatch<?> entitypatch);
	}
	
	protected void playReboundAnimation() {
		this.getClientAnimator().playReboundAnimation();
	}
	
	public void resetSize(EntityDimensions size) {
		EntityDimensions entitysize = this.original.dimensions;
		EntityDimensions entitysize1 = size;
		this.original.dimensions = entitysize1;
	    if (entitysize1.width < entitysize.width) {
	    	double d0 = (double)entitysize1.width / 2.0D;
	    	this.original.setBoundingBox(new AABB(original.getX() - d0, original.getY(), original.getZ() - d0, original.getX() + d0,
	    			original.getY() + (double)entitysize1.height, original.getZ() + d0));
	    } else {
	    	AABB axisalignedbb = this.original.getBoundingBox();
	    	this.original.setBoundingBox(new AABB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ, axisalignedbb.minX + (double)entitysize1.width,
	    			axisalignedbb.minY + (double)entitysize1.height, axisalignedbb.minZ + (double)entitysize1.width));
	    	
	    	if (entitysize1.width > entitysize.width && !original.level.isClientSide()) {
	    		float f = entitysize.width - entitysize1.width;
	        	this.original.move(MoverType.SELF, new Vec3((double)f, 0.0D, (double)f));
	    	}
	    }
    }
	
	@Override
	public void applyStun(StunType stunType, float stunTime) {
		this.cancelKnockback = true;
		StaticAnimation hitAnimation = this.getHitAnimation(stunType);
		
		if (hitAnimation != null) {
			this.playAnimationSynchronized(hitAnimation, stunType.hasFixedStunTime() ? 0.0F : stunTime);
		}
	}
	
	public void correctRotation() {
	}
	
	public void updateHeldItem(CapabilityItem fromCap, CapabilityItem toCap, ItemStack from, ItemStack to, InteractionHand hand) {
	}
	
	public void updateArmor(CapabilityItem fromCap, CapabilityItem toCap, EquipmentSlot slotType) {
	}
	
	public void onAttackBlocked(DamageSource damageSource, LivingEntityPatch<?> opponent) {
	}
	
	public void onMount(boolean isMountOrDismount, Entity ridingEntity) {
	}
	
	@SuppressWarnings("unchecked")
	public <A extends Animator> A getAnimator() {
		return (A) this.animator;
	}
	
	public ClientAnimator getClientAnimator() {
		return this.<ClientAnimator>getAnimator();
	}
	
	public ServerAnimator getServerAnimator() {
		return this.<ServerAnimator>getAnimator();
	}
	
	public abstract StaticAnimation getHitAnimation(StunType stunType);
	public void aboutToDeath() {}
	
	public SoundEvent getWeaponHitSound(InteractionHand hand) {
		return this.getAdvancedHoldingItemCapability(hand).getHitSound();
	}

	public SoundEvent getSwingSound(InteractionHand hand) {
		return this.getAdvancedHoldingItemCapability(hand).getSmashingSound();
	}
	
	public HitParticleType getWeaponHitParticle(InteractionHand hand) {
		return this.getAdvancedHoldingItemCapability(hand).getHitParticle();
	}

	public Collider getColliderMatching(InteractionHand hand) {
		return this.getAdvancedHoldingItemCapability(hand).getWeaponCollider();
	}

	public int getMaxStrikes(InteractionHand hand) {
		return (int) (hand == InteractionHand.MAIN_HAND ? this.original.getAttributeValue(EpicFightAttributes.MAX_STRIKES.get()) : 
			this.isOffhandItemValid() ? this.original.getAttributeValue(EpicFightAttributes.OFFHAND_MAX_STRIKES.get()) : this.original.getAttribute(EpicFightAttributes.MAX_STRIKES.get()).getBaseValue());
	}
	
	public float getArmorNegation(InteractionHand hand) {
		return (float) (hand == InteractionHand.MAIN_HAND ? this.original.getAttributeValue(EpicFightAttributes.ARMOR_NEGATION.get()) : 
			this.isOffhandItemValid() ? this.original.getAttributeValue(EpicFightAttributes.OFFHAND_ARMOR_NEGATION.get()) : this.original.getAttribute(EpicFightAttributes.ARMOR_NEGATION.get()).getBaseValue());
	}
	
	public float getImpact(InteractionHand hand) {
		float impact;
		int i = 0;
		
		if (hand == InteractionHand.MAIN_HAND) {
			impact = (float)this.original.getAttributeValue(EpicFightAttributes.IMPACT.get());
			i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, this.getOriginal().getMainHandItem());
		} else {
			if (this.isOffhandItemValid()) {
				impact = (float)this.original.getAttributeValue(EpicFightAttributes.OFFHAND_IMPACT.get());
				i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, this.getOriginal().getOffhandItem());
			} else {
				impact = (float)this.original.getAttribute(EpicFightAttributes.IMPACT.get()).getBaseValue();
			}
		}
		
		return impact * (1.0F + i * 0.12F);
	}
	
	public ItemStack getValidItemInHand(InteractionHand hand) {
		if (hand == InteractionHand.MAIN_HAND) {
			return this.original.getItemInHand(hand);
		} else {
			return this.isOffhandItemValid() ? this.original.getItemInHand(hand) : ItemStack.EMPTY;
		}
	}
	
	public boolean isOffhandItemValid() {
		return this.getHoldingItemCapability(InteractionHand.MAIN_HAND).checkOffhandValid(this);
	}
	
	public boolean isTeammate(Entity entityIn) {
		if (this.original.getVehicle() != null && this.original.getVehicle().equals(entityIn)) {
			return true;
		} else if (this.isRideOrBeingRidden(entityIn)) {
			return true;
		}
		
		return this.original.isAlliedTo(entityIn) && this.original.getTeam() != null && !this.original.getTeam().isAllowFriendlyFire();
	}
	
	public boolean canPush(Entity entity) {
		LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
		
		if (entitypatch != null) {
			EntityState state = entitypatch.getEntityState();
			
			if (state.inaction()) {
				return false;
			}
		}
		
		EntityState thisState = this.getEntityState();
		
		return !thisState.inaction() && !entity.is(this.entityBeingInteract);
	}
	
	public List<LivingEntity> getCurrenltyAttackedEntities() {
		return this.currentlyAttackedEntity;
	}
	
	public Vec3 getLastAttackPosition() {
		return this.lastAttackPosition;
	}
	
	public void setLastAttackPosition() {
		this.lastAttackPosition = this.original.position();
	}
	
	private boolean isRideOrBeingRidden(Entity entityIn) {
		LivingEntity orgEntity = this.getOriginal();
		
		for (Entity passanger : orgEntity.getPassengers()) {
			if (passanger.equals(entityIn)) {
				return true;
			}
		}
		
		for (Entity passanger : entityIn.getPassengers()) {
			if (passanger.equals(orgEntity)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void airborne() {
		this.airborne = true;
	}
	
	public void setLastAttackSuccess(boolean setter) {
		this.isLastAttackSuccess = setter;
	}
	
	public boolean isLastAttackSuccess() {
		return this.isLastAttackSuccess;
	}
	
	public boolean moveHere() {
		return !this.isLogicalClient();
	}
	
	public boolean isFirstPerson() {
		return false;
	}
	
	public boolean shouldSkipRender() {
		return false;
	}
	
	public boolean shouldBlockMoving() {
		return false;
	}
	
	public float getYRotLimit() {
		return 20.0F;
	}
	
	public EntityState getEntityState() {
		return this.state;
	}
	
	public LivingMotion getCurrentLivingMotion() {
		return this.currentLivingMotion;
	}
}