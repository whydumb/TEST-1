package com.kAIS.KAIMyEntity.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class KAIMyEntityRenderer<T extends Entity> extends EntityRenderer<T> {
    protected String modelName;
    protected EntityRendererProvider.Context context;

    public KAIMyEntityRenderer(EntityRendererProvider.Context renderManager, String entityName) {
        super(renderManager);
        this.modelName = entityName.replace(':', '.');
        this.context = renderManager;
    }

    @Override
    public boolean shouldRender(T livingEntityIn, Frustum camera, double camX, double camY, double camZ) {
        return super.shouldRender(livingEntityIn, camera, camX, camY, camZ);
    }

    @Override
    public void render(T entityIn, float entityYaw, float tickDelta, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
        Minecraft MCinstance = Minecraft.getInstance();
        super.render(entityIn, entityYaw, tickDelta, matrixStackIn, bufferIn, packedLightIn);

        float bodyYaw = entityYaw;
        if(entityIn instanceof LivingEntity){
            bodyYaw = Mth.rotLerp(tickDelta, ((LivingEntity)entityIn).yBodyRotO, ((LivingEntity)entityIn).yBodyRot);
        }
        Vector3f entityTrans = new Vector3f(0.0f);
        MMDModelManager.Model model = MMDModelManager.GetModel(modelName, entityIn.getStringUUID());
        if(model == null){
            return;
        }
        model.loadModelProperties(false);
        float[] size = sizeOfModel(model);

        matrixStackIn.pushPose();
        if(entityIn instanceof LivingEntity && ((LivingEntity) entityIn).isBaby()){
            matrixStackIn.scale(0.5f, 0.5f, 0.5f);
        }

        matrixStackIn.scale(size[0], size[0], size[0]);
        RenderSystem.setShader(GameRenderer::getRendertypeEntityCutoutNoCullShader);
        model.model.Render(entityIn, bodyYaw, 0.0f, entityTrans, tickDelta, matrixStackIn, packedLightIn);
        matrixStackIn.popPose();
    }

    float[] sizeOfModel(MMDModelManager.Model model){
        float[] size = new float[2];
        size[0] = (model.properties.getProperty("size") == null) ? 1.0f : Float.valueOf(model.properties.getProperty("size"));
        size[1] = (model.properties.getProperty("size_in_inventory") == null) ? 1.0f : Float.valueOf(model.properties.getProperty("size_in_inventory"));
        return size;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}