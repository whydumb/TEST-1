// common/src/main/java/com/kAIS/KAIMyEntity/renderer/VRPlayerRenderer.java
package com.kAIS.KAIMyEntity.renderer;

import com.kAIS.KAIMyEntity.VRDetector;

public class VRPlayerRenderer {

    public static boolean isVRPlayer(Object model) {
        return VRDetector.isVRPlayer(model);
    }

    public static VRState getVRState(Object model) {
        if (!VRDetector.isVRLoaded) return VRState.NONE;

        if (VRDetector.isVRPlayerWithArms(model)) {
            return VRState.THIRD_PERSON_STANDING;
        } else if (VRDetector.isVRPlayer(model)) {
            return VRState.THIRD_PERSON_SITTING;
        }
        return VRState.NONE;
    }

    public enum VRState {
        NONE,
        FIRST_PERSON,
        THIRD_PERSON_STANDING,
        THIRD_PERSON_SITTING
    }
}