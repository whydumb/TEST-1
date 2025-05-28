// VMCDataManager.java
package com.kAIS.KAIMyEntity.vmc;

import com.kAIS.KAIMyEntity.vmc.VMCBoneData;
import com.kAIS.KAIMyEntity.vmc.VMCBlendShapeData;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * VMC 데이터를 관리하는 싱글톤 클래스
 */
public class VMCDataManager {
    private static VMCDataManager instance;

    // VMC 오버라이드 활성화 여부
    public boolean enableVMCOverride = false;

    // 본 데이터 맵 (본 이름 -> 데이터)
    public Map<String, VMCBoneData> vmcBones = new ConcurrentHashMap<>();

    // 블렌드셰이프 데이터 맵 (블렌드셰이프 이름 -> 가중치)
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
            // 연결이 끊어지면 VMC 오버라이드도 비활성화
            enableVMCOverride = false;
            vmcBones.clear();
            vmcBlendShapes.clear();
        }
    }

    /**
     * VMC 데이터가 유효한지 확인 (최근 1초 이내 업데이트)
     */
    public boolean isDataValid() {
        return System.currentTimeMillis() - lastUpdateTime < 1000;
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
        // VMC는 Unity 좌표계 (Y-up, Z-forward)
        // Minecraft는 Y-up, Z는 남쪽이 양수
        return new Vector3f(-position.x, position.y, -position.z);
    }

    /**
     * VMC 회전을 마인크래프트 회전으로 변환
     */
    public Quaternionf getMinecraftRotation() {
        // 좌표계 변환에 따른 회전 조정
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
        this.weight = Math.max(0.0f, Math.min(1.0f, weight)); // 0-1 범위로 제한
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

// VMCReceiver.java
package com.kAIS.KAIMyEntity.vmc;

        import java.net.*;
        import java.io.IOException;
        import java.util.concurrent.ExecutorService;
        import java.util.concurrent.Executors;
        import org.joml.Vector3f;
        import org.joml.Quaternionf;
        import org.apache.logging.log4j.LogManager;
        import org.apache.logging.log4j.Logger;

/**
 * VMC 프로토콜을 통해 모션 데이터를 받는 클래스
 * 실제 OSC 통신을 위해서는 JavaOSC 라이브러리 필요
 */
public class VMCReceiver {
    private static final Logger logger = LogManager.getLogger();
    private static final int DEFAULT_PORT = 39539; // VMC Protocol 기본 포트

    private DatagramSocket socket;
    private volatile boolean isRunning = false;
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
            logger.warn("VMC Receiver is already running");
            return;
        }

        try {
            socket = new DatagramSocket(port);
            isRunning = true;
            vmcDataManager.setConnected(true);

            executor.submit(() -> {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                logger.info("VMC Receiver started on port " + port);

                while (isRunning && !socket.isClosed()) {
                    try {
                        socket.receive(packet);
                        processVMCData(packet.getData(), packet.getLength());
                    } catch (IOException e) {
                        if (isRunning) {
                            logger.error("VMC 데이터 수신 오류: " + e.getMessage());
                        }
                    }
                }
            });

        } catch (SocketException e) {
            logger.error("VMC Receiver 시작 실패: " + e.getMessage());
            vmcDataManager.setConnected(false);
        }
    }

    public void stopReceiving() {
        isRunning = false;
        vmcDataManager.setConnected(false);

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        logger.info("VMC Receiver stopped");
    }

    private void processVMCData(byte[] data, int length) {
        // 실제 OSC 메시지 파싱
        // 이 부분은 JavaOSC 라이브러리를 사용하여 구현해야 합니다
        // 현재는 간단한 예시 구현

        try {
            String message = new String(data, 0, length);

            // VMC 프로토콜 메시지 파싱
            if (message.contains("/VMC/Ext/Bone/Pos")) {
                // 본 위치 데이터 파싱
                parseBonePositionMessage(message);
            } else if (message.contains("/VMC/Ext/Blend/Val")) {
                // 블렌드셰이프 데이터 파싱
                parseBlendShapeMessage(message);
            }
        } catch (Exception e) {
            logger.error("VMC 데이터 파싱 오류: " + e.getMessage());
        }
    }

    private void parseBonePositionMessage(String message) {
        // 실제 구현에서는 OSC 메시지 구조에 맞게 파싱
        // 예시: "/VMC/Ext/Bone/Pos" "Bone Name" x y z qx qy qz qw

        // 임시 파싱 로직 (실제로는 OSC 라이브러리 사용)
        String[] parts = message.split("\\s+");
        if (parts.length >= 8) {
            try {
                String boneName = parts[1].replace("\"", "");
                float x = Float.parseFloat(parts[2]);
                float y = Float.parseFloat(parts[3]);
                float z = Float.parseFloat(parts[4]);
                float qx = Float.parseFloat(parts[5]);
                float qy = Float.parseFloat(parts[6]);
                float qz = Float.parseFloat(parts[7]);
                float qw = Float.parseFloat(parts[8]);

                Vector3f position = new Vector3f(x, y, z);
                Quaternionf rotation = new Quaternionf(qx, qy, qz, qw);

                VMCBoneData boneData = new VMCBoneData(boneName, position, rotation);
                vmcDataManager.updateBoneData(boneName, boneData);

            } catch (NumberFormatException e) {
                logger.warn("Failed to parse bone data: " + e.getMessage());
            }
        }
    }

    private void parseBlendShapeMessage(String message) {
        // 실제 구현에서는 OSC 메시지 구조에 맞게 파싱
        // 예시: "/VMC/Ext/Blend/Val" "BlendShape Name" weight

        String[] parts = message.split("\\s+");
        if (parts.length >= 3) {
            try {
                String blendShapeName = parts[1].replace("\"", "");
                float weight = Float.parseFloat(parts[2]);

                vmcDataManager.updateBlendShapeData(blendShapeName, weight);

            } catch (NumberFormatException e) {
                logger.warn("Failed to parse blend shape data: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return isRunning && vmcDataManager.isVMCConnected();
    }
}