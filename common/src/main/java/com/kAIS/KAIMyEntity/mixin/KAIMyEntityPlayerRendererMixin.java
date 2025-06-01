package com.kAIS.KAIMyEntity.mixin;

import com.kAIS.KAIMyEntity.KAIMyEntityClient;
import com.kAIS.KAIMyEntity.renderer.IMMDModel;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager.Model;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class KAIMyEntityPlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public KAIMyEntityPlayerRendererMixin(EntityRendererProvider.Context ctx, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = {"render"}, at = @At("HEAD"), cancellable = true)
    public void render(AbstractClientPlayer entityIn, float entityYaw, float tickDelta, PoseStack matrixStackIn, MultiBufferSource vertexConsumers, int packedLightIn, CallbackInfo ci) {
        Minecraft MCinstance = Minecraft.getInstance();
        IMMDModel model = null;
        float bodyYaw = Mth.rotLerp(tickDelta, entityIn.yBodyRotO, entityIn.yBodyRot);
        Vector3f entityTrans = new Vector3f(0.0f);

        MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + entityIn.getName().getString());
        if (m == null){
            m = MMDModelManager.GetModel("EntityPlayer");
        }
        if (m == null){
            super.render(entityIn, entityYaw, tickDelta, matrixStackIn, vertexConsumers, packedLightIn);
            return;
        }

        model = m.model;
        m.loadModelProperties(KAIMyEntityClient.reloadProperties);
        float[] size = sizeOfModel(m);

        if (model != null) {
            if(KAIMyEntityClient.calledFrom(6).contains("InventoryScreen") || KAIMyEntityClient.calledFrom(6).contains("class_490")){
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                PoseStack PTS_modelViewStack = new PoseStack();
                PTS_modelViewStack.setIdentity();
                PTS_modelViewStack.mulPose(RenderSystem.getModelViewMatrix());
                PTS_modelViewStack.pushPose();
                int PosX_in_inventory;
                int PosY_in_inventory;
                if(MCinstance.gameMode.getPlayerMode() != GameType.CREATIVE && MCinstance.screen instanceof InventoryScreen){
                    PosX_in_inventory = ((InventoryScreen) MCinstance.screen).getRecipeBookComponent().updateScreenPosition(MCinstance.screen.width, 176);
                    PosY_in_inventory = (MCinstance.screen.height - 166) / 2;
                    PTS_modelViewStack.translate(PosX_in_inventory+51, PosY_in_inventory+75, 50);
                    PTS_modelViewStack.scale(1.5f, 1.5f, 1.5f);
                }else{
                    PosX_in_inventory = (MCinstance.screen.width - 121) / 2;
                    PosY_in_inventory = (MCinstance.screen.height - 195) / 2;
                    PTS_modelViewStack.translate(PosX_in_inventory+51, PosY_in_inventory+75, 50.0);
                }
                PTS_modelViewStack.scale(size[1], size[1], size[1]);
                PTS_modelViewStack.scale(20.0f,20.0f, -20.0f);
                Quaternionf quaternionf = (new Quaternionf()).rotateZ((float)Math.PI);
                Quaternionf quaternionf1 = (new Quaternionf()).rotateX(-entityIn.getXRot() * ((float)Math.PI / 180F));
                Quaternionf quaternionf2 = (new Quaternionf()).rotateY(-entityIn.yBodyRot * ((float)Math.PI / 180F));
                quaternionf.mul(quaternionf1);
                quaternionf.mul(quaternionf2);
                PTS_modelViewStack.mulPose(quaternionf);
                RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentShader);
                model.Render(entityIn, entityYaw, 0.0f, new Vector3f(0.0f), tickDelta, PTS_modelViewStack, packedLightIn);
                PTS_modelViewStack.popPose();
                matrixStackIn.mulPose(quaternionf2);
                matrixStackIn.scale(size[1], size[1], size[1]);
                matrixStackIn.scale(0.09f, 0.09f, 0.09f);
            }else{
                matrixStackIn.scale(size[0], size[0], size[0]);
                RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentShader);
                model.Render(entityIn, bodyYaw, 0.0f, entityTrans, tickDelta, matrixStackIn, packedLightIn);
            }
        }

        ci.cancel();
    }

    float[] sizeOfModel(Model model){
        float[] size = new float[2];
        size[0] = (model.properties.getProperty("size") == null) ? 1.0f : Float.valueOf(model.properties.getProperty("size"));
        size[1] = (model.properties.getProperty("size_in_inventory") == null) ? 1.0f : Float.valueOf(model.properties.getProperty("size_in_inventory"));
        return size;
    }
}