// VMCBlendShapeData.java
package com.kAIS.KAIMyEntity.vmc;

/**
 * VMC에서 받은 블렌드셰이프(표정) 데이터를 저장하는 클래스
 */
public class VMCBlendShapeData {
    private String blendShapeName;
    private float weight;
    
    public VMCBlendShapeData(String blendShapeName, float weight) {
        this.blendShapeName = blendShapeName;
        this.weight = weight;
    }
    
    public String getBlendShapeName() {
        return blendShapeName;
    }
    
    public float getWeight() {
        return weight;
    }
    
    public void setWeight(float weight) {
        this.weight = Math.max(0.0f, Math.min(1.0f, weight)); // 0-1 범위로 제한
    }
}
