package com.kAIS.KAIMyEntity.mixin;

import com.kAIS.KAIMyEntity.KAIMyEntityClient;
import com.kAIS.KAIMyEntity.NativeFunc;
import com.kAIS.KAIMyEntity.renderer.IMMDModel;
import com.kAIS.KAIMyEntity.renderer.MMDAnimManager;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager.ModelWithEntityData;
// VMC 관련 클래스 추가 (실제 구현에 맞게 조정 필요)
import com.kAIS.KAIMyEntity.vmc.VMCDataManager;
import com.kAIS.KAIMyEntity.vmc.VMCBoneData;
import com.kAIS.KAIMyEntity.vmc.VMCBlendShapeData;

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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.GameType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.kAIS.KAIMyEntity.NativeFunc;
@Mixin(PlayerRenderer.class)
public abstract class KAIMyEntityPlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static NativeFunc nf = NativeFunc.GetInst();
    public KAIMyEntityPlayerRendererMixin(EntityRendererProvider.Context ctx, PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = {"render"}, at = @At("HEAD"), cancellable = true)
    public void render(AbstractClientPlayer entityIn, float entityYaw, float tickDelta, PoseStack matrixStackIn, MultiBufferSource vertexConsumers, int packedLightIn, CallbackInfo ci) {
        Minecraft MCinstance = Minecraft.getInstance();
        IMMDModel model = null;
        float bodyYaw = Mth.rotLerp(tickDelta, entityIn.yBodyRotO, entityIn.yBodyRot);
        float bodyPitch = 0.0f;
        Vector3f entityTrans = new Vector3f(0.0f);
        MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + entityIn.getName().getString());
        if (m == null){
            m = MMDModelManager.GetModel("EntityPlayer");
        }
        if (m == null){
            super.render(entityIn, entityYaw, tickDelta, matrixStackIn, vertexConsumers, packedLightIn);
            return;
        } 
        if (m != null){
            model = m.model;
        }
        MMDModelManager.ModelWithEntityData mwed = (MMDModelManager.ModelWithEntityData) m;
        mwed.loadModelProperties(KAIMyEntityClient.reloadProperties);
        
        // VMC 데이터 가져오기
        VMCDataManager vmcData = VMCDataManager.getInstance();
        
        float sleepingPitch = mwed.properties.getProperty("sleepingPitch") == null ? 0.0f : Float.valueOf(mwed.properties.getProperty("sleepingPitch"));
        Vector3f sleepingTrans = mwed.properties.getProperty("sleepingTrans") == null ? new Vector3f(0.0f) : KAIMyEntityClient.str2Vec3f(mwed.properties.getProperty("sleepingTrans"));
        float flyingPitch = mwed.properties.getProperty("flyingPitch") == null ? 0.0f : Float.valueOf(mwed.properties.getProperty("flyingPitch"));
        Vector3f flyingTrans = mwed.properties.getProperty("flyingTrans") == null ? new Vector3f(0.0f) : KAIMyEntityClient.str2Vec3f(mwed.properties.getProperty("flyingTrans"));
        float swimmingPitch = mwed.properties.getProperty("swimmingPitch") == null ? 0.0f : Float.valueOf(mwed.properties.getProperty("swimmingPitch"));
        Vector3f swimmingTrans = mwed.properties.getProperty("swimmingTrans") == null ? new Vector3f(0.0f) : KAIMyEntityClient.str2Vec3f(mwed.properties.getProperty("swimmingTrans"));
        float crawlingPitch = mwed.properties.getProperty("crawlingPitch") == null ? 0.0f : Float.valueOf(mwed.properties.getProperty("crawlingPitch"));
        Vector3f crawlingTrans = mwed.properties.getProperty("crawlingTrans") == null ? new Vector3f(0.0f) : KAIMyEntityClient.str2Vec3f(mwed.properties.getProperty("crawlingTrans"));
        float[] size = sizeOfModel(mwed);

        if (model != null) {
            if (!mwed.entityData.playCustomAnim) {
                // VMC 오버라이드 확인
                if (!vmcData.enableVMCOverride) {
                    // 기존 게임 기반 애니메이션 로직
                    performDefaultAnimations(entityIn, mwed, bodyPitch, entityTrans, sleepingPitch, sleepingTrans, flyingPitch, flyingTrans, swimmingPitch, swimmingTrans, crawlingPitch, crawlingTrans);
                } else {
                    // VMC 데이터로 본 직접 제어
                    applyVMCBoneData(mwed, vmcData);
                }
                
                // 손 애니메이션은 VMC가 활성화되어도 게임 로직 유지 (필요에 따라 조정 가능)
                if (!vmcData.enableVMCOverride) {
                    performHandAnimations(entityIn, mwed);
                }
                
                // 기타 레이어 애니메이션
                if (!vmcData.enableVMCOverride) {
                    performOtherLayerAnimations(entityIn, mwed);
                }
            }
            
            // 표정은 항상 VMC 적용 (VMC가 활성화된 경우)
            if (vmcData.enableVMCOverride) {
                applyVMCBlendShapes(mwed, vmcData);
            }

            // 렌더링 로직 (기존 유지)
            performRendering(entityIn, entityYaw, tickDelta, matrixStackIn, vertexConsumers, packedLightIn, MCinstance, model, mwed, size, bodyYaw, bodyPitch, entityTrans);
        }
        
        ci.cancel();
    }
    
    /**
     * 기존 기본 애니메이션 로직을 별도 메서드로 분리
     */
    private void performDefaultAnimations(AbstractClientPlayer entityIn, MMDModelManager.ModelWithEntityData mwed, 
                                        float bodyPitch, Vector3f entityTrans, float sleepingPitch, Vector3f sleepingTrans, 
                                        float flyingPitch, Vector3f flyingTrans, float swimmingPitch, Vector3f swimmingTrans, 
                                        float crawlingPitch, Vector3f crawlingTrans) {
        // Layer 0 - 기본 애니메이션
        if (entityIn.getHealth() == 0.0f) {
            AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Die, 0);
        } else if (entityIn.isFallFlying()) {
            AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.ElytraFly, 0);
            bodyPitch = entityIn.getXRot() + flyingPitch;
            entityTrans = flyingTrans;
        } else if (entityIn.isSleeping()) {
            AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Sleep, 0);
            bodyPitch = sleepingPitch;
            entityTrans = sleepingTrans;
        } else if (entityIn.isPassenger()) {
            if(entityIn.getVehicle().getType() == EntityType.HORSE && (entityIn.getX() - entityIn.xo != 0.0f || entityIn.getZ() - entityIn.zo != 0.0f)){
                AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.OnHorse, 0);
            }else if(entityIn.getVehicle().getType() == EntityType.HORSE){
                AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Ride, 0);
            }else{
                AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Ride, 0);
            }
        } else if (entityIn.isSwimming()) {
            AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Swim, 0);
            bodyPitch = entityIn.getXRot() + swimmingPitch;
            entityTrans = swimmingTrans;
        } else if (entityIn.onClimbable()) {
            if(entityIn.getY() - entityIn.yo > 0){
                AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.OnClimbableUp, 0);
            }else if(entityIn.getY() - entityIn.yo < 0){
                AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.OnClimbableDown, 0);
            }else{
                AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.OnClimbable, 0);
            }
        } else if (entityIn.isSprinting() && !entityIn.isShiftKeyDown()) {
            AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Sprint, 0);
        } else if (entityIn.isVisuallyCrawling()){
            if(entityIn.getX() - entityIn.xo != 0.0f || entityIn.getZ() - entityIn.zo != 0.0f){
                AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Crawl, 0);
            }else {
                AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.LieDown, 0);
            }
            bodyPitch = crawlingPitch;
            entityTrans = crawlingTrans;
        } else if (entityIn.getX() - entityIn.xo != 0.0f || entityIn.getZ() - entityIn.zo != 0.0f) {
            AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Walk, 0);
        } else {
            AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Idle, 0);
        }
    }
    
    /**
     * 손 애니메이션 로직을 별도 메서드로 분리
     */
    private void performHandAnimations(AbstractClientPlayer entityIn, MMDModelManager.ModelWithEntityData mwed) {
        // Layer 1 - 손 애니메이션
        if(!entityIn.isUsingItem() && !entityIn.swinging || entityIn.isSleeping()){
            if (mwed.entityData.stateLayers[1] != MMDModelManager.EntityData.EntityState.Idle) {
                mwed.entityData.stateLayers[1] = MMDModelManager.EntityData.EntityState.Idle;
                mwed.model.ChangeAnim(0, 1);
            }
        }else{
            if((entityIn.getUsedItemHand() == InteractionHand.MAIN_HAND) && entityIn.isUsingItem()){
                String itemId = getItemId_in_ActiveHand(entityIn, InteractionHand.MAIN_HAND);
                CustomItemActiveAnim(mwed, MMDModelManager.EntityData.EntityState.ItemRight, itemId, "Right", "using", 1);
            }else if((entityIn.swingingArm == InteractionHand.MAIN_HAND) && entityIn.swinging){
                String itemId = getItemId_in_ActiveHand(entityIn, InteractionHand.MAIN_HAND);
                CustomItemActiveAnim(mwed, MMDModelManager.EntityData.EntityState.SwingRight, itemId, "Right", "swinging", 1);
            }else if((entityIn.getUsedItemHand() == InteractionHand.OFF_HAND) && entityIn.isUsingItem()){
                String itemId = getItemId_in_ActiveHand(entityIn, InteractionHand.OFF_HAND);
                CustomItemActiveAnim(mwed, MMDModelManager.EntityData.EntityState.ItemLeft, itemId, "Left", "using", 1);
            }else if((entityIn.swingingArm == InteractionHand.OFF_HAND) && entityIn.swinging){
                String itemId = getItemId_in_ActiveHand(entityIn, InteractionHand.OFF_HAND);
                CustomItemActiveAnim(mwed, MMDModelManager.EntityData.EntityState.SwingLeft, itemId, "Left", "swinging", 1);
            }
        }
    }
    
    /**
     * 기타 레이어 애니메이션 로직을 별도 메서드로 분리
     */
    private void performOtherLayerAnimations(AbstractClientPlayer entityIn, MMDModelManager.ModelWithEntityData mwed) {
        // Layer 2 - 웅크리기 등
        if (entityIn.isShiftKeyDown() && !entityIn.isVisuallyCrawling()) {
            AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Sneak, 2);
        } else {
            if (mwed.entityData.stateLayers[2] != MMDModelManager.EntityData.EntityState.Idle) {
                mwed.entityData.stateLayers[2] = MMDModelManager.EntityData.EntityState.Idle;
                mwed.model.ChangeAnim(0, 2);
            }
        }
    }
    
    /**
     * VMC 본 데이터 적용
     */
    private void applyVMCBoneData(MMDModelManager.ModelWithEntityData mwed, VMCDataManager vmcData) {
        // VMC에서 받은 본 데이터를 모델에 적용
        for (VMCBoneData boneData : vmcData.getBoneDataList()) {
            // 본 이름으로 해당 본을 찾아서 위치/회전 적용
            NativeFunc nf = NativeFunc.GetInst();
            long boneIndex = nf.GetBoneIndex(mwed.model.GetModelLong(), boneData.getBoneName());
            
            if (boneIndex != -1) {
                // 위치 적용
                nf.SetBonePosition(mwed.model.GetModelLong(), boneIndex, 
                    boneData.getPosition().x, boneData.getPosition().y, boneData.getPosition().z);
                
                // 회전 적용 (쿼터니언)
                nf.SetBoneRotation(mwed.model.GetModelLong(), boneIndex, 
                    boneData.getRotation().x, boneData.getRotation().y, 
                    boneData.getRotation().z, boneData.getRotation().w);
            }
        }
        
        // 본 업데이트 적용
        nf.UpdateBones(mwed.model.GetModelLong());
    }
    
    /**
     * VMC 블렌드셰이프(표정) 데이터 적용
     */
    private void applyVMCBlendShapes(MMDModelManager.ModelWithEntityData mwed, VMCDataManager vmcData) {
        // VMC에서 받은 블렌드셰이프 데이터를 모델에 적용
        for (VMCBlendShapeData blendData : vmcData.getBlendShapeDataList()) {
            NativeFunc nf = NativeFunc.GetInst();
            long morphIndex = nf.GetMorphIndex(mwed.model.GetModelLong(), blendData.getBlendShapeName());
            
            if (morphIndex != -1) {
                nf.SetMorphWeight(mwed.model.GetModelLong(), morphIndex, blendData.getWeight());
            }
        }
        
        // 모프 업데이트 적용
        NativeFunc nf = NativeFunc.GetInst();
        nf.UpdateMorphs(mwed.model.GetModelLong());
    }
    
    /**
     * 렌더링 로직을 별도 메서드로 분리
     */
    private void performRendering(AbstractClientPlayer entityIn, float entityYaw, float tickDelta, 
                                PoseStack matrixStackIn, MultiBufferSource vertexConsumers, int packedLightIn,
                                Minecraft MCinstance, IMMDModel model, MMDModelManager.ModelWithEntityData mwed, 
                                float[] size, float bodyYaw, float bodyPitch, Vector3f entityTrans) {
        
        // 기존 렌더링 로직 유지
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
            model.Render(entityIn, bodyYaw, bodyPitch, entityTrans, tickDelta, matrixStackIn, packedLightIn);
        }
        
        // 아이템 렌더링
        renderHandItems(entityIn, matrixStackIn, vertexConsumers, packedLightIn, mwed);
    }
    
    /**
     * 손에 든 아이템 렌더링을 별도 메서드로 분리
     */
    private void renderHandItems(AbstractClientPlayer entityIn, PoseStack matrixStackIn, 
                               MultiBufferSource vertexConsumers, int packedLightIn, 
                               MMDModelManager.ModelWithEntityData mwed) {
        NativeFunc nf = NativeFunc.GetInst();
        float rotationDegree = 0.0f;
        
        // 오른손 아이템
        nf.GetRightHandMat(mwed.model.GetModelLong(), mwed.entityData.rightHandMat);
        matrixStackIn.pushPose();
        matrixStackIn.last().pose().mul(DataToMat(nf, mwed.entityData.rightHandMat));
        rotationDegree = ItemRotaionDegree(entityIn, mwed, InteractionHand.MAIN_HAND, "z");
        matrixStackIn.mulPose(new Quaternionf().rotateZ(rotationDegree*((float)Math.PI / 180F)));
        rotationDegree = ItemRotaionDegree(entityIn, mwed, InteractionHand.MAIN_HAND, "x");
        matrixStackIn.mulPose(new Quaternionf().rotateX(rotationDegree*((float)Math.PI / 180F)));
        matrixStackIn.scale(10.0f, 10.0f, 10.0f);
        Minecraft.getInstance().getItemRenderer().renderStatic(entityIn, entityIn.getMainHandItem(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, matrixStackIn, vertexConsumers, entityIn.level(), packedLightIn, OverlayTexture.NO_OVERLAY, 0);
        matrixStackIn.popPose();

        // 왼손 아이템
        nf.GetLeftHandMat(mwed.model.GetModelLong(), mwed.entityData.leftHandMat);
        matrixStackIn.pushPose();
        matrixStackIn.last().pose().mul(DataToMat(nf, mwed.entityData.leftHandMat));
        rotationDegree = ItemRotaionDegree(entityIn, mwed, InteractionHand.OFF_HAND, "z");
        matrixStackIn.mulPose(new Quaternionf().rotateZ(rotationDegree*((float)Math.PI / 180F)));
        rotationDegree = ItemRotaionDegree(entityIn, mwed, InteractionHand.OFF_HAND, "x");
        matrixStackIn.mulPose(new Quaternionf().rotateX(rotationDegree*((float)Math.PI / 180F)));
        matrixStackIn.scale(10.0f, 10.0f, 10.0f);
        Minecraft.getInstance().getItemRenderer().renderStatic(entityIn, entityIn.getOffhandItem(), ItemDisplayContext.THIRD_PERSON_LEFT_HAND, true, matrixStackIn, vertexConsumers, entityIn.level(), packedLightIn, OverlayTexture.NO_OVERLAY, 0);
        matrixStackIn.popPose();
    }

    // 기존 메서드들은 그대로 유지
    String getItemId_in_ActiveHand(AbstractClientPlayer entityIn, InteractionHand hand) {
        String descriptionId = entityIn.getItemInHand(hand).getItem().getDescriptionId();
        String result = descriptionId.substring(descriptionId.indexOf(".") + 1);
        return result;
    }

    void AnimStateChangeOnce(MMDModelManager.ModelWithEntityData model, MMDModelManager.EntityData.EntityState targetState, Integer layer) {
        String Property = MMDModelManager.EntityData.stateProperty.get(targetState);
        if (model.entityData.stateLayers[layer] != targetState) {
            model.entityData.stateLayers[layer] = targetState;
            model.model.ChangeAnim(MMDAnimManager.GetAnimModel(model.model, Property), layer);
        }
    }

    void CustomItemActiveAnim(MMDModelManager.ModelWithEntityData model, MMDModelManager.EntityData.EntityState targetState, String itemName, String activeHand, String handState, Integer layer) {
        long anim = MMDAnimManager.GetAnimModel(model.model, String.format("itemActive_%s_%s_%s", itemName, activeHand, handState));
        if (anim != 0) {
            if (model.entityData.stateLayers[layer] != targetState) {
                model.entityData.stateLayers[layer] = targetState;
                model.model.ChangeAnim(anim, layer);
            }
            return;
        }
        if (targetState == MMDModelManager.EntityData.EntityState.ItemRight || targetState == MMDModelManager.EntityData.EntityState.SwingRight) {
            AnimStateChangeOnce(model, MMDModelManager.EntityData.EntityState.SwingRight, layer);
        } else if (targetState == MMDModelManager.EntityData.EntityState.ItemLeft || targetState == MMDModelManager.EntityData.EntityState.SwingLeft) {
            AnimStateChangeOnce(model, MMDModelManager.EntityData.EntityState.SwingLeft, layer);
        }
    }
    
    float DataToFloat(NativeFunc nf, long data, long pos)
    {
        int temp = 0;
        temp |= nf.ReadByte(data, pos) & 0xff;
        temp |= (nf.ReadByte(data, pos + 1) & 0xff) << 8;
        temp |= (nf.ReadByte(data, pos + 2) & 0xff) << 16;
        temp |= (nf.ReadByte(data, pos + 3) & 0xff) << 24;
        return Float.intBitsToFloat(temp);
    }
    
    Matrix4f DataToMat(NativeFunc nf, long data)
    {
        Matrix4f result = new Matrix4f(
            DataToFloat(nf, data, 0),DataToFloat(nf, data, 16),DataToFloat(nf, data, 32),DataToFloat(nf, data, 48),
            DataToFloat(nf, data, 4),DataToFloat(nf, data, 20),DataToFloat(nf, data, 36),DataToFloat(nf, data, 52),
            DataToFloat(nf, data, 8),DataToFloat(nf, data, 24),DataToFloat(nf, data, 40),DataToFloat(nf, data, 56),
            DataToFloat(nf, data, 12),DataToFloat(nf, data, 28),DataToFloat(nf, data, 44),DataToFloat(nf, data, 60)
        );
        result.transpose();
        return result;
    }

    float ItemRotaionDegree(AbstractClientPlayer entityIn, ModelWithEntityData mwed, InteractionHand iHand, String axis){
        float result = 0.0f;
        String itemId;
        String strHand;
        String handState;
        
        itemId = getItemId_in_ActiveHand(entityIn,iHand);

        if (iHand == InteractionHand.MAIN_HAND){
            strHand = "Right";
        } else {
            strHand = "Left";
        }

        if ((iHand == entityIn.getUsedItemHand()) && (entityIn.isUsingItem())){
            handState = "using";
        } else if ((iHand == entityIn.swingingArm) && (entityIn.swinging)){
            handState = "swinging";
        } else {
            handState = "idle";
        }

        if (mwed.properties.getProperty(itemId + "_" + strHand + "_" + handState + "_" + axis) != null ){
            result = Float.valueOf(mwed.properties.getProperty(itemId + "_" + strHand + "_" + handState + "_" + axis));
        } else if (mwed.properties.getProperty("default_" + axis) != null){
            result = Float.valueOf(mwed.properties.getProperty("default_" + axis));
        }
        
        return result;
    }

    float[] sizeOfModel(ModelWithEntityData mwed){
        float[] size = new float[2]; // "new new float[2]" -> "new float[2]"로 수정
        size[0] = (mwed.properties.getProperty("size") == null) ? 1.0f : Float.valueOf(mwed.properties.getProperty("size"));
        size[1] = (mwed.properties.getProperty("size_in_inventory") == null) ? 1.0f : Float.valueOf(mwed.properties.getProperty("size_in_inventory"));
        return size;
    }
}