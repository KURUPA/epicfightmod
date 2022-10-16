package yesman.epicfight.client.gui.screen;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.item.SkillBookItem;

@OnlyIn(Dist.CLIENT)
public class SkillBookScreen extends Screen {
	public static final ResourceLocation BACKGROUND = new ResourceLocation(EpicFightMod.MODID, "textures/gui/screen/skillbook.png");
	private final Player opener;
	private final Skill skill;
	private final InteractionHand hand;
	
	public SkillBookScreen(Player opener, ItemStack stack, InteractionHand hand) {
		super(TextComponent.EMPTY);
		this.opener = opener;
		this.skill = SkillBookItem.getContainSkill(stack);
		this.hand = hand;
	}
	
	@Override
	protected void init() {
		LocalPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(this.opener, LocalPlayerPatch.class);
		boolean isUsing = this.skill.equals(playerpatch.getSkill(this.skill.getCategory()).getSkill());
		Skill priorSkill = this.skill.getPriorSkill();
		boolean condition = priorSkill == null ? true : playerpatch.getSkill(priorSkill.getCategory()).getSkill() == priorSkill;
		Button.OnTooltip tooltip = Button.NO_TOOLTIP;
		
		if (!isUsing) {
			if (condition) {
				if (playerpatch.getSkill(this.skill.getCategory()).getSkill() != null) {
					tooltip = (button, matrixStack, mouseX, mouseY) -> {
						this.renderTooltip(matrixStack, this.minecraft.font.split(new TranslatableComponent("gui." + EpicFightMod.MODID + ".replace",
								new TranslatableComponent(skill.getTranslatableText()).getString()), Math.max(this.width / 2 - 43, 170)), mouseX, mouseY);
					};
				}
			} else {
				tooltip = (button, matrixStack, mouseX, mouseY) -> {
					this.renderTooltip(matrixStack, this.minecraft.font.split(new TranslatableComponent("gui." + EpicFightMod.MODID + ".require_learning",
							new TranslatableComponent(priorSkill.getTranslatableText()).getString()), Math.max(this.width / 2 - 43, 170)), mouseX, mouseY);
				};
			}
		}
		
		Button changeButton = new Button((this.width + 130) / 2, (this.height + 90) / 2, 46, 20,
			new TranslatableComponent("gui." + EpicFightMod.MODID + (isUsing ? ".applied" : condition ? ".learn" : ".unusable")), (p_onPress_1_) -> {
				if (playerpatch != null) {
					playerpatch.getSkill(this.skill.getCategory()).setSkill(this.skill);
					this.minecraft.setScreen((Screen) null);
					playerpatch.getSkillCapability().addLearnedSkill(this.skill);
					int i = this.hand == InteractionHand.MAIN_HAND ? this.opener.getInventory().selected : 40;
					EpicFightNetworkManager.sendToServer(new CPChangeSkill(this.skill.getCategory().universalOrdinal(), i, this.skill.toString(), false));
				}
			}, tooltip);
		
		if (isUsing || !condition) {
			changeButton.active = false;
		}
		
		this.addRenderableWidget(changeButton);
	}
	
	@Override
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(matrixStack);
		int posX = (this.width - 250) / 2;
		int posY = (this.height - 200) / 2;
		
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, BACKGROUND);
		this.blit(matrixStack, posX, posY, 0, 0, 256, 181);
		RenderSystem.setShaderTexture(0, this.skill.getSkillTexture());
		
		RenderSystem.enableBlend();
		GuiComponent.blit(matrixStack, posX + 25, posY + 50, 50, 50, 0, 0, 64, 64, 64, 64);
		RenderSystem.disableBlend();
		
		String translationName = this.skill.getTranslatableText();
		
		String skillName = new TranslatableComponent(translationName).getString();
		int width = this.font.width(skillName);
		this.font.draw(matrixStack, skillName, posX + 50 - width / 2, posY + 115, 0);
		
		String skillCategory = String.format("(%s)", new TranslatableComponent("skill." + EpicFightMod.MODID + "." + this.skill.getCategory().toString().toLowerCase() + ".category").getString());
		width = this.font.width(skillCategory);
		this.font.draw(matrixStack, skillCategory, posX + 50 - width / 2, posY + 130, 0);
		
		List<FormattedCharSequence> list = this.font.split(new TranslatableComponent(translationName + ".tooltip", this.skill.getTooltipArgs().toArray(new Object[0])), 140);
		int height = posY + 20;
		
		for (int l1 = 0; l1 < list.size(); ++l1) {
			FormattedCharSequence ireorderingprocessor1 = list.get(l1);
            if (ireorderingprocessor1 != null) {
               this.font.draw(matrixStack, ireorderingprocessor1, posX + 105, height, 0);
            }
            
            height+=10;
		}
		
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}
}