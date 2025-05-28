// VMCDataManager.java
package com.kAIS.KAIMyEntity.vmc;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * VMC 데이터를 관리하는 싱글톤 클래스
 */
public class VMCDataManager {
    private static VMCDataManager instance;
    
    // VMC 오버라이드 활성화 여부
    public boolean enableVMCOverride = false;
    
    // 본 데이터 리스트
    private List<VMCBoneData> boneDataList = new CopyOnWriteArrayList<>();
    
    // 블렌드셰이프 데이터 리스트
    private List<VMCBlendShapeData> blendShapeDataList = new CopyOnWriteArrayList<>();
    
    // VMC 연결 상태
    private boolean isConnected = false;
    
    private VMCDataManager() {}
    
    public static VMCDataManager getInstance() {
        if (instance == null) {
            instance = new VMCDataManager();
        }
        return instance;
    }
    
    public List<VMCBoneData> getBoneDataList() {
        return new ArrayList<>(boneDataList);
    }
    
    public List<VMCBlendShapeData> getBlendShapeDataList() {
        return new ArrayList<>(blendShapeDataList);
    }
    
    public void updateBoneData(List<VMCBoneData> newBoneData) {
        this.boneDataList.clear();
        this.boneDataList.addAll(newBoneData);
    }
    
    public void updateBlendShapeData(List<VMCBlendShapeData> newBlendShapeData) {
        this.blendShapeDataList.clear();
        this.blendShapeDataList.addAll(newBlendShapeData);
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public void setConnected(boolean connected) {
        this.isConnected = connected;
        if (!connected) {
            // 연결이 끊어지면 VMC 오버라이드도 비활성화
            enableVMCOverride = false;
        }
    }
}

// VMCBoneData.java
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
    
    public VMCBoneData(String boneName, Vector3f position, Quaternionf rotation) {
        this.boneName = boneName;
        this.position = new Vector3f(position);
        this.rotation = new Quaternionf(rotation);
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
    
    public void setPosition(Vector3f position) {
        this.position.set(position);
    }
    
    public void setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
    }
}
