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
import com.kAIS.KAIMyEntity.vseeFace.VSeeFaceManager;


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
            // VSeeFace 통합 처리
            handleVSeeFaceIntegration(model, entityIn, entityYaw, tickDelta);

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

    /**
     * VSeeFace 통합 처리 메서드
     */
    private void handleVSeeFaceIntegration(IMMDModel model, AbstractClientPlayer entityIn, float entityYaw, float tickDelta) {
        // VSeeFace 활성화 여부 확인
        VSeeFaceManager vseeFaceManager = VSeeFaceManager.getInstance();

        if (vseeFaceManager.isEnabled()) {
            // VSeeFace 데이터 적용
            vseeFaceManager.applyFaceDataToModel(model.GetModelLong());
        } else {
            // 기존 머리 각도 계산 (다리 제어 테스트 포함)
            float headAngleX = entityIn.getXRot();
            float headAngleY = (entityYaw - Mth.lerp(tickDelta, entityIn.yHeadRotO, entityIn.yHeadRot)) % 360.0f;

            // 다리 제어 모드 확인 (임시로 머리에 적용하여 테스트)
            try {
                if (com.kAIS.KAIMyEntity.neoforge.register.KAIMyEntityRegisterClient.isLegControlMode()) {
                    // 다리 제어 모드일 때 머리를 과장되게 움직임
                    float[] legAngles = com.kAIS.KAIMyEntity.neoforge.register.KAIMyEntityRegisterClient.calculateLegControlAngles(entityIn);
                    headAngleX = legAngles[0];  // 과장된 상하 움직임
                    headAngleY = legAngles[1];  // 과장된 좌우 움직임
                }
            } catch (Exception e) {
                // 다리 제어 클래스에 문제가 있어도 기본 머리 움직임은 유지
            }

            // 기존 머리 각도 제한
            if(headAngleX < -50.0f){
                headAngleX = -50.0f;
            }else if(50.0f < headAngleX){
                headAngleX = 50.0f;
            }
            if(headAngleY < -180.0f){
                headAngleY = headAngleY + 360.0f;
            } else if(180.0f < headAngleY){
                headAngleY = headAngleY - 360.0f;
            }
            if(headAngleY < -80.0f){
                headAngleY = -80.0f;
            }else if(80.0f < headAngleY){
                headAngleY = 80.0f;
            }

            // 기존 방식으로 머리 각도 적용
            if(KAIMyEntityClient.calledFrom(6).contains("InventoryScreen") || KAIMyEntityClient.calledFrom(6).contains("class_490")){
                // 인벤토리 화면에서의 머리 각도 적용 (nf.SetHeadAngle 메서드 필요)
                // nf.SetHeadAngle(model.GetModelLong(), headAngleX*((float)Math.PI / 180F), -headAngleY*((float)Math.PI / 180F), 0.0f, false);
            }else{
                // 게임 화면에서의 머리 각도 적용 (nf.SetHeadAngle 메서드 필요)
                // nf.SetHeadAngle(model.GetModelLong(), headAngleX*((float)Math.PI / 180F), headAngleY*((float)Math.PI / 180F), 0.0f, true);
            }
        }
    }

    float[] sizeOfModel(Model model){
        float[] size = new float[2];
        size[0] = (model.properties.getProperty("size") == null) ? 1.0f : Float.valueOf(model.properties.getProperty("size"));
        size[1] = (model.properties.getProperty("size_in_inventory") == null) ? 1.0f : Float.valueOf(model.properties.getProperty("size_in_inventory"));
        return size;
    }
}