// common/src/main/java/com/kAIS/KAIMyEntity/VRDetector.java
package com.kAIS.KAIMyEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VRDetector {
    public static final Logger logger = LogManager.getLogger();
    public static boolean isVRLoaded = false;

    static {
        try {
            // ViveCraft 클래스 존재 확인
            Class.forName("org.vivecraft.client.render.VRPlayerModel");
            Class.forName("org.vivecraft.client.render.VRPlayerModel_WithArms");
            isVRLoaded = true;
            logger.info("ViveCraft detected, enabling VR compatibility");
        } catch (ClassNotFoundException e) {
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
}