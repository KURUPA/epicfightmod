package yesman.epicfight.api.animation.types;

import net.minecraft.world.entity.monster.RangedAttackMob;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.ModelOld;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class RangedAttackAnimation extends AttackAnimation {
	public RangedAttackAnimation(float convertTime, float antic, float preDelay, float contact, float recovery, Collider collider, String index, String path, ModelOld model) {
		super(convertTime, antic, preDelay, contact, recovery, collider, index, path, model);
	}
	
	@Override
	public void hurtCollidingEntities(LivingEntityPatch<?> entitypatch, float prevElapsedTime, float elapsedTime, EntityState prevState, EntityState state, Phase phase) {
		if (state.attacking() && entitypatch.getTarget() != null && (entitypatch.getOriginal() instanceof RangedAttackMob)) {
			((RangedAttackMob)entitypatch.getOriginal()).performRangedAttack(entitypatch.getTarget(), elapsedTime);
		}
	}
}