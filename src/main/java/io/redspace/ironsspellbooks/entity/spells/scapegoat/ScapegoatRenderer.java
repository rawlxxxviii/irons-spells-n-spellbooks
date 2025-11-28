package io.redspace.ironsspellbooks.entity.spells.scapegoat;


import io.redspace.ironsspellbooks.render.GeoLivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ScapegoatRenderer extends GeoLivingEntityRenderer<ScapegoatEntity> {
    public ScapegoatRenderer(EntityRendererProvider.Context context) {
        super(context, new ScapegoatModel());
    }

}
