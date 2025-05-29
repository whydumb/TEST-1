package com.kAIS.KAIMyEntity.vmc;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VMC 데이터를 관리하는 싱글톤 클래스
 */
public class VMCDataManager {
    private static VMCDataManager instance;

    // VMC 오버라이드 활성화 여부
    public boolean enableVMCOverride = false;

    // 본 데이터 맵
    public Map<String, VMCBoneData> vmcBones = new ConcurrentHashMap<>();

    // 블렌드셰이프 데이터 맵
    public Map<String, Float> vmcBlendShapes = new ConcurrentHashMap<>();

    // VMC 연결 상태
    private volatile boolean isConnected = false;

    // 마지막 업데이트 시간
    public volatile long lastUpdateTime = 0;

    private VMCDataManager() {}

    public static VMCDataManager getInstance() {
        if (instance == null) {
            synchronized (VMCDataManager.class) {
                if (instance == null) {
                    instance = new VMCDataManager();
                }
            }
        }
        return instance;
    }

    public List<VMCBoneData> getBoneDataList() {
        return new ArrayList<>(vmcBones.values());
    }

    public List<VMCBlendShapeData> getBlendShapeDataList() {
        List<VMCBlendShapeData> result = new ArrayList<>();
        for (Map.Entry<String, Float> entry : vmcBlendShapes.entrySet()) {
            result.add(new VMCBlendShapeData(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    public void updateBoneData(String boneName, VMCBoneData boneData) {
        vmcBones.put(boneName, boneData);
        lastUpdateTime = System.currentTimeMillis();
    }

    public void updateBlendShapeData(String blendShapeName, float weight) {
        vmcBlendShapes.put(blendShapeName, weight);
        lastUpdateTime = System.currentTimeMillis();
    }

    public boolean isVMCConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        this.isConnected = connected;
        if (!connected) {
            enableVMCOverride = false;
            vmcBones.clear();
            vmcBlendShapes.clear();
        }
    }

    public boolean isDataValid() {
        return System.currentTimeMillis() - lastUpdateTime < 1000;
    }

    public boolean isConnected() {
        return isConnected;
    }
}