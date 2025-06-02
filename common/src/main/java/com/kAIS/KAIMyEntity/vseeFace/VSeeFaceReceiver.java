package com.kAIS.KAIMyEntity.vseeFace;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

public class VSeeFaceReceiver {
    private static final Logger logger = LogManager.getLogger();

    // OSC 클래스들을 Object로 선언하여 컴파일 타임 의존성 제거
    private Object receiver;
    private boolean isRunning = false;
    private final int port;
    private boolean oscAvailable = false;

    // VSeeFace 데이터 저장
    private final Map<String, Float> faceData = new ConcurrentHashMap<>();

    // VSeeFace OSC 주소들
    private static final String HEAD_ROTATION = "/VMC/Ext/Bone/Pos/Head";
    private static final String EYE_LEFT_BLINK = "/VMC/Ext/Blend/Val/eyeBlinkLeft";
    private static final String EYE_RIGHT_BLINK = "/VMC/Ext/Blend/Val/eyeBlinkRight";
    private static final String MOUTH_A = "/VMC/Ext/Blend/Val/mouthOpen";
    private static final String MOUTH_I = "/VMC/Ext/Blend/Val/mouthSmile";
    private static final String MOUTH_U = "/VMC/Ext/Blend/Val/mouthPucker";
    private static final String MOUTH_E = "/VMC/Ext/Blend/Val/mouthShrugLower";
    private static final String MOUTH_O = "/VMC/Ext/Blend/Val/mouthFunnel";

    // OSC 클래스들
    private Class<?> oscPortInClass;
    private Class<?> oscMessageClass;
    private Class<?> oscMessageListenerClass;

    public VSeeFaceReceiver(int port) {
        this.port = port;
        initializeFaceData();
        checkOSCAvailability();
    }

    private void checkOSCAvailability() {
        try {
            oscPortInClass = Class.forName("com.illposed.osc.OSCPortIn");
            oscMessageClass = Class.forName("com.illposed.osc.OSCMessage");
            oscMessageListenerClass = Class.forName("com.illposed.osc.OSCMessageListener");
            oscAvailable = true;
            logger.info("OSC library detected and available");
        } catch (ClassNotFoundException e) {
            oscAvailable = false;
            logger.warn("OSC library not found. VSeeFace functionality will be disabled.");
        }
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
        if (isRunning) {
            logger.warn("VSeeFace receiver is already running");
            return;
        }

        if (!oscAvailable) {
            logger.error("Cannot start VSeeFace receiver: OSC library not available");
            return;
        }

        try {
            // OSCPortIn 생성
            Constructor<?> constructor = oscPortInClass.getConstructor(int.class);
            receiver = constructor.newInstance(port);

            // 머리 회전 데이터 핸들러 생성
            Object headRotationListener = createMessageListener((time, message) -> {
                try {
                    Method getArgumentsMethod = oscMessageClass.getMethod("getArguments");
                    List<Object> arguments = (List<Object>) getArgumentsMethod.invoke(message);

                    if (arguments.size() >= 7) {
                        // VMC 프로토콜: x, y, z, qx, qy, qz, qw
                        float qx = ((Number) arguments.get(3)).floatValue();
                        float qy = ((Number) arguments.get(4)).floatValue();
                        float qz = ((Number) arguments.get(5)).floatValue();
                        float qw = ((Number) arguments.get(6)).floatValue();

                        // 쿼터니언을 오일러각으로 변환
                        float[] eulerAngles = quaternionToEuler(qx, qy, qz, qw);
                        faceData.put("headRotX", eulerAngles[0]);
                        faceData.put("headRotY", eulerAngles[1]);
                        faceData.put("headRotZ", eulerAngles[2]);

                        logger.debug("Head rotation updated: X={}, Y={}, Z={}",
                                eulerAngles[0], eulerAngles[1], eulerAngles[2]);
                    }
                } catch (Exception e) {
                    logger.warn("Error processing head rotation data", e);
                }
            });

            // 블렌드 셰이프 데이터 핸들러 생성
            Object blendShapeListener = createMessageListener((time, message) -> {
                try {
                    Method getAddressMethod = oscMessageClass.getMethod("getAddress");
                    Method getArgumentsMethod = oscMessageClass.getMethod("getArguments");

                    String address = (String) getAddressMethod.invoke(message);
                    List<Object> arguments = (List<Object>) getArgumentsMethod.invoke(message);

                    if (arguments.size() >= 1) {
                        float value = Math.max(0.0f, Math.min(1.0f, ((Number) arguments.get(0)).floatValue()));

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
                            default:
                                logger.debug("Unknown blend shape: {} = {}", address, value);
                                break;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error processing blend shape data", e);
                }
            });

            // 리스너 등록
            Method addListenerMethod = oscPortInClass.getMethod("addListener", String.class, oscMessageListenerClass);
            addListenerMethod.invoke(receiver, HEAD_ROTATION, headRotationListener);
            addListenerMethod.invoke(receiver, EYE_LEFT_BLINK, blendShapeListener);
            addListenerMethod.invoke(receiver, EYE_RIGHT_BLINK, blendShapeListener);
            addListenerMethod.invoke(receiver, MOUTH_A, blendShapeListener);
            addListenerMethod.invoke(receiver, MOUTH_I, blendShapeListener);
            addListenerMethod.invoke(receiver, MOUTH_U, blendShapeListener);
            addListenerMethod.invoke(receiver, MOUTH_E, blendShapeListener);
            addListenerMethod.invoke(receiver, MOUTH_O, blendShapeListener);

            // 수신 시작
            Method startListeningMethod = oscPortInClass.getMethod("startListening");
            startListeningMethod.invoke(receiver);

            isRunning = true;
            logger.info("VSeeFace OSC receiver started successfully on port {}", port);

        } catch (Exception e) {
            logger.error("Failed to start VSeeFace OSC receiver on port {}: {}", port, e.getMessage());
            isRunning = false;

            if (e.getCause() != null && e.getCause().getMessage() != null &&
                    e.getCause().getMessage().contains("Address already in use")) {
                logger.info("Port {} is already in use. Please check if another VSeeFace receiver is running or try a different port.", port);
            }
        }
    }

    private Object createMessageListener(MessageHandler handler) {
        return java.lang.reflect.Proxy.newProxyInstance(
                oscMessageListenerClass.getClassLoader(),
                new Class<?>[]{oscMessageListenerClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("acceptMessage")) {
                        handler.handle(args[0], args[1]);
                    }
                    return null;
                });
    }

    @FunctionalInterface
    private interface MessageHandler {
        void handle(Object time, Object message);
    }

    public void stopReceiving() {
        if (isRunning && receiver != null) {
            try {
                Method stopListeningMethod = oscPortInClass.getMethod("stopListening");
                Method closeMethod = oscPortInClass.getMethod("close");

                stopListeningMethod.invoke(receiver);
                closeMethod.invoke(receiver);

                isRunning = false;
                logger.info("VSeeFace OSC receiver stopped");
            } catch (Exception e) {
                logger.error("Error stopping VSeeFace receiver", e);
            }
        }
    }

    /**
     * 쿼터니언을 오일러각으로 변환
     */
    private float[] quaternionToEuler(float x, float y, float z, float w) {
        float[] angles = new float[3];

        // Roll (x-axis rotation)
        double sinr_cosp = 2 * (w * x + y * z);
        double cosr_cosp = 1 - 2 * (x * x + y * y);
        angles[2] = (float) Math.atan2(sinr_cosp, cosr_cosp);

        // Pitch (y-axis rotation)
        double sinp = 2 * (w * y - z * x);
        if (Math.abs(sinp) >= 1) {
            angles[0] = (float) Math.copySign(Math.PI / 2, sinp);
        } else {
            angles[0] = (float) Math.asin(sinp);
        }

        // Yaw (z-axis rotation)
        double siny_cosp = 2 * (w * z + x * y);
        double cosy_cosp = 1 - 2 * (y * y + z * z);
        angles[1] = (float) Math.atan2(siny_cosp, cosy_cosp);

        // 라디안을 도로 변환
        angles[0] = (float) Math.toDegrees(angles[0]);
        angles[1] = (float) Math.toDegrees(angles[1]);
        angles[2] = (float) Math.toDegrees(angles[2]);

        return angles;
    }

    // 현재 얼굴 데이터 가져오기
    public VSeeFaceData getCurrentFaceData() {
        return new VSeeFaceData(
                faceData.getOrDefault("headRotX", 0.0f),
                faceData.getOrDefault("headRotY", 0.0f),
                faceData.getOrDefault("headRotZ", 0.0f),
                faceData.getOrDefault("eyeLeftBlink", 0.0f),
                faceData.getOrDefault("eyeRightBlink", 0.0f),
                faceData.getOrDefault("mouthA", 0.0f),
                faceData.getOrDefault("mouthI", 0.0f),
                faceData.getOrDefault("mouthU", 0.0f),
                faceData.getOrDefault("mouthE", 0.0f),
                faceData.getOrDefault("mouthO", 0.0f)
        );
    }

    public boolean isRunning() {
        return isRunning && oscAvailable;
    }

    public boolean isOSCAvailable() {
        return oscAvailable;
    }

    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        if (!isRunning || !oscAvailable || receiver == null) {
            return false;
        }

        try {
            Method isListeningMethod = oscPortInClass.getMethod("isListening");
            return (Boolean) isListeningMethod.invoke(receiver);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 마지막으로 데이터를 받은 시간을 확인하는 메서드
     */
    public boolean hasRecentData() {
        return faceData.values().stream().anyMatch(value -> Math.abs(value) > 0.001f);
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

        @Override
        public String toString() {
            return String.format("VSeeFaceData{head=[%.2f,%.2f,%.2f], eyes=[%.2f,%.2f], mouth=[%.2f,%.2f,%.2f,%.2f,%.2f]}",
                    headRotX, headRotY, headRotZ, eyeLeftBlink, eyeRightBlink, mouthA, mouthI, mouthU, mouthE, mouthO);
        }
    }
}