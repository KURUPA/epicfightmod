package yesman.epicfight.network.server;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.gameasset.Skills;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

public class SPRemoveSkill {
	private String skillName;
	
	public SPRemoveSkill() {
		this("");
	}
	
	public SPRemoveSkill(String name) {
		this.skillName = name;
	}
	
	public static SPRemoveSkill fromBytes(FriendlyByteBuf buf) {
		SPRemoveSkill msg = new SPRemoveSkill(buf.readUtf());
		return msg;
	}
	
	public static void toBytes(SPRemoveSkill msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.skillName);
	}
	
	public static void handle(SPRemoveSkill msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			LocalPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(mc.player, LocalPlayerPatch.class);
			
			if (playerpatch != null) {
				Skill skill = Skills.getSkill(msg.skillName);
				playerpatch.getSkillCapability().removeLearnedSkill(skill);
				SkillContainer skillContainer = playerpatch.getSkillCapability().skillContainers[skill.getCategory().universalOrdinal()];
				
				if (skillContainer.getSkill() == skill) {
					skillContainer.setSkill(null);
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}
}