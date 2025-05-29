package com.kAIS.KAIMyEntity.vmc;

import org.joml.Vector3f;
import org.joml.Quaternionf;

/**
 * VMC에서 받은 본 데이터를 저장하는 클래스
 */
public class VMCBoneData {
    private String boneName;
    private Vector3f position;
    private Quaternionf rotation;
    public long timestamp;

    public VMCBoneData(String boneName, Vector3f position, Quaternionf rotation) {
        this.boneName = boneName;
        this.position = new Vector3f(position);
        this.rotation = new Quaternionf(rotation);
        this.timestamp = System.currentTimeMillis();
    }

    public String getBoneName() {
        return boneName;
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Quaternionf getRotation() {
        return new Quaternionf(rotation);
    }

    /**
     * VMC 좌표계를 마인크래프트 좌표계로 변환
     */
    public Vector3f getMinecraftPosition() {
        return new Vector3f(-position.x, position.y, -position.z);
    }

    /**
     * VMC 회전을 마인크래프트 회전으로 변환
     */
    public Quaternionf getMinecraftRotation() {
        return new Quaternionf(-rotation.x, rotation.y, -rotation.z, rotation.w);
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
        this.timestamp = System.currentTimeMillis();
    }

    public void setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
        this.timestamp = System.currentTimeMillis();
    }
}