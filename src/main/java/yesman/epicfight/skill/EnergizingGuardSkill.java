package yesman.epicfight.skill;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Skills;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.HurtEvent;

public class EnergizingGuardSkill extends GuardSkill {
	public static GuardSkill.Builder createEnergizingGuardBuilder() {
		return GuardSkill.createGuardBuilder()
				.addAdvancedGuardMotion(WeaponCategories.LONGSWORD, (item, player) -> Animations.LONGSWORD_GUARD_HIT)
				.addAdvancedGuardMotion(WeaponCategories.SPEAR, (item, player) -> item.getStyle(player) == Styles.TWO_HAND ? Animations.SPEAR_GUARD_HIT : null)
				.addAdvancedGuardMotion(WeaponCategories.TACHI, (item, player) -> Animations.LONGSWORD_GUARD_HIT)
				.addAdvancedGuardMotion(WeaponCategories.GREATSWORD, (item, player) -> Animations.GREATSWORD_GUARD_HIT);
	}
	
	protected final float superiorPenalizer;
	protected final float damageReducer;
	
	public EnergizingGuardSkill(GuardSkill.Builder builder, CompoundTag parameters) {
		super(builder, parameters);
		
		this.superiorPenalizer = parameters.getFloat("superior_penalizer");
		this.damageReducer = parameters.getFloat("damage_reducer");
	}
	
	@Override
	public void guard(SkillContainer container, CapabilityItem itemCapapbility, HurtEvent.Pre event, float knockback, float impact, boolean advanced) {
		boolean canUse = this.isHoldingWeaponAvailable(event.getPlayerPatch(), itemCapapbility, BlockType.ADVANCED_GUARD);
		
		if (event.getDamageSource().isExplosion()) {
			impact = event.getAmount();
		}
		
		super.guard(container, itemCapapbility, event, knockback, impact, canUse);
	}
	
	@Override
	public void dealEvent(PlayerPatch<?> playerpatch, HurtEvent.Pre event) {
		boolean isSpecialSource = isSpecialDamageSource(event.getDamageSource());
		event.setAmount(isSpecialSource ? event.getAmount() * this.damageReducer * 0.01F : 0.0F);
		event.setResult(isSpecialSource ? AttackResult.ResultType.SUCCESS : AttackResult.ResultType.BLOCKED);
		
		if (event.getDamageSource() instanceof EpicFightDamageSource) {
			((EpicFightDamageSource)event.getDamageSource()).setStunType(StunType.NONE);
		}
		
		event.setCanceled(true);
		Entity directEntity = event.getDamageSource().getDirectEntity();
		LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(directEntity, LivingEntityPatch.class);
		
		if (entitypatch != null) {
			entitypatch.onAttackBlocked(event.getDamageSource(), playerpatch);
		}
	}
	
	@Override
	protected boolean isBlockableSource(DamageSource damageSource, boolean advanced) {
		return (!damageSource.isBypassArmor() || damageSource.msgId.equals("indirectMagic")) && (advanced || super.isBlockableSource(damageSource, false)) && !damageSource.isBypassInvul();
	}
	
	@Override
	public float getPenalizer(CapabilityItem itemCap) {
		return this.advancedGuardMotions.containsKey(itemCap.getWeaponCategory()) ? this.superiorPenalizer : this.penalizer;
	}
	
	private static boolean isSpecialDamageSource(DamageSource damageSource) {
		return (damageSource.isExplosion() || damageSource.isMagic() || damageSource.isFire() || damageSource.isProjectile()) && !(damageSource.isBypassArmor() && !damageSource.msgId.equals("indirectMagic"));
	}
	
	@Override
	public Skill getPriorSkill() {
		return Skills.GUARD;
	}
	
	@Override
	protected boolean isAdvancedGuard() {
		return true;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public List<Object> getTooltipArgs(List<Object> list) {
		list.clear();
		list.add(String.format("%.1f", this.damageReducer));
		list.add(String.format("%s, %s, %s, %s", WeaponCategories.GREATSWORD, WeaponCategories.LONGSWORD, WeaponCategories.SPEAR, WeaponCategories.TACHI).toLowerCase());
		
		return list;
	}
}