// common/src/main/java/com/kAIS/KAIMyEntity/VRDetector.java
package com.kAIS.KAIMyEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class VRDetector {
    public static final Logger logger = LogManager.getLogger();
    public static boolean isVRLoaded = false;

    // ViveCraft 클래스 및 메서드 참조
    private static Class<?> vrPlayerModelClass;
    private static Class<?> vrPlayerModelWithArmsClass;
    private static Class<?> vrDataClass;
    private static Class<?> minecraftClientVRClass;

    // VR 상태 가져오기 메서드들
    private static Method getVRModeMethod;
    private static Method getControllerPoseMethod;
    private static Method getHeadPoseMethod;
    private static Method isSeatedMethod;
    private static Field vrDataField;

    static {
        try {
            // ViveCraft 핵심 클래스들 로드
            vrPlayerModelClass = Class.forName("org.vivecraft.client.render.VRPlayerModel");
            vrPlayerModelWithArmsClass = Class.forName("org.vivecraft.client.render.VRPlayerModel_WithArms");
            vrDataClass = Class.forName("org.vivecraft.client.VRData");
            minecraftClientVRClass = Class.forName("org.vivecraft.client.MinecraftClientVR");

            // VR 상태 접근 메서드들 가져오기
            getVRModeMethod = minecraftClientVRClass.getMethod("getVRMode");
            getControllerPoseMethod = vrDataClass.getMethod("getController", int.class);
            getHeadPoseMethod = vrDataClass.getMethod("getHead");
            isSeatedMethod = vrDataClass.getMethod("isSeated");
            vrDataField = minecraftClientVRClass.getField("vrData");

            isVRLoaded = true;
            logger.info("ViveCraft detected, VR API access enabled");
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            isVRLoaded = false;
            logger.info("ViveCraft not found, VR support disabled");
        }
    }

    public static boolean isVRPlayer(Object model) {
        if (!isVRLoaded) return false;
        try {
            String className = model.getClass().getName();
            return className.contains("VRPlayerModel");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isVRPlayerWithArms(Object model) {
        if (!isVRLoaded) return false;
        try {
            String className = model.getClass().getName();
            return className.contains("VRPlayerModel_WithArms");
        } catch (Exception e) {
            return false;
        }
    }

    // 새로운 VR API 접근 메서드들
    public static VRState getVRState() {
        if (!isVRLoaded) return VRState.NONE;

        try {
            Object minecraftVR = getMinecraftVRInstance();
            if (minecraftVR == null) return VRState.NONE;

            Object vrData = vrDataField.get(minecraftVR);
            if (vrData == null) return VRState.NONE;

            // VR 모드 확인
            String vrMode = (String) getVRModeMethod.invoke(minecraftVR);
            if (vrMode == null || vrMode.equals("UNSET")) return VRState.NONE;

            // 착석 상태 확인
            boolean seated = (Boolean) isSeatedMethod.invoke(vrData);
            return seated ? VRState.SEATED : VRState.STANDING;

        } catch (Exception e) {
            logger.warn("Failed to get VR state: " + e.getMessage());
            return VRState.NONE;
        }
    }

    public static VRControllerData getControllerData(int controllerIndex) {
        if (!isVRLoaded) return null;

        try {
            Object minecraftVR = getMinecraftVRInstance();
            if (minecraftVR == null) return null;

            Object vrData = vrDataField.get(minecraftVR);
            if (vrData == null) return null;

            Object controller = getControllerPoseMethod.invoke(vrData, controllerIndex);
            if (controller == null) return null;

            // 컨트롤러 데이터 추출 (리플렉션으로 위치, 회전 등)
            VRControllerData data = new VRControllerData();
            // TODO: 실제 ViveCraft API에 맞게 데이터 추출 구현
            return data;

        } catch (Exception e) {
            logger.warn("Failed to get controller data: " + e.getMessage());
            return null;
        }
    }

    public static VRHeadData getHeadData() {
        if (!isVRLoaded) return null;

        try {
            Object minecraftVR = getMinecraftVRInstance();
            if (minecraftVR == null) return null;

            Object vrData = vrDataField.get(minecraftVR);
            if (vrData == null) return null;

            Object head = getHeadPoseMethod.invoke(vrData);
            if (head == null) return null;

            // 헤드 데이터 추출
            VRHeadData data = new VRHeadData();
            // TODO: 실제 ViveCraft API에 맞게 데이터 추출 구현
            return data;

        } catch (Exception e) {
            logger.warn("Failed to get head data: " + e.getMessage());
            return null;
        }
    }

    private static Object getMinecraftVRInstance() {
        try {
            // MinecraftClientVR 인스턴스 가져오기
            // 실제 ViveCraft에서는 Minecraft.getInstance()를 캐스팅하거나
            // 다른 방법으로 접근할 수 있음
            return null; // TODO: 실제 구현 필요
        } catch (Exception e) {
            return null;
        }
    }

    // VR 데이터 클래스들
    public static class VRControllerData {
        public float posX, posY, posZ;
        public float rotX, rotY, rotZ, rotW;
        public boolean isTracking;
        public boolean isConnected;
    }

    public static class VRHeadData {
        public float posX, posY, posZ;
        public float rotX, rotY, rotZ, rotW;
        public boolean isTracking;
    }

    public enum VRState {
        NONE,
        SEATED,
        STANDING
    }
}