package yesman.epicfight.world.capabilities.entitypatch;

import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import yesman.epicfight.world.damagesource.StunType;

public class GlobalMobPatch extends HurtableEntityPatch<Mob> {
	private int remainStunTime;
	
	@Override
	protected void serverTick(LivingUpdateEvent event) {
		super.serverTick(event);
		--this.remainStunTime;
	}
	
	@Override
	public void applyStun(StunType stunType, float stunTime) {
		this.cancelKnockback = true;
		this.remainStunTime = (int)(stunTime * 20.0F);
	}
	
	public boolean isStunned() {
		return this.remainStunTime > 0;
	}
}