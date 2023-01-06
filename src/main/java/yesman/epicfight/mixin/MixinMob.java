package yesman.epicfight.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Mob;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.GlobalMobPatch;

@Mixin(value = Mob.class)
public abstract class MixinMob {
	@Inject(at = @At(value = "HEAD"), method = "serverAiStep()V", cancellable = true)
	private void epicfight_serverAiStep(CallbackInfo info) {
		Mob self = (Mob)((Object)this);
		GlobalMobPatch entitypatch = EpicFightCapabilities.getEntityPatch(self, GlobalMobPatch.class);
		
		if (entitypatch != null && entitypatch.isStunned()) {
			info.cancel();
		}
	}
}