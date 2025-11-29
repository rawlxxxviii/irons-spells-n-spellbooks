package io.redspace.ironsspellbooks.entity.spells.scapegoat;


import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.render.GeoLivingEntityRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ScapegoatRenderer extends GeoLivingEntityRenderer<ScapegoatEntity> {
    public ScapegoatRenderer(EntityRendererProvider.Context context) {
        super(context, new ScapegoatModel());
    }

    @Override
    public void render(ScapegoatEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }
}
