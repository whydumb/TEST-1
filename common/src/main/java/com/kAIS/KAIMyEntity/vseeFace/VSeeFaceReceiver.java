package com.kAIS.KAIMyEntity.vseeFace;

import com.illposed.osc.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class VSeeFaceReceiver {
    private static final Logger logger = LogManager.getLogger();

    private OSCPortIn receiver;
    private boolean isRunning = false;
    private final int port;

    // VSeeFace 데이터 저장
    private final Map<String, Float> faceData = new ConcurrentHashMap<>();

    // VSeeFace OSC 주소들
    private static final String HEAD_ROTATION_X = "/VMC/Ext/Bone/Pos/Head";
    private static final String EYE_LEFT_X = "/VMC/Ext/Blend/Val/eyeLookInLeft";
    private static final String EYE_LEFT_Y = "/VMC/Ext/Blend/Val/eyeLookUpLeft";
    private static final String EYE_RIGHT_X = "/VMC/Ext/Blend/Val/eyeLookInRight";
    private static final String EYE_RIGHT_Y = "/VMC/Ext/Blend/Val/eyeLookUpRight";
    private static final String EYE_LEFT_BLINK = "/VMC/Ext/Blend/Val/eyeBlinkLeft";
    private static final String EYE_RIGHT_BLINK = "/VMC/Ext/Blend/Val/eyeBlinkRight";
    private static final String MOUTH_A = "/VMC/Ext/Blend/Val/mouthOpen";
    private static final String MOUTH_I = "/VMC/Ext/Blend/Val/mouthSmile";
    private static final String MOUTH_U = "/VMC/Ext/Blend/Val/mouthPucker";
    private static final String MOUTH_E = "/VMC/Ext/Blend/Val/mouthShrugLower";
    private static final String MOUTH_O = "/VMC/Ext/Blend/Val/mouthFunnel";

    public VSeeFaceReceiver(int port) {
        this.port = port;
        initializeFaceData();
    }

    private void initializeFaceData() {
        // 기본값으로 초기화
        faceData.put("headRotX", 0.0f);
        faceData.put("headRotY", 0.0f);
        faceData.put("headRotZ", 0.0f);
        faceData.put("eyeLeftX", 0.0f);
        faceData.put("eyeLeftY", 0.0f);
        faceData.put("eyeRightX", 0.0f);
        faceData.put("eyeRightY", 0.0f);
        faceData.put("eyeLeftBlink", 0.0f);
        faceData.put("eyeRightBlink", 0.0f);
        faceData.put("mouthA", 0.0f);
        faceData.put("mouthI", 0.0f);
        faceData.put("mouthU", 0.0f);
        faceData.put("mouthE", 0.0f);
        faceData.put("mouthO", 0.0f);
    }

    public void startReceiving() {
        if (isRunning) return;

        try {
            receiver = new OSCPortIn(port);

            // 머리 회전 데이터 핸들러
            OSCMessageListener headRotationListener = (time, message) -> {
                if (message.getArguments().size() >= 7) {
                    // VMC 프로토콜: x, y, z, qx, qy, qz, qw
                    float qx = ((Number) message.getArguments().get(3)).floatValue();
                    float qy = ((Number) message.getArguments().get(4)).floatValue();
                    float qz = ((Number) message.getArguments().get(5)).floatValue();
                    float qw = ((Number) message.getArguments().get(6)).floatValue();

                    // 쿼터니언을 오일러각으로 변환
                    float[] eulerAngles = quaternionToEuler(qx, qy, qz, qw);
                    faceData.put("headRotX", eulerAngles[0]);
                    faceData.put("headRotY", eulerAngles[1]);
                    faceData.put("headRotZ", eulerAngles[2]);
                }
            };

            // 블렌드 셰이프 데이터 핸들러
            OSCMessageListener blendShapeListener = (time, message) -> {
                String address = message.getAddress();
                if (message.getArguments().size() >= 1) {
                    float value = ((Number) message.getArguments().get(0)).floatValue();

                    switch (address) {
                        case EYE_LEFT_BLINK:
                            faceData.put("eyeLeftBlink", value);
                            break;
                        case EYE_RIGHT_BLINK:
                            faceData.put("eyeRightBlink", value);
                            break;
                        case MOUTH_A:
                            faceData.put("mouthA", value);
                            break;
                        case MOUTH_I:
                            faceData.put("mouthI", value);
                            break;
                        case MOUTH_U:
                            faceData.put("mouthU", value);
                            break;
                        case MOUTH_E:
                            faceData.put("mouthE", value);
                            break;
                        case MOUTH_O:
                            faceData.put("mouthO", value);
                            break;
                    }
                }
            };

            // 리스너 등록
            receiver.addListener(HEAD_ROTATION_X, headRotationListener);
            receiver.addListener(EYE_LEFT_BLINK, blendShapeListener);
            receiver.addListener(EYE_RIGHT_BLINK, blendShapeListener);
            receiver.addListener(MOUTH_A, blendShapeListener);
            receiver.addListener(MOUTH_I, blendShapeListener);
            receiver.addListener(MOUTH_U, blendShapeListener);
            receiver.addListener(MOUTH_E, blendShapeListener);
            receiver.addListener(MOUTH_O, blendShapeListener);

            receiver.startListening();
            isRunning = true;
            logger.info("VSeeFace OSC receiver started on port " + port);

        } catch (SocketException e) {
            logger.error("Failed to start VSeeFace OSC receiver", e);
        }
    }

    public void stopReceiving() {
        if (receiver != null && isRunning) {
            receiver.stopListening();
            receiver.close();
            isRunning = false;
            logger.info("VSeeFace OSC receiver stopped");
        }
    }

    // 쿼터니언을 오일러각으로 변환
    private float[] quaternionToEuler(float x, float y, float z, float w) {
        float[] angles = new float[3];

        // Roll (x-axis rotation)
        double sinr_cosp = 2 * (w * x + y * z);
        double cosr_cosp = 1 - 2 * (x * x + y * y);
        angles[2] = (float) Math.atan2(sinr_cosp, cosr_cosp);

        // Pitch (y-axis rotation)
        double sinp = 2 * (w * y - z * x);
        if (Math.abs(sinp) >= 1)
            angles[0] = (float) Math.copySign(Math.PI / 2, sinp);
        else
            angles[0] = (float) Math.asin(sinp);

        // Yaw (z-axis rotation)
        double siny_cosp = 2 * (w * z + x * y);
        double cosy_cosp = 1 - 2 * (y * y + z * z);
        angles[1] = (float) Math.atan2(siny_cosp, cosy_cosp);

        return angles;
    }

    // 현재 얼굴 데이터 가져오기
    public VSeeFaceData getCurrentFaceData() {
        return new VSeeFaceData(
                faceData.get("headRotX"),
                faceData.get("headRotY"),
                faceData.get("headRotZ"),
                faceData.get("eyeLeftBlink"),
                faceData.get("eyeRightBlink"),
                faceData.get("mouthA"),
                faceData.get("mouthI"),
                faceData.get("mouthU"),
                faceData.get("mouthE"),
                faceData.get("mouthO")
        );
    }

    public boolean isRunning() {
        return isRunning;
    }

    // VSeeFace 데이터 구조체
    public static class VSeeFaceData {
        public final float headRotX, headRotY, headRotZ;
        public final float eyeLeftBlink, eyeRightBlink;
        public final float mouthA, mouthI, mouthU, mouthE, mouthO;

        public VSeeFaceData(float headRotX, float headRotY, float headRotZ,
                            float eyeLeftBlink, float eyeRightBlink,
                            float mouthA, float mouthI, float mouthU, float mouthE, float mouthO) {
            this.headRotX = headRotX;
            this.headRotY = headRotY;
            this.headRotZ = headRotZ;
            this.eyeLeftBlink = eyeLeftBlink;
            this.eyeRightBlink = eyeRightBlink;
            this.mouthA = mouthA;
            this.mouthI = mouthI;
            this.mouthU = mouthU;
            this.mouthE = mouthE;
            this.mouthO = mouthO;
        }
    }
}