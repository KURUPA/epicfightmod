package yesman.epicfight.events;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionAddedEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionExpiryEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionRemoveEvent;
import net.minecraftforge.event.entity.player.PlayerFlyableFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPPlayAnimation;
import yesman.epicfight.network.server.SPPotion;
import yesman.epicfight.network.server.SPPotion.Action;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.HurtableEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.mob.EndermanPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.projectile.ProjectilePatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.ExtraDamageInstance;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.effect.EpicFightMobEffects;
import yesman.epicfight.world.entity.eventlistener.HurtEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.eventlistener.ProjectileHitEvent;

@Mod.EventBusSubscriber(modid=EpicFightMod.MODID)
public class EntityEvents {
	@SuppressWarnings("unchecked")
	@SubscribeEvent
	public static void spawnEvent(EntityJoinWorldEvent event) {
		EntityPatch<Entity> entitypatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), EntityPatch.class);
		
		if (entitypatch != null && !entitypatch.isInitialized()) {
			entitypatch.onJoinWorld(event.getEntity(), event);
		}
		
		if (event.getEntity() instanceof Projectile) {
			Projectile projectileentity = (Projectile)event.getEntity();
			ProjectilePatch<Projectile> projectilePatch = EpicFightCapabilities.getProjectilePatch(projectileentity, ProjectilePatch.class);
			
			if (projectilePatch != null) {
				projectilePatch.onJoinWorld(projectileentity, event);
			}
		}
	}
	
	@SubscribeEvent
	public static void updateEvent(LivingUpdateEvent event) {
		EntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getEntityLiving(), EntityPatch.class);
		
		if (entitypatch != null && entitypatch.getOriginal() != null) {
			entitypatch.tick(event);
		}
	}
	
	@SubscribeEvent
	public static void knockBackEvent(LivingKnockBackEvent event) {
		HurtableEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getEntityLiving(), HurtableEntityPatch.class);
		
		if (entitypatch != null && entitypatch.shouldCancelKnockback()) {
			event.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public static void hurtEvent(LivingHurtEvent event) {
		EpicFightDamageSource epicFightDamageSource = null;
		Entity trueSource = event.getSource().getEntity();
		
		if (trueSource != null) {
			LivingEntityPatch<?> attackerEntityPatch = EpicFightCapabilities.getEntityPatch(trueSource, LivingEntityPatch.class);
			float baseDamage = event.getAmount();
			
			if (event.getSource() instanceof EpicFightDamageSource) {
				epicFightDamageSource = (EpicFightDamageSource) event.getSource();
			} else if (event.getSource() instanceof IndirectEntityDamageSource && event.getSource().getDirectEntity() != null) {
				ProjectilePatch<?> projectileCap = EpicFightCapabilities.getProjectilePatch(event.getSource().getDirectEntity(), ProjectilePatch.class);
				
				if (projectileCap != null) {
					epicFightDamageSource = projectileCap.getEpicFightDamageSource(event.getSource());
				}
			} else if (attackerEntityPatch != null) {
				epicFightDamageSource = attackerEntityPatch.getAnimationDamageSource();
				baseDamage = attackerEntityPatch.getModifiedBaseDamage(trueSource, epicFightDamageSource, baseDamage);
			}
			
			if (epicFightDamageSource != null) {
				LivingEntity hitEntity = event.getEntityLiving();
				float totalDamage = epicFightDamageSource.getDamageModifier().getTotalValue(baseDamage);
				
				if (trueSource instanceof LivingEntity && epicFightDamageSource.getExtraDamages() != null) {
					for (ExtraDamageInstance extraDamage : epicFightDamageSource.getExtraDamages()) {
						totalDamage += extraDamage.get((LivingEntity)trueSource, epicFightDamageSource.getHurtItem(), hitEntity, baseDamage);
					}
				}
				
				if (hitEntity instanceof ServerPlayer) {
					ServerPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(hitEntity, ServerPlayerPatch.class);
					HurtEvent.Post hurtEvent = new HurtEvent.Post(playerpatch, epicFightDamageSource, totalDamage);
					playerpatch.getEventListener().triggerEvents(EventType.HURT_EVENT_POST, hurtEvent);
					totalDamage = hurtEvent.getAmount();
				}
				
				float trueDamage = totalDamage * epicFightDamageSource.getArmorNegation() * 0.01F;
				float calculatedDamage = trueDamage;
				
			    if (hitEntity.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
			    	int i = (hitEntity.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
			        int j = 25 - i;
			        float f = calculatedDamage * (float)j;
			        float f1 = calculatedDamage;
			        calculatedDamage = Math.max(f / 25.0F, 0.0F);
			        float f2 = f1 - calculatedDamage;
			        
					if (f2 > 0.0F && f2 < 3.4028235E37F) {
			        	if (hitEntity instanceof ServerPlayer) {
			        		((ServerPlayer)hitEntity).awardStat(Stats.DAMAGE_RESISTED, Math.round(f2 * 10.0F));
			        	} else if (event.getSource().getEntity() instanceof ServerPlayer) {
			                ((ServerPlayer)event.getSource().getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(f2 * 10.0F));
			        	}
			        }
			    }

				if (calculatedDamage > 0.0F) {
					int k = EnchantmentHelper.getDamageProtection(hitEntity.getArmorSlots(), event.getSource());
					if (k > 0) {
			        	calculatedDamage = CombatRules.getDamageAfterMagicAbsorb(calculatedDamage, (float)k);
			        }
			    }
			    
			    float absorpAmount = hitEntity.getAbsorptionAmount() - calculatedDamage;
			    hitEntity.setAbsorptionAmount(Math.max(absorpAmount, 0.0F));
		        float realHealthDamage = Math.max(-absorpAmount, 0.0F);
		        
		        if (realHealthDamage > 0.0F && realHealthDamage < 3.4028235E37F && event.getSource().getEntity() instanceof ServerPlayer) {
		        	((ServerPlayer)event.getSource().getEntity()).awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(realHealthDamage * 10.0F));
		        }
		        
				if (absorpAmount < 0.0F) {
					hitEntity.setHealth(hitEntity.getHealth() + absorpAmount);
		        	
					if (attackerEntityPatch != null) {
						attackerEntityPatch.gatherDamageDealt(epicFightDamageSource, calculatedDamage);
					}
		        }
		        
				event.setAmount(totalDamage - trueDamage);
				
				if (event.getAmount() + trueDamage > 0.0F) {
					HurtableEntityPatch<?> hitentitypatch = EpicFightCapabilities.getEntityPatch(hitEntity, HurtableEntityPatch.class);
					
					if (hitentitypatch != null) {
						StunType stunType = StunType.NONE;
						float stunTime = 0.0F;
						float knockBackAmount = 0.0F;
						float weight = 40.0F / (float)hitentitypatch.getWeight();
						float stunShield = hitentitypatch.getStunShield();
						
						if (stunShield > 0.0F) {
							hitentitypatch.setStunShield(stunShield - epicFightDamageSource.getImpact());
						}
						
						switch (epicFightDamageSource.getStunType()) {
						case SHORT:
							if (!hitEntity.hasEffect(EpicFightMobEffects.STUN_IMMUNITY.get()) && (hitentitypatch.getStunShield() == 0.0F)) {
								float totalStunTime = (0.25F + (epicFightDamageSource.getImpact()) * 0.1F) * weight;
								totalStunTime *= (1.0F - hitentitypatch.getStunTimeTimeReduction());
								
								if (totalStunTime >= 0.1F) {
									stunTime = totalStunTime - 0.1F;
									boolean flag = totalStunTime >= 0.83F;
									stunTime = flag ? 0.83F : stunTime;
									stunType = flag ? StunType.LONG : StunType.SHORT;
									knockBackAmount = Math.min(flag ? epicFightDamageSource.getImpact() * 0.05F : totalStunTime, 2.0F);
								}
							}
							break;
						case LONG:
							stunType = hitEntity.hasEffect(EpicFightMobEffects.STUN_IMMUNITY.get()) ? StunType.NONE : StunType.LONG;
							knockBackAmount = Math.min(epicFightDamageSource.getImpact() * 0.25F * weight, 5.0F);
							stunTime = 0.83F;
							break;
						case HOLD:
							stunType = StunType.SHORT;
							stunTime = epicFightDamageSource.getImpact() * 0.1F;
							break;
						case KNOCKDOWN:
							stunType = hitEntity.hasEffect(EpicFightMobEffects.STUN_IMMUNITY.get()) ? StunType.NONE : StunType.KNOCKDOWN;
							knockBackAmount = Math.min(epicFightDamageSource.getImpact() * 0.05F, 5.0F);
							stunTime = 2.0F;
							break;
						case FALL:
							break;
						case NONE:
							break;
						}
						
						stunTime *= 1.0F - hitEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
						Vec3 sourcePosition = ((DamageSource)epicFightDamageSource).getSourcePosition();
						
						if (sourcePosition != null) {
							if (!(hitEntity instanceof Player)) {
								hitEntity.lookAt(EntityAnchorArgument.Anchor.FEET, sourcePosition);
							}
							
							hitentitypatch.setStunReductionOnHit();
							hitentitypatch.applyStun(stunType, stunTime);
							
							if (knockBackAmount != 0.0F) {
								hitentitypatch.knockBackEntity(((DamageSource)epicFightDamageSource).getSourcePosition(), knockBackAmount);
							}
						}
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void damageEvent(LivingDamageEvent event) {
		Entity attacker = event.getSource().getEntity();
		
		if (attacker != null) {
			LivingEntityPatch<?> attackerpatch = EpicFightCapabilities.getEntityPatch(attacker, LivingEntityPatch.class);
			
			if (attackerpatch != null && attackerpatch.getAnimationDamageSource() != null) {
				attackerpatch.gatherDamageDealt(attackerpatch.getAnimationDamageSource(), event.getAmount());
			}
		}
	}
	
	@SubscribeEvent
	public static void attackEvent(LivingAttackEvent event) {
		if (event.getEntity().level.isClientSide()) {
			return;
		}
		
		LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), LivingEntityPatch.class);
		DamageSource damageSource = null;
		
		if (event.getEntityLiving().getHealth() > 0.0F) {
			LivingEntityPatch<?> attackerPatch = EpicFightCapabilities.getEntityPatch(event.getSource().getEntity(), LivingEntityPatch.class);
			
			if (event.getSource() instanceof IndirectEntityDamageSource && event.getSource().getDirectEntity() != null) {
				ProjectilePatch<?> projectilepatch = EpicFightCapabilities.getProjectilePatch(event.getSource().getDirectEntity(), ProjectilePatch.class);
				
				if (projectilepatch != null) {
					damageSource = projectilepatch.getEpicFightDamageSource(event.getSource());
				}
			} else if (attackerPatch != null && attackerPatch.getAnimationDamageSource() != null) {
				damageSource = attackerPatch.getAnimationDamageSource().cast();
			}
			
			if (damageSource == null) {
				damageSource = event.getSource();
			}
			
			AttackResult result = (entitypatch != null) ? entitypatch.tryHurt(damageSource, event.getAmount()) : AttackResult.success(event.getAmount());
			
			if (attackerPatch != null) {
				attackerPatch.setLastAttackResult(event.getEntity(), result);
			}
			
			if (!result.resultType.dealtDamage()) {
				event.setCanceled(true);
			} else if (event.getAmount() != result.damage) {
				event.setCanceled(true);
				
				DamageSource damagesource = new DamageSource(event.getSource().getMsgId());
				damagesource.bypassArmor();
				
				event.getEntity().hurt(damagesource, result.damage);
			}
		}
	}
	
	@SubscribeEvent
	public static void dropEvent(LivingDropsEvent event) {
		LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getEntityLiving(), LivingEntityPatch.class);
		
		if (entitypatch != null) {
			if (entitypatch.onDrop(event)) {
				event.setCanceled(true);
			}
		}
	}
	
	@SubscribeEvent
	public static void projectileImpactEvent(ProjectileImpactEvent event) {
		ProjectilePatch<?> projectilepatch = EpicFightCapabilities.getProjectilePatch(event.getEntity(), ProjectilePatch.class);
		
		if (!event.getProjectile().level.isClientSide() && projectilepatch != null) {
			if (projectilepatch.onProjectileImpact(event)) {
				event.setCanceled(true);
				return;
			}
		}
		
		if (event.getRayTraceResult() instanceof EntityHitResult) {
			EntityHitResult rayresult = ((EntityHitResult)event.getRayTraceResult());
			
			if (rayresult.getEntity() != null) {
				if (rayresult.getEntity() instanceof ServerPlayer) {
					ServerPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(rayresult.getEntity(), ServerPlayerPatch.class);
					boolean canceled = playerpatch.getEventListener().triggerEvents(EventType.PROJECTILE_HIT_EVENT, new ProjectileHitEvent(playerpatch, event));
					
					if (canceled) {
						event.setCanceled(true);
					}
				}
				
				if (event.getProjectile().getOwner() != null) {
					if (rayresult.getEntity().equals(event.getProjectile().getOwner().getVehicle())) {
						event.setCanceled(true);
					}
					
					if (rayresult.getEntity() instanceof PartEntity) {
						Entity parent = ((PartEntity<?>)rayresult.getEntity()).getParent();
						
						if (event.getProjectile().getOwner().is(parent)) {
							event.setCanceled(true);
						}
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void equipChangeEvent(LivingEquipmentChangeEvent event) {
		LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), LivingEntityPatch.class);
		CapabilityItem fromCap = EpicFightCapabilities.getItemStackCapability(event.getFrom());
		CapabilityItem toCap = EpicFightCapabilities.getItemStackCapability(event.getTo());
		
		if (event.getSlot() != EquipmentSlot.OFFHAND) {
			if (fromCap != null) {
				event.getEntityLiving().getAttributes().removeAttributeModifiers(fromCap.getAttributeModifiers(event.getSlot(), entitypatch));
			}
			
			if (toCap != null) {
				event.getEntityLiving().getAttributes().addTransientAttributeModifiers(toCap.getAttributeModifiers(event.getSlot(), entitypatch));
			}
		}
		
		if (entitypatch != null && entitypatch.getOriginal() != null) {
			if (event.getSlot().getType() == EquipmentSlot.Type.HAND) {
				InteractionHand hand = event.getSlot() == EquipmentSlot.MAINHAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
				entitypatch.updateHeldItem(fromCap, toCap, event.getFrom(), event.getTo(), hand);
			} else if (event.getSlot().getType() == EquipmentSlot.Type.ARMOR) {
				entitypatch.updateArmor(fromCap, toCap, event.getSlot());
			}
		}
	}
	
	@SubscribeEvent
	public static void sizingEvent(EntityEvent.Size event) {
		if (event.getEntity() instanceof EnderDragon) {
			event.setNewSize(EntityDimensions.scalable(5.0F, 3.0F));
		}
	}
	
	
	@SubscribeEvent
	public static void effectAddEvent(PotionAddedEvent event) {
		if (!event.getEntity().level.isClientSide()) {
			EpicFightNetworkManager.sendToAll(new SPPotion(event.getPotionEffect().getEffect(), Action.ACTIVATE, event.getEntity().getId()));
		}
	}
	
	@SubscribeEvent
	public static void effectRemoveEvent(PotionRemoveEvent event) {
		if (!event.getEntity().level.isClientSide() && event.getPotionEffect() != null) {
			EpicFightNetworkManager.sendToAll(new SPPotion(event.getPotionEffect().getEffect(), Action.REMOVE, event.getEntity().getId()));
		}
	}
	
	@SubscribeEvent
	public static void effectExpiryEvent(PotionExpiryEvent event) {
		if (!event.getEntity().level.isClientSide()) {
			EpicFightNetworkManager.sendToAll(new SPPotion(event.getPotionEffect().getEffect(), Action.REMOVE, event.getEntity().getId()));
		}
	}
	
	@SubscribeEvent
	public static void mountEvent(EntityMountEvent event) {
		EntityPatch<?> mountEntity = EpicFightCapabilities.getEntityPatch(event.getEntityMounting(), EntityPatch.class);
		
		if (!event.getWorldObj().isClientSide() && mountEntity instanceof HumanoidMobPatch && mountEntity.getOriginal() != null) {
			if (event.getEntityBeingMounted() instanceof Mob) {
				((HumanoidMobPatch<?>)mountEntity).onMount(event.isMounting(), event.getEntityBeingMounted());
			}
		}
	}
	
	@SubscribeEvent
	public static void tpEvent(EntityTeleportEvent.EnderEntity event) {
		LivingEntity entity = event.getEntityLiving();
		
		if (event.getEntityLiving() instanceof EnderMan) {
			EnderMan enderman = (EnderMan)entity;
			EndermanPatch endermanpatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), EndermanPatch.class);
			
			if (endermanpatch != null) {
				if (endermanpatch.getEntityState().inaction()) {
					for (Entity collideEntity : enderman.level.getEntitiesOfClass(Entity.class, enderman.getBoundingBox().inflate(0.2D, 0.2D, 0.2D))) {
						if (collideEntity instanceof Projectile) {
	                    	return;
	                    }
	                }
					
					event.setCanceled(true);
				} else if (endermanpatch.isRaging()) {
					event.setCanceled(true);
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void jumpEvent(LivingJumpEvent event) {
		LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), LivingEntityPatch.class);
		
		if (entitypatch != null && entitypatch.isLogicalClient()) {
			if (!entitypatch.getEntityState().inaction() && !event.getEntity().isInWater()) {
				StaticAnimation jumpAnimation = entitypatch.getClientAnimator().getJumpAnimation();
				entitypatch.getAnimator().playAnimation(jumpAnimation, 0);
				EpicFightNetworkManager.sendToServer(new CPPlayAnimation(jumpAnimation.getNamespaceId(), jumpAnimation.getId(), 0, true, false));
			}
		}
	}
	
	@SubscribeEvent
	public static void deathEvent(LivingDeathEvent event) {
		LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getEntityLiving(), LivingEntityPatch.class);
		
		if (entitypatch != null) {
			entitypatch.onDeath();
		}
	}
	
	
	@SubscribeEvent
	public static void fallEvent(LivingFallEvent event) {
		LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), LivingEntityPatch.class);
		
		if (entitypatch != null) {
			entitypatch.onFall(event);
		}
	}

	@SubscribeEvent
	public static void playerFallEvent(PlayerFlyableFallEvent event) {
		PlayerPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(event.getEntity(), PlayerPatch.class);
		
		if (entitypatch != null) {
			entitypatch.onFall(event);
		}
	}
}