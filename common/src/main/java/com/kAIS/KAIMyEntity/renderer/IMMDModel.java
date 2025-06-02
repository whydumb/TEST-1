package com.kAIS.KAIMyEntity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

public interface IMMDModel {
    void Render(Entity entityIn, float entityYaw, float entityPitch, Vector3f entityTrans, float tickDelta, PoseStack mat, int packedLight);

    long GetModelLong();

    String GetModelDir();
}