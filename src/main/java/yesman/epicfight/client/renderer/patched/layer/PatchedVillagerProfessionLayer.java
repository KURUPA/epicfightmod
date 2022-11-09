package yesman.epicfight.client.renderer.patched.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.ZombieVillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.resources.metadata.animation.VillagerMetaDataSection;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.ClientModel;
import yesman.epicfight.api.client.model.AnimatedModels;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

@OnlyIn(Dist.CLIENT)
public class PatchedVillagerProfessionLayer extends PatchedLayer<ZombieVillager, MobPatch<ZombieVillager>, ZombieVillagerModel<ZombieVillager>, VillagerProfessionLayer<ZombieVillager, ZombieVillagerModel<ZombieVillager>>> {
	@Override
	public void renderLayer(MobPatch<ZombieVillager> entitypatch, ZombieVillager entityliving, VillagerProfessionLayer<ZombieVillager, ZombieVillagerModel<ZombieVillager>> originalRenderer, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, OpenMatrix4f[] poses, float netYawHead, float pitchHead, float partialTicks) {
		if (!entityliving.isInvisible()) {
			ClientModel model1 = entitypatch.getEntityModel(AnimatedModels.LOGICAL_CLIENT);
			ClientModel model2 = Models.AnimatedModels.villagerZombieBody;
			ClientModel drawingModel;
			VillagerData villagerdata = ((VillagerDataHolder)entitypatch.getOriginal()).getVillagerData();
			
			VillagerMetaDataSection.Hat typeHat = originalRenderer.getHatData(originalRenderer.typeHatCache, "type", Registry.VILLAGER_TYPE, villagerdata.getType());
	        @SuppressWarnings("deprecation")
	        VillagerMetaDataSection.Hat professionHat = originalRenderer.getHatData(originalRenderer.professionHatCache, "profession", Registry.VILLAGER_PROFESSION, villagerdata.getProfession());
	        drawingModel = (professionHat == VillagerMetaDataSection.Hat.NONE || professionHat == VillagerMetaDataSection.Hat.PARTIAL && typeHat != VillagerMetaDataSection.Hat.FULL) ? model1 : model2;
	        
			VertexConsumer builder1 = bufferIn.getBuffer(EpicFightRenderTypes.triangles(RenderType.entityCutoutNoCull(originalRenderer.getResourceLocation("type", Registry.VILLAGER_TYPE.getKey(villagerdata.getType())))));
			drawingModel.drawAnimatedModel(matrixStackIn, builder1, packedLightIn, 1.0F, 1.0F, 1.0F, 1.0F, LivingEntityRenderer.getOverlayCoords(entityliving, 0.0F), poses);
			drawingModel = entitypatch.getOriginal().getItemBySlot(EquipmentSlot.HEAD).isEmpty() ? model1 : model2;
			
			if (villagerdata.getProfession() != VillagerProfession.NONE) {
				VertexConsumer builder2 = bufferIn.getBuffer(EpicFightRenderTypes.triangles(RenderType.entityCutoutNoCull(originalRenderer.getResourceLocation("profession", villagerdata.getProfession().getRegistryName()))));
				drawingModel.drawAnimatedModel(matrixStackIn, builder2, packedLightIn, 1.0F, 1.0F, 1.0F, 1.0F, LivingEntityRenderer.getOverlayCoords(entityliving, 0.0F), poses);
			}
		}
	}
}