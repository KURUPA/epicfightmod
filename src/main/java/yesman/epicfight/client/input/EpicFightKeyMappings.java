package yesman.epicfight.client.input;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ClientRegistry;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class EpicFightKeyMappings {
	public static final KeyMapping WEAPON_INNATE_SKILL_TOOLTIP = new KeyMapping("key." + EpicFightMod.MODID + ".show_tooltip", 80, "key." + EpicFightMod.MODID + ".gui");
	public static final KeyMapping SWITCH_MODE = new KeyMapping("key." + EpicFightMod.MODID + ".switch_mode", 82, "key." + EpicFightMod.MODID + ".combat");
	public static final KeyMapping DODGE = new KeyMapping("key." + EpicFightMod.MODID + ".dodge", 342, "key." + EpicFightMod.MODID + ".combat");
	public static final KeyMapping WEAPON_INNATE_SKILL = new WeaponInnateSkillKeyMapping("key." + EpicFightMod.MODID + ".weapon_innate_skill", InputConstants.Type.MOUSE, 0, "key." + EpicFightMod.MODID + ".combat");
	public static final KeyMapping SKILL_EDIT = new KeyMapping("key." + EpicFightMod.MODID + ".skill_gui", 75, "key." + EpicFightMod.MODID + ".gui");
	public static final KeyMapping LOCK_ON = new KeyMapping("key." + EpicFightMod.MODID + ".lock_on", InputConstants.Type.MOUSE, 2, "key." + EpicFightMod.MODID + ".combat");
	
	public static void registerKeys() {
		ClientRegistry.registerKeyBinding(WEAPON_INNATE_SKILL_TOOLTIP);
		ClientRegistry.registerKeyBinding(SWITCH_MODE);
		ClientRegistry.registerKeyBinding(DODGE);
		ClientRegistry.registerKeyBinding(WEAPON_INNATE_SKILL);
		ClientRegistry.registerKeyBinding(SKILL_EDIT);
		ClientRegistry.registerKeyBinding(LOCK_ON);
	}
}