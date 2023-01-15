package yesman.epicfight.world.capabilities.entitypatch;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.damagesource.StunType;

public abstract class HurtableEntityPatch<T extends LivingEntity> extends EntityPatch<T> {
	protected float stunTimeReduction;
	protected boolean cancelKnockback;
	
	@Override
	public void tick(LivingUpdateEvent event) {
		if (this.isLogicalClient()) {
			this.clientTick(event);
		} else {
			this.serverTick(event);
		}
	}
	
	@Override
	protected void serverTick(LivingUpdateEvent event) {
		this.cancelKnockback = false;
		
		if (this.stunTimeReduction > 0.0F) {
			float stunArmor = this.getStunArmor();
			this.stunTimeReduction -= 0.05F * (1.1F - this.stunTimeReduction * this.stunTimeReduction) * (1.0F - stunArmor / (7.5F + stunArmor));
			this.stunTimeReduction = Math.max(0.0F, this.stunTimeReduction);
		}
	}
	
	@Override
	public OpenMatrix4f getModelMatrix(float partialTicks) {
		return null;
	}
	
	public abstract void applyStun(StunType stunType, float stunTime);
	
	public float getWeight() {
		return (float)this.original.getAttributeValue(Attributes.MAX_HEALTH) * 2.0F;
	}
	
	public float getStunShield() {
		return 0.0F;
	}
	
	public void setStunShield(float value) {
	}
	
	public void setStunReductionOnHit() {
		this.stunTimeReduction += Math.max((1.0F - this.stunTimeReduction) * 0.8F, 0.5F);
		this.stunTimeReduction = Math.min(1.0F, this.stunTimeReduction);
	}
	
	public float getStunTimeTimeReduction() {
		return this.stunTimeReduction;
	}
	
	public float getStunArmor() {
		return 0.0F;
	}
	
	public boolean shouldCancelKnockback() {
		return this.cancelKnockback;
	}

	public void knockBackEntity(Vec3 sourceLocation, float power) {
		double d1 = sourceLocation.x() - this.original.getX();
        double d0;
        
		for (d0 = sourceLocation.z() - this.original.getZ(); d1 * d1 + d0 * d0 < 1.0E-4D; d0 = (Math.random() - Math.random()) * 0.01D) {
            d1 = (Math.random() - Math.random()) * 0.01D;
        }
		
		power *= 1.0D - this.original.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		
		if (!(power <= 0.0D)) {
			this.original.hasImpulse = true;
			Vec3 vec3 = this.original.getDeltaMovement();
			Vec3 vec31 = (new Vec3(d1, 0.0D, d0)).normalize().scale(power);
			this.original.setDeltaMovement(vec3.x / 2.0D - vec31.x, this.original.onGround ? Math.min(0.4D, vec3.y / 2.0D) : vec3.y, vec3.z / 2.0D - vec31.z);
		}
	}
}