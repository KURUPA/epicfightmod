package yesman.epicfight.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.projectile.ProjectilePatch;
import yesman.epicfight.world.capabilities.provider.EntityPatchProvider;
import yesman.epicfight.world.capabilities.provider.ItemCapabilityProvider;
import yesman.epicfight.world.capabilities.provider.ProjectilePatchProvider;
import yesman.epicfight.world.capabilities.provider.SkillCapabilityProvider;

@Mod.EventBusSubscriber(modid=EpicFightMod.MODID)
public class CapabilityEvent {
	@SubscribeEvent
	public static void attachItemCapability(AttachCapabilitiesEvent<ItemStack> event) {
		if (event.getObject() != null) {
			ItemCapabilityProvider prov = new ItemCapabilityProvider(event.getObject());
			if (prov.hasCapability()) {
				event.addCapability(new ResourceLocation(EpicFightMod.MODID, "item_cap"), prov);
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@SubscribeEvent
	public static void attachEntityCapability(AttachCapabilitiesEvent<Entity> event) {
		EntityPatch entitypatch = EpicFightCapabilities.getEntityPatch(event.getObject(), EntityPatch.class);
		
		if (entitypatch == null) {
			EntityPatchProvider prov = new EntityPatchProvider(event.getObject());
			
			if (prov.hasCapability()) {
				EntityPatch entityCap = prov.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).orElse(null);
				entityCap.onConstructed(event.getObject());
				event.addCapability(new ResourceLocation(EpicFightMod.MODID, "entity_cap"), prov);
				
				if (entityCap instanceof PlayerPatch<?>) {
					if (event.getObject().getCapability(EpicFightCapabilities.CAPABILITY_SKILL).orElse(null) == null) {
						PlayerPatch<?> playerpatch = (PlayerPatch<?>)entityCap;
						
						if (playerpatch != null) {
							SkillCapabilityProvider skillProvider = new SkillCapabilityProvider(playerpatch);
							event.addCapability(new ResourceLocation(EpicFightMod.MODID, "skill_cap"), skillProvider);
						}
					}
				}
			}
		}
		
		if (event.getObject() instanceof Projectile) {
			ProjectilePatch<?> projectilePatch = EpicFightCapabilities.getProjectilePatch(event.getObject(), ProjectilePatch.class);
			
			if(projectilePatch == null) {
				ProjectilePatchProvider prov = new ProjectilePatchProvider(((Projectile)event.getObject()));
				
				if(prov.hasCapability()) {
					event.addCapability(new ResourceLocation(EpicFightMod.MODID, "projectile_cap"), prov);
				}
			}
		}
	}
}