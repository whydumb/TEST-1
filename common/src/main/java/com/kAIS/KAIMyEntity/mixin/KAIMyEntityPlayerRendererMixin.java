package com.kAIS.KAIMyEntity.mixin;

import com.kAIS.KAIMyEntity.KAIMyEntityClient;
import com.kAIS.KAIMyEntity.NativeFunc;
import com.kAIS.KAIMyEntity.renderer.IMMDModel;
import com.kAIS.KAIMyEntity.renderer.MMDAnimManager;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager.ModelWithEntityData;
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
                //Layer 0
                if (entityIn.getHealth() == 0.0f) {
                    AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Die, 0);
                } else if (entityIn.isFallFlying()) {
                    AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.ElytraFly, 0);
                    bodyPitch = entityIn.getXRot() + flyingPitch;
                    entityTrans = flyingTrans;
                } else if (entityIn.isSleeping()) {
                    AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Sleep, 0);
                    bodyYaw = entityIn.getBedOrientation().toYRot() + 180.0f;
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

                //Layer 1
                if(!entityIn.isUsingItem() && !entityIn.swinging || entityIn.isSleeping()){
                    if (mwed.entityData.stateLayers[1] != MMDModelManager.EntityData.EntityState.Idle) {
                        mwed.entityData.stateLayers[1] = MMDModelManager.EntityData.EntityState.Idle;
                        model.ChangeAnim(0, 1);
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


                //Layer 2
                if (entityIn.isShiftKeyDown() && !entityIn.isVisuallyCrawling()) {
                    AnimStateChangeOnce(mwed, MMDModelManager.EntityData.EntityState.Sneak, 2);
                } else {
                    if (mwed.entityData.stateLayers[2] != MMDModelManager.EntityData.EntityState.Idle) {
                        mwed.entityData.stateLayers[2] = MMDModelManager.EntityData.EntityState.Idle;
                        model.ChangeAnim(0, 2);
                    }
                }
            }

            // 머리 각도 계산 (다리 제어 테스트 포함)
            float headAngleX = entityIn.getXRot();
            float headAngleY = (entityYaw - Mth.lerp(tickDelta, entityIn.yHeadRotO, entityIn.yHeadRot))%360.0f;

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

            if(KAIMyEntityClient.calledFrom(6).contains("InventoryScreen") || KAIMyEntityClient.calledFrom(6).contains("class_490")){ // net.minecraft.class_490 == net.minecraft.client.gui.screen.ingame.InventoryScreen
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                PoseStack PTS_modelViewStack = new PoseStack(); //[P]osition [T]ex [S]hader
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

            NativeFunc nf = NativeFunc.GetInst();

            // 머리 각도 적용 (다리 제어 테스트 포함)
            if(KAIMyEntityClient.calledFrom(6).contains("InventoryScreen") || KAIMyEntityClient.calledFrom(6).contains("class_490")){
                nf.SetHeadAngle(model.GetModelLong(), headAngleX*((float)Math.PI / 180F), -headAngleY*((float)Math.PI / 180F), 0.0f, false);
            }else{
                nf.SetHeadAngle(model.GetModelLong(), headAngleX*((float)Math.PI / 180F), headAngleY*((float)Math.PI / 180F), 0.0f, true);
            }

            // 손 렌더링
            float rotationDegree = 0.0f;
            nf.GetRightHandMat(model.GetModelLong(), mwed.entityData.rightHandMat);
            matrixStackIn.pushPose();
            matrixStackIn.last().pose().mul(DataToMat(nf, mwed.entityData.rightHandMat));
            rotationDegree = ItemRotaionDegree(entityIn, mwed, InteractionHand.MAIN_HAND, "z");
            matrixStackIn.mulPose(new Quaternionf().rotateZ(rotationDegree*((float)Math.PI / 180F)));
            rotationDegree = ItemRotaionDegree(entityIn, mwed, InteractionHand.MAIN_HAND, "x");
            matrixStackIn.mulPose(new Quaternionf().rotateX(rotationDegree*((float)Math.PI / 180F)));
            matrixStackIn.scale(10.0f, 10.0f, 10.0f);
            Minecraft.getInstance().getItemRenderer().renderStatic(entityIn, entityIn.getMainHandItem(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, matrixStackIn, vertexConsumers, entityIn.level(), packedLightIn, OverlayTexture.NO_OVERLAY, 0);
            matrixStackIn.popPose();

            nf.GetLeftHandMat(model.GetModelLong(), mwed.entityData.leftHandMat);
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

        ci.cancel();//Added By FMyuchuan. | 隐藏模型脚下的史蒂夫
    }

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
        float[] size = new float[2];
        size[0] = (mwed.properties.getProperty("size") == null) ? 1.0f : Float.valueOf(mwed.properties.getProperty("size"));
        size[1] = (mwed.properties.getProperty("size_in_inventory") == null) ? 1.0f : Float.valueOf(mwed.properties.getProperty("size_in_inventory"));
        return size;
    }
}