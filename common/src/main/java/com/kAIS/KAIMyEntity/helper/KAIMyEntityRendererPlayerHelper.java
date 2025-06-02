package com.kAIS.KAIMyEntity.helper;

import com.kAIS.KAIMyEntity.NativeFunc;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * KAIMyEntity 플레이어 렌더링 도우미 클래스
 * 커스텀 애니메이션과 물리 리셋 기능을 제공합니다.
 */
public class KAIMyEntityRendererPlayerHelper {
    private static final Logger logger = LogManager.getLogger();
    private static NativeFunc nf;

    static {
        try {
            nf = NativeFunc.GetInst();
        } catch (Exception e) {
            logger.error("Failed to initialize NativeFunc", e);
        }
    }

    /**
     * 플레이어에게 커스텀 애니메이션을 적용합니다.
     *
     * @param player 대상 플레이어
     * @param animId 애니메이션 ID ("1", "2", "3", "4")
     */
    public static void CustomAnim(Player player, String animId) {
        if (player == null || animId == null) {
            logger.warn("CustomAnim called with null parameters");
            return;
        }

        try {
            String modelName = "EntityPlayer_" + player.getName().getString();
            MMDModelManager.Model model = MMDModelManager.GetModel(modelName);

            if (model == null || model.model == null) {
                logger.debug("Model not found for player: {}", player.getName().getString());
                return;
            }

            long modelPtr = model.model.GetModelLong();
            if (modelPtr == 0) {
                logger.warn("Invalid model pointer for player: {}", player.getName().getString());
                return;
            }

            // 커스텀 애니메이션 파일 이름 생성
            String animFileName = "custom_" + animId + ".vmd";

            logger.info("Playing custom animation {} for player {}", animId, player.getName().getString());

            // TODO: 실제 네이티브 함수 구현 필요
            // 현재는 네이티브 함수가 없으므로 로그만 출력
            if (nf != null) {
                // nf.PlayCustomAnimation(modelPtr, animFileName);
                // 또는 기존 애니메이션 시스템 사용
                logger.debug("CustomAnim: Would play {} for model {}", animFileName, modelPtr);
            }

            // 애니메이션 상태 설정 (예시)
            setAnimationState(model, "custom", animId);

        } catch (Exception e) {
            logger.error("Error in CustomAnim for player {} with anim {}",
                    player.getName().getString(), animId, e);
        }
    }

    /**
     * 플레이어 모델의 물리 시뮬레이션을 리셋합니다.
     *
     * @param player 대상 플레이어
     */
    public static void ResetPhysics(Player player) {
        if (player == null) {
            logger.warn("ResetPhysics called with null player");
            return;
        }

        try {
            String modelName = "EntityPlayer_" + player.getName().getString();
            MMDModelManager.Model model = MMDModelManager.GetModel(modelName);

            if (model == null || model.model == null) {
                logger.debug("Model not found for player: {}", player.getName().getString());
                return;
            }

            long modelPtr = model.model.GetModelLong();
            if (modelPtr == 0) {
                logger.warn("Invalid model pointer for player: {}", player.getName().getString());
                return;
            }

            logger.info("Resetting physics for player: {}", player.getName().getString());

            // 네이티브 함수를 통한 물리 리셋
            if (nf != null) {
                nf.ResetModelPhysics(modelPtr);
                logger.debug("Physics reset completed for model: {}", modelPtr);
            } else {
                logger.warn("NativeFunc not available, cannot reset physics");
            }

        } catch (Exception e) {
            logger.error("Error in ResetPhysics for player {}",
                    player.getName().getString(), e);
        }
    }

    /**
     * 플레이어에게 특정 애니메이션을 재생합니다.
     *
     * @param player 대상 플레이어
     * @param animationType 애니메이션 타입 (예: "idle", "walk", "run")
     */
    public static void PlayAnimation(Player player, String animationType) {
        if (player == null || animationType == null) {
            logger.warn("PlayAnimation called with null parameters");
            return;
        }

        try {
            String modelName = "EntityPlayer_" + player.getName().getString();
            MMDModelManager.Model model = MMDModelManager.GetModel(modelName);

            if (model == null || model.model == null) {
                return;
            }

            long modelPtr = model.model.GetModelLong();
            if (modelPtr == 0) {
                return;
            }

            String animFileName = animationType + ".vmd";
            logger.debug("Playing animation {} for player {}", animationType, player.getName().getString());

            // TODO: 네이티브 애니메이션 재생 함수 구현
            // nf.PlayAnimation(modelPtr, animFileName);

        } catch (Exception e) {
            logger.error("Error playing animation {} for player {}",
                    animationType, player.getName().getString(), e);
        }
    }

    /**
     * 플레이어 모델의 특정 본에 회전을 적용합니다.
     *
     * @param player 대상 플레이어
     * @param boneName 본 이름
     * @param rotX X축 회전 (라디안)
     * @param rotY Y축 회전 (라디안)
     * @param rotZ Z축 회전 (라디안)
     */
    public static void SetBoneRotation(Player player, String boneName, float rotX, float rotY, float rotZ) {
        if (player == null || boneName == null) {
            return;
        }

        try {
            String modelName = "EntityPlayer_" + player.getName().getString();
            MMDModelManager.Model model = MMDModelManager.GetModel(modelName);

            if (model == null || model.model == null) {
                return;
            }

            long modelPtr = model.model.GetModelLong();
            if (modelPtr == 0) {
                return;
            }

            if (nf != null) {
                nf.SetBoneRotation(modelPtr, boneName, rotX, rotY, rotZ);
            }

        } catch (Exception e) {
            logger.error("Error setting bone rotation for player {}", player.getName().getString(), e);
        }
    }

    /**
     * 플레이어 모델의 모프(표정) 가중치를 설정합니다.
     *
     * @param player 대상 플레이어
     * @param morphName 모프 이름
     * @param weight 가중치 (0.0 ~ 1.0)
     */
    public static void SetMorphWeight(Player player, String morphName, float weight) {
        if (player == null || morphName == null) {
            return;
        }

        try {
            String modelName = "EntityPlayer_" + player.getName().getString();
            MMDModelManager.Model model = MMDModelManager.GetModel(modelName);

            if (model == null || model.model == null) {
                return;
            }

            long modelPtr = model.model.GetModelLong();
            if (modelPtr == 0) {
                return;
            }

            if (nf != null) {
                nf.SetMorphWeight(modelPtr, morphName, weight);
            }

        } catch (Exception e) {
            logger.error("Error setting morph weight for player {}", player.getName().getString(), e);
        }
    }

    /**
     * 애니메이션 상태를 설정합니다. (내부 사용)
     */
    private static void setAnimationState(MMDModelManager.Model model, String type, String id) {
        try {
            // 모델의 속성에 현재 애니메이션 정보 저장
            if (model.properties != null) {
                model.properties.setProperty("current_animation_type", type);
                model.properties.setProperty("current_animation_id", id);
                model.properties.setProperty("animation_timestamp", String.valueOf(System.currentTimeMillis()));
            }
        } catch (Exception e) {
            logger.debug("Could not set animation state", e);
        }
    }

    /**
     * 플레이어 모델이 로드되어 있는지 확인합니다.
     *
     * @param player 확인할 플레이어
     * @return 모델이 로드되어 있으면 true
     */
    public static boolean isModelLoaded(Player player) {
        if (player == null) {
            return false;
        }

        try {
            String modelName = "EntityPlayer_" + player.getName().getString();
            MMDModelManager.Model model = MMDModelManager.GetModel(modelName);
            return model != null && model.model != null && model.model.GetModelLong() != 0;
        } catch (Exception e) {
            logger.debug("Error checking model loaded status", e);
            return false;
        }
    }

    /**
     * 현재 재생 중인 애니메이션 정보를 가져옵니다.
     *
     * @param player 대상 플레이어
     * @return 애니메이션 정보 문자열
     */
    public static String getCurrentAnimation(Player player) {
        if (player == null) {
            return "unknown";
        }

        try {
            String modelName = "EntityPlayer_" + player.getName().getString();
            MMDModelManager.Model model = MMDModelManager.GetModel(modelName);

            if (model == null || model.properties == null) {
                return "unknown";
            }

            String type = model.properties.getProperty("current_animation_type", "default");
            String id = model.properties.getProperty("current_animation_id", "0");

            return type + "_" + id;

        } catch (Exception e) {
            logger.debug("Error getting current animation", e);
            return "unknown";
        }
    }
}