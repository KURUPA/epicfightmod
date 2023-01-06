
package yesman.epicfight.api.animation.types;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.TimeStampedEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.client.animation.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.JointMask;
import yesman.epicfight.api.client.animation.JointMask.BindModifier;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.Layer.LayerType;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.model.JsonModelLoader;
import yesman.epicfight.config.ConfigurationIngame;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class StaticAnimation extends DynamicAnimation {
	protected final Map<AnimationProperty<?>, Object> properties = Maps.newHashMap();
	protected final StateSpectrum.Blueprint stateSpectrumBlueprint = new StateSpectrum.Blueprint();
	protected final ResourceLocation resourceLocation;
	protected final Armature armature;
	protected final int namespaceId;
	protected final int animationId;
	
	private final StateSpectrum stateSpectrum = new StateSpectrum();
	
	public StaticAnimation() {
		super(0.0F, false);
		this.namespaceId = -1;
		this.animationId = -1;
		this.resourceLocation = null;
		this.armature = null;
	}
	
	public StaticAnimation(boolean repeatPlay, String path, Armature armature) {
		this(ConfigurationIngame.GENERAL_ANIMATION_CONVERT_TIME, repeatPlay, path, armature);
	}
	
	public StaticAnimation(float convertTime, boolean isRepeat, String path, Armature armature) {
		super(convertTime, isRepeat);
		
		AnimationManager animationManager = EpicFightMod.getInstance().animationManager;
		this.namespaceId = animationManager.getNamespaceHash();
		this.animationId = animationManager.getIdCounter();
		
		int colon = path.indexOf(':');
		String modid = (colon == -1) ? animationManager.getModid() : path.substring(0, colon);
		String folderPath = (colon == -1) ? path : path.substring(colon + 1, path.length());
		
		animationManager.getIdMap().put(this.animationId, this);
		this.resourceLocation = new ResourceLocation(modid, "animmodels/animations/" + folderPath);
		animationManager.getNameMap().put(new ResourceLocation(animationManager.getModid(), folderPath), this);
		this.armature = armature;
	}
	
	public StaticAnimation(float convertTime, boolean repeatPlay, String path, Armature armature, boolean notRegisteredInAnimationManager) {
		super(convertTime, repeatPlay);
		this.namespaceId = -1;
		this.animationId = -1;
		this.resourceLocation = new ResourceLocation(EpicFightMod.getInstance().animationManager.getModid(), "animmodels/animations/" + path);
		this.armature = armature;
	}
	
	public static void load(ResourceManager resourceManager, StaticAnimation animation) {
		ResourceLocation extenderPath = new ResourceLocation(animation.resourceLocation.getNamespace(), animation.resourceLocation.getPath() + ".json");
		(new JsonModelLoader(resourceManager, extenderPath)).loadStaticAnimation(animation);
	}
	
	public static void loadBothSide(ResourceManager resourceManager, StaticAnimation animation) {
		ResourceLocation extenderPath = new ResourceLocation(animation.resourceLocation.getNamespace(), animation.resourceLocation.getPath() + ".json");
		(new JsonModelLoader(resourceManager, extenderPath)).loadStaticAnimationBothSide(animation);
	}
	
	public void loadAnimation(ResourceManager resourceManager) {
		try {
			int id = Integer.parseInt(this.resourceLocation.getPath().substring(22));
			StaticAnimation animation = EpicFightMod.getInstance().animationManager.findAnimationById(this.namespaceId, id);
			this.jointTransforms = animation.jointTransforms;
			this.setTotalTime(animation.totalTime);
		} catch (NumberFormatException e) {
			load(resourceManager, this);
		}
		
		this.onLoaded();
	}
	
	protected void onLoaded() {
		this.stateSpectrum.readFrom(this.stateSpectrumBlueprint);
	}
	
	@Override
	public void begin(LivingEntityPatch<?> entitypatch) {
		this.getProperty(StaticAnimationProperty.ON_BEGIN_EVENTS).ifPresent((events) -> {
			for (AnimationEvent event : events) {
				event.executeIfRightSide(entitypatch);
			}
		});
	}
	
	@Override
	public void end(LivingEntityPatch<?> entitypatch, boolean isEnd) {
		this.getProperty(StaticAnimationProperty.ON_END_EVENTS).ifPresent((events) -> {
			for (AnimationEvent event : events) {
				event.executeIfRightSide(entitypatch);
			}
		});
	}
	
	@Override
	public void tick(LivingEntityPatch<?> entitypatch) {
		this.getProperty(StaticAnimationProperty.EVENTS).ifPresent((events) -> {
			for (AnimationEvent event : events) {
				event.executeIfRightSide(entitypatch);
			}
		});
		
		this.getProperty(StaticAnimationProperty.TIME_STAMPED_EVENTS).ifPresent((events) -> {
			AnimationPlayer player = entitypatch.getAnimator().getPlayerFor(this);
			
			if (player != null) {
				float prevElapsed = player.getPrevElapsedTime();
				float elapsed = player.getElapsedTime();
				
				for (TimeStampedEvent event : events) {
					event.executeIfRightSide(entitypatch, prevElapsed, elapsed);
				}
			}
		});
	}
	
	@Override
	public final EntityState getState(LivingEntityPatch<?> entitypatch, float time) {
		return this.stateSpectrum.bindStates(entitypatch, time);
	}
	
	@Override
	public boolean isJointEnabled(LivingEntityPatch<?> entitypatch, String joint) {
		if (!super.isJointEnabled(entitypatch, joint)) {
			return false;
		} else {
			boolean bool = this.getProperty(ClientAnimationProperties.JOINT_MASK).map((bindModifier) -> {
				return !bindModifier.isMasked(entitypatch.getCurrentLivingMotion(), joint);
			}).orElse(true);
			
			return bool;
		}
	}
	
	@Override
	public BindModifier getBindModifier(LivingEntityPatch<?> entitypatch, String joint) {
		return this.getProperty(ClientAnimationProperties.JOINT_MASK).map((jointMaskEntry) -> {
			List<JointMask> list = jointMaskEntry.getMask(entitypatch.getCurrentLivingMotion());
			int position = list.indexOf(JointMask.of(joint));
			
			if (position >= 0) {
				return list.get(position).getBindModifier();
			} else {
				return null;
			}
		}).orElse(null);
	}
	
	@Override
	public int getNamespaceId() {
		return this.namespaceId;
	}
	
	@Override
	public int getId() {
		return this.animationId;
	}
	
	public ResourceLocation getLocation() {
		return this.resourceLocation;
	}
	
	public Armature getArmature() {
		return this.armature;
	}
	
	public boolean isBasicAttackAnimation() {
		return false;
	}
	
	@Override
	public float getPlaySpeed(LivingEntityPatch<?> entitypatch) {
		return this.getProperty(StaticAnimationProperty.PLAY_SPEED).orElse(1.0F);
	}
	
	@Override
	public String toString() {
		String classPath = this.getClass().toString();
		return classPath.substring(classPath.lastIndexOf(".") + 1) + " " + this.getLocation();
	}
	
	public <V> StaticAnimation addProperty(StaticAnimationProperty<V> propertyType, V value) {
		this.properties.put(propertyType, value);
		return this;
	}
	
	public StaticAnimation addEvents(StaticAnimationProperty<?> key, AnimationEvent... events) {
		this.properties.put(key, events);
		return this;
	}
	
	public <V extends AnimationEvent> StaticAnimation addEvents(TimeStampedEvent... events) {
		this.properties.put(StaticAnimationProperty.TIME_STAMPED_EVENTS, events);
		return this;
	}
	
	public StateSpectrum.Blueprint getStateSpectrumBP() {
		return this.stateSpectrumBlueprint;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <V> Optional<V> getProperty(AnimationProperty<V> propertyType) {
		return (Optional<V>) Optional.ofNullable(this.properties.get(propertyType));
	}
	
	@OnlyIn(Dist.CLIENT)
	public Layer.Priority getPriority() {
		return this.getProperty(ClientAnimationProperties.PRIORITY).orElse(Layer.Priority.LOWEST);
	}
	
	@OnlyIn(Dist.CLIENT)
	public Layer.LayerType getLayerType() {
		return this.getProperty(ClientAnimationProperties.LAYER_TYPE).orElse(LayerType.BASE_LAYER);
	}
}