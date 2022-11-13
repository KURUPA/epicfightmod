package yesman.epicfight.api.animation.types;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;

public class LongHitAnimation extends ActionAnimation {
	public LongHitAnimation(float convertTime, String path, ResourceLocation armature) {
		super(convertTime, path, armature);
		this.addProperty(ActionAnimationProperty.STOP_MOVEMENT, true);
		
		this.stateSpectrumBlueprint.clear()
			.newTimePair(0.0F, Float.MAX_VALUE)
			.addState(EntityState.TURNING_LOCKED, true)
			.addState(EntityState.MOVEMENT_LOCKED, true)
			.addState(EntityState.CAN_BASIC_ATTACK, false)
			.addState(EntityState.CAN_SKILL_EXECUTION, false)
			.addState(EntityState.INACTION, true)
			.addState(EntityState.HURT,	true);
	}
}