package yesman.epicfight.world.damagesource;

import net.minecraft.ChatFormatting;

public enum StunType {
	NONE(ChatFormatting.GRAY + "NONE", true),
	SHORT(ChatFormatting.GREEN + "SHORT" + ChatFormatting.DARK_GRAY + " stun", false),
	LONG(ChatFormatting.GOLD + "LONG" + ChatFormatting.DARK_GRAY + " stun", true),
	HOLD(ChatFormatting.RED + "HOLD", false),
	KNOCKDOWN(ChatFormatting.RED + "KNOCKDOWN", true),
	FALL(ChatFormatting.GRAY + "FALL", true);
	
	private String tooltip;
	private boolean fixedStunTime;
	
	StunType(String tooltip, boolean fixedStunTime) {
		this.tooltip = tooltip;
		this.fixedStunTime = fixedStunTime;
	}
	
	public boolean hasFixedStunTime() {
		return this.fixedStunTime;
	}
	
	@Override
	public String toString() {
		return tooltip;
	}
}