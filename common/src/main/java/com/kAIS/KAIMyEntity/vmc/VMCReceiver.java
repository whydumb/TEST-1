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
 */
public class VMCReceiver {
    private static final Logger logger = LogManager.getLogger();
    private static final int DEFAULT_PORT = 39539;

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
        try {
            String message = new String(data, 0, length);

            if (message.contains("/VMC/Ext/Bone/Pos")) {
                parseBonePositionMessage(message);
            } else if (message.contains("/VMC/Ext/Blend/Val")) {
                parseBlendShapeMessage(message);
            }
        } catch (Exception e) {
            logger.error("VMC 데이터 파싱 오류: " + e.getMessage());
        }
    }

    private void parseBonePositionMessage(String message) {
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