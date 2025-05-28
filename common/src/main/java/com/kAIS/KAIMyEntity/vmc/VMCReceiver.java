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
// VMCReceiver.java (UDP 통신용)
package com.kAIS.KAIMyEntity.vmc;

import java.net.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.joml.Vector3f;
import org.joml.Quaternionf;

/**
 * VMC 프로토콜을 통해 모션 데이터를 받는 클래스
 */
public class VMCReceiver {
    private static final int DEFAULT_PORT = 39539; // VMC Protocol 기본 포트
    private DatagramSocket socket;
    private boolean isRunning = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private VMCDataManager vmcDataManager;
    
    public VMCReceiver() {
        this.vmcDataManager = VMCDataManager.getInstance();
    }
    
    public void startReceiving() {
        startReceiving(DEFAULT_PORT);
    }
    
    public void startReceiving(int port) {
        if (isRunning) {
            return;
        }
        
        try {
            socket = new DatagramSocket(port);
            isRunning = true;
            vmcDataManager.setConnected(true);
            
            executor.submit(() -> {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                while (isRunning && !socket.isClosed()) {
                    try {
                        socket.receive(packet);
                        processVMCData(new String(packet.getData(), 0, packet.getLength()));
                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("VMC 데이터 수신 오류: " + e.getMessage());
                        }
                    }
                }
            });
            
            System.out.println("VMC Receiver started on port " + port);
        } catch (SocketException e) {
            System.err.println("VMC Receiver 시작 실패: " + e.getMessage());
        }
    }
    
    public void stopReceiving() {
        isRunning = false;
        vmcDataManager.setConnected(false);
        
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        
        executor.shutdown();
        System.out.println("VMC Receiver stopped");
    }
    
    private void processVMCData(String data) {
        // 실제 VMC 프로토콜 파싱 로직
        // 이 부분은 실제 VMC 프로토콜 명세에 맞게 구현해야 합니다
        
        try {
            // 예시: 간단한 JSON 파싱 (실제로는 OSC 메시지 형태일 수 있음)
            if (data.contains("bone")) {
                parseBoneData(data);
            } else if (data.contains("blendShape")) {
                parseBlendShapeData(data);
            }
        } catch (Exception e) {
            System.err.println("VMC 데이터 파싱 오류: " + e.getMessage());
        }
    }
    
    private void parseBoneData(String data) {
        // VMC 본 데이터 파싱
        // 실제 구현에서는 OSC 메시지 파서를 사용해야 함
        List<VMCBoneData> boneDataList = new ArrayList<>();
        
        // 예시 파싱 로직 (실제 VMC 프로토콜에 맞게 수정 필요)
        // "/VMC/Ext/Bone/Pos" 메시지 형태로 올 것으로 예상
        
        vmcDataManager.updateBoneData(boneDataList);
    }
    
    private void parseBlendShapeData(String data) {
        // VMC 블렌드셰이프 데이터 파싱
        List<VMCBlendShapeData> blendShapeDataList = new ArrayList<>();
        
        // 예시 파싱 로직 (실제 VMC 프로토콜에 맞게 수정 필요)
        // "/VMC/Ext/Blend/Val" 메시지 형태로 올 것으로 예상
        
        vmcDataManager.updateBlendShapeData(blendShapeDataList);
    }
    
    public boolean isConnected() {
        return isRunning && vmcDataManager.isConnected();
    }
}