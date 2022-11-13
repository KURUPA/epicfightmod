package yesman.epicfight.api.animation.types;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.EntityDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.SourceTag;

public class KnockdownAnimation extends LongHitAnimation {
	public KnockdownAnimation(float convertTime, float delayTime, String path, ResourceLocation armature) {
		super(convertTime, path, armature);

		this.stateSpectrumBlueprint
			.addState(EntityState.KNOCKDOWN, true)
			.addState(EntityState.INVULNERABILITY_PREDICATE, (damagesource) -> {
				if (damagesource instanceof EntityDamageSource && !damagesource.isExplosion() && !damagesource.isMagic() && !damagesource.isBypassInvul()) {
					if (damagesource instanceof EpicFightDamageSource) {
						return !((EpicFightDamageSource)damagesource).hasTag(SourceTag.FINISHER);
					} else {
						return true;
					}
				}
				return false;
			});
	}
}