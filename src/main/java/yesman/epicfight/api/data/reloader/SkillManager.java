package yesman.epicfight.api.data.reloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModLoader;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.server.SPDatapackSync;
import yesman.epicfight.skill.Skill;

public class SkillManager extends SimpleJsonResourceReloadListener {
	private static final Map<ResourceLocation, Skill> SKILLS = Maps.newHashMap();
	private static final Map<ResourceLocation, Skill> LEARNABLE_SKILLS = Maps.newHashMap();
	private static final Map<ResourceLocation, CompoundTag> PARAMETER_MAP = Maps.newHashMap();
	private static final Map<ResourceLocation, Pair<? extends Skill.Builder<?>, Function<? extends Skill.Builder<?>, ? extends Skill>>> BUILDERS = Maps.newHashMap();
	private static final Gson GSON = (new GsonBuilder()).create();
	private static final Random RANDOM = new Random();
	private static int LAST_PICK = 0;
	
	public static Stream<ResourceLocation> getLearnableSkillNames() {
		return BUILDERS.values().stream().map(map -> map.getFirst()).filter(builder -> builder.isLearnable()).map(builder -> builder.getRegistryName());
	}
	
	public static Skill getSkill(String name) {
		ResourceLocation rl;
		
		if (name.indexOf(':') >= 0) {
			rl = new ResourceLocation(name);
		} else {
			rl = new ResourceLocation(EpicFightMod.MODID, name);
		}
		
		if (SKILLS.containsKey(rl)) {
			return SKILLS.get(rl);
		} else {
			return null;
		}
	}
	
	public static String getRandomLearnableSkillName() {
		List<Skill> values = new ArrayList<Skill>(LEARNABLE_SKILLS.values());
		LAST_PICK = (LAST_PICK + RANDOM.nextInt(values.size() - 1) + 1) % values.size();
		
		return values.get(LAST_PICK).toString();
	}
	
	public static <T extends Skill, B extends Skill.Builder<T>> void register(Function<B, T> constructor, B builder, String modid, String name) {
		ResourceLocation registryName = new ResourceLocation(modid, name);
		BUILDERS.put(registryName, Pair.of(builder.setRegistryName(registryName), constructor));
		
		EpicFightMod.LOGGER.info("register skill " + registryName);
	}
	
	public static void buildAll() {
		SkillBuildEvent onBuild = new SkillBuildEvent(BUILDERS, SKILLS, LEARNABLE_SKILLS);
		ModLoader.get().postEvent(onBuild);
	}
	
	public static Stream<CompoundTag> getDataStream() {
		Stream<CompoundTag> tagStream = PARAMETER_MAP.entrySet().stream().map((entry) -> {
			entry.getValue().putString("id", entry.getKey().toString());
			
			return entry.getValue();
		});
		
		return tagStream;
	}
	
	public static int getParamCount() {
		return PARAMETER_MAP.size();
	}
	
	public SkillManager() {
		super(GSON, "skill_parameters");
	}
	
	@Override
	protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManager, ProfilerFiller profileFiller) {
		SkillManager.buildAll();
		
		for (Map.Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
			CompoundTag tag = null;
			
			try {
				tag = TagParser.parseTag(entry.getValue().toString());
			} catch (CommandSyntaxException e) {
				e.printStackTrace();
			}
			
			if (SKILLS.containsKey(entry.getKey())) {
				SKILLS.get(entry.getKey()).setParams(tag);
				PARAMETER_MAP.put(entry.getKey(), tag);
			} else {
				EpicFightMod.LOGGER.warn("Skill " + entry.getKey() + " not exists");
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static void processServerPacket(SPDatapackSync packet) {
		for (CompoundTag tag : packet.getTags()) {
			SKILLS.get(new ResourceLocation(tag.getString("id"))).setParams(tag);
		}
	}
}