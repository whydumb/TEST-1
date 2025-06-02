package com.kAIS.KAIMyEntity.neoforge.register;

import com.kAIS.KAIMyEntity.KAIMyEntityClient;
import com.kAIS.KAIMyEntity.neoforge.config.KAIMyEntityConfig;
import com.kAIS.KAIMyEntity.neoforge.network.KAIMyEntityNetworkPack;
import com.kAIS.KAIMyEntity.renderer.KAIMyEntityRenderFactory;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT)
public class KAIMyEntityRegisterClient {
    static final Logger logger = LogManager.getLogger();
    static KeyMapping keyCustomAnim1 = new KeyMapping("key.customAnim1", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.title");
    static KeyMapping keyCustomAnim2 = new KeyMapping("key.customAnim2", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.title");
    static KeyMapping keyCustomAnim3 = new KeyMapping("key.customAnim3", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.title");
    static KeyMapping keyCustomAnim4 = new KeyMapping("key.customAnim4", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.title");
    static KeyMapping keyReloadModels = new KeyMapping("key.reloadModels", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.title");
    static KeyMapping keyResetPhysics = new KeyMapping("key.resetPhysics", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.title");
    static KeyMapping keyReloadProperties = new KeyMapping("key.reloadProperties", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.title");
    static KeyMapping keyChangeProgram = new KeyMapping("key.changeProgram", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_0, "key.title");

    public static void Register() {
        Minecraft MCinstance = Minecraft.getInstance();
        RegisterRenderers RR = new RegisterRenderers();
        RegisterKeyMappingsEvent RKE = new RegisterKeyMappingsEvent(MCinstance.options);
        for (KeyMapping i : new KeyMapping[]{keyCustomAnim1, keyCustomAnim2, keyCustomAnim3, keyCustomAnim4, keyReloadModels, keyResetPhysics, keyReloadProperties})
            RKE.register(i);
        if(KAIMyEntityConfig.isMMDShaderEnabled.get())
            RKE.register(keyChangeProgram);

        File[] modelDirs = new File(MCinstance.gameDirectory, "KAIMyEntity").listFiles();
        if (modelDirs != null) {
            for (File i : modelDirs) {
                if (!i.getName().startsWith("EntityPlayer") && !i.getName().equals("DefaultAnim") && !i.getName().equals("Shader")) {
                    String mcEntityName = i.getName().replace('.', ':');
                    if (EntityType.byString(mcEntityName).isPresent()){
                        RR.registerEntityRenderer(EntityType.byString(mcEntityName).get(), new KAIMyEntityRenderFactory<>(mcEntityName));
                        logger.info(mcEntityName + " is present, rendering it.");
                    }else{
                        logger.warn(mcEntityName + " not present, ignore rendering it!");
                    }
                }
            }
        }
        logger.info("KAIMyEntityRegisterClient.Register() finished.");
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        Minecraft MCinstance = Minecraft.getInstance();
        LocalPlayer localPlayer = MCinstance.player;
        if (keyCustomAnim1.isDown()) {
            MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + localPlayer.getName().getString());
            if (m != null) {
                // TODO: 커스텀 애니메이션 기능 구현 예정
                logger.info("Custom animation 1 triggered for " + localPlayer.getName().getString());
                assert localPlayer != null;
                PacketDistributor.sendToServer(new KAIMyEntityNetworkPack(1, localPlayer.getGameProfile(), 1));
            }
        }
        if (keyCustomAnim2.isDown()) {
            MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + localPlayer.getName().getString());
            if (m != null) {
                // TODO: 커스텀 애니메이션 기능 구현 예정
                logger.info("Custom animation 2 triggered for " + localPlayer.getName().getString());
                assert localPlayer != null;
                PacketDistributor.sendToServer(new KAIMyEntityNetworkPack(1, localPlayer.getGameProfile(), 2));
            }
        }
        if (keyCustomAnim3.isDown()) {
            MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + localPlayer.getName().getString());
            if (m != null) {
                // TODO: 커스텀 애니메이션 기능 구현 예정
                logger.info("Custom animation 3 triggered for " + localPlayer.getName().getString());
                assert localPlayer != null;
                PacketDistributor.sendToServer(new KAIMyEntityNetworkPack(1, localPlayer.getGameProfile(), 3));
            }
        }
        if (keyCustomAnim4.isDown()) {
            MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + localPlayer.getName().getString());
            if (m != null) {
                // TODO: 커스텀 애니메이션 기능 구현 예정
                logger.info("Custom animation 4 triggered for " + localPlayer.getName().getString());
                assert localPlayer != null;
                PacketDistributor.sendToServer(new KAIMyEntityNetworkPack(1, localPlayer.getGameProfile(), 4));
            }
        }
        if (keyReloadModels.isDown()) {
            MMDModelManager.ReloadModel();
        }
        if (keyResetPhysics.isDown()) {
            MMDModelManager.Model m = MMDModelManager.GetModel("EntityPlayer_" + localPlayer.getName().getString());
            if (m != null) {
                // TODO: 물리 리셋 기능 구현 예정
                logger.info("Reset physics triggered for " + localPlayer.getName().getString());
                assert localPlayer != null;
                PacketDistributor.sendToServer(new KAIMyEntityNetworkPack(2, localPlayer.getGameProfile(), 0));
            }
        }
        if (keyReloadProperties.isDown()) {
            KAIMyEntityClient.reloadProperties = true;
        }
        if (keyChangeProgram.isDown() && KAIMyEntityConfig.isMMDShaderEnabled.get()) {
            KAIMyEntityClient.usingMMDShader = 1 - KAIMyEntityClient.usingMMDShader;

            if(KAIMyEntityClient.usingMMDShader == 0)
                MCinstance.gui.getChat().addMessage(Component.literal("Default shader"));
            if(KAIMyEntityClient.usingMMDShader == 1)
                MCinstance.gui.getChat().addMessage(Component.literal("MMDShader"));
        }
    }
}