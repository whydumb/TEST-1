package com.kAIS.KAIMyEntity;

import com.kAIS.KAIMyEntity.renderer.MMDAnimManager;
import com.kAIS.KAIMyEntity.renderer.MMDModelManager;
import com.kAIS.KAIMyEntity.renderer.MMDTextureManager;
import com.kAIS.KAIMyEntity.vmc.VMCDataManager;
import com.kAIS.KAIMyEntity.vmc.VMCReceiver;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Map;
import java.util.HashMap; // 추가된 import
import java.util.Collections; // 추가된 import
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.Font; // 추가된 import
import net.minecraft.client.renderer.MultiBufferSource; // 추가된 import
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;
import org.joml.Quaternionf;

public class KAIMyEntityClient {
    public static final Logger logger = LogManager.getLogger();
    public static int usingMMDShader = 0;
    public static boolean reloadProperties = false;
    static final Minecraft MCinstance = Minecraft.getInstance();
    static final String gameDirectory = MCinstance.gameDirectory.getAbsolutePath();
    static final int BUFFER = 512;
    static final long TOOBIG = 0x6400000; // Max size of unzipped data, 100MB
    static final int TOOMANY = 1024;      // Max number of files

    // VMC 관련 정적 인스턴스
    private static VMCReceiver vmcReceiver;
    private static VMCDataManager vmcDataManager;

    public static void initClient() {
        checkKAIMyEntityFolder();
        MMDModelManager.Init();
        MMDTextureManager.Init();
        MMDAnimManager.Init();

        // VMC 초기화
        initVMC();
    }

    /**
     * VMC 시스템 초기화
     */
    private static void initVMC() {
        logger.info("=== VMC 시스템 초기화 시작 ===");

        try {
            // VMC 데이터 매니저 초기화
            logger.info("VMC 데이터 매니저 초기화 중...");
            vmcDataManager = VMCDataManager.getInstance();
            if (vmcDataManager != null) {
                logger.info("✓ VMC 데이터 매니저 초기화 완료");
            } else {
                logger.error("❌ VMC 데이터 매니저 초기화 실패");
                return;
            }

            // VMC 리시버 초기화
            logger.info("VMC 리시버 초기화 중...");
            vmcReceiver = new VMCReceiver();
            if (vmcReceiver != null) {
                logger.info("✓ VMC 리시버 초기화 완료");
            } else {
                logger.error("❌ VMC 리시버 초기화 실패");
                return;
            }

            // VMC 설정 로드
            logger.info("VMC 설정 로드 중...");
            try {
                VMCConfig.loadConfig();
                logger.info("✓ VMC 설정 로드 완료");

                // 설정 정보 출력
                logger.info("VMC 포트: " + VMCConfig.getPort());
                logger.info("VMC 자동 시작: " + VMCConfig.isAutoStart());
                logger.info("VMC 디버그 모드: " + VMCConfig.isDebugMode());

            } catch (Exception e) {
                logger.warn("VMC 설정 로드 중 오류 발생, 기본 설정 사용", e);
                // 기본 설정으로 계속 진행
            }

            // 자동 시작 확인 및 실행
            try {
                if (VMCConfig.isAutoStart()) {
                    logger.info("VMC 자동 시작 설정이 활성화됨 - VMC 수신 시작");
                    startVMC();
                } else {
                    logger.info("VMC 자동 시작 비활성화 - 수동으로 시작 필요");
                }
            } catch (Exception e) {
                logger.warn("VMC 자동 시작 중 오류 발생", e);
            }

            // 초기화 완료 확인
            boolean vmcReady = (vmcDataManager != null) && (vmcReceiver != null);
            if (vmcReady) {
                logger.info("✅ VMC 시스템 초기화 성공");
            } else {
                logger.error("❌ VMC 시스템 초기화 부분 실패");
            }

        } catch (Exception e) {
            logger.error("❌ VMC 시스템 초기화 중 예외 발생", e);

            // 실패 시 안전하게 정리
            try {
                if (vmcReceiver != null) {
                    vmcReceiver.stopReceiving();
                }
            } catch (Exception cleanupException) {
                logger.warn("VMC 정리 중 오류", cleanupException);
            }

            vmcReceiver = null;
            vmcDataManager = null;
        }

        logger.info("=== VMC 시스템 초기화 완료 ===");
    }

    /**
     * VMC 수신 시작 (안전한 버전)
     */
    public static void startVMC() {
        logger.info("VMC 수신 시작 요청");

        try {
            if (vmcReceiver == null) {
                logger.error("VMC 리시버가 초기화되지 않음");
                return;
            }

            if (vmcReceiver.isConnected()) {
                logger.warn("VMC가 이미 연결되어 있음");
                return;
            }

            int port = VMCConfig.getPort();
            logger.info("VMC 수신 시작 - 포트: " + port);

            vmcReceiver.startReceiving(port);

            // 잠시 대기 후 연결 상태 확인
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 1초 대기
                    if (vmcReceiver.isConnected()) {
                        logger.info("✅ VMC 수신 시작 성공");
                    } else {
                        logger.warn("⚠️ VMC 수신 시작했지만 연결 상태 불명");
                    }
                } catch (InterruptedException e) {
                    logger.warn("VMC 상태 확인 중 인터럽트", e);
                }
            }).start();

        } catch (Exception e) {
            logger.error("VMC 수신 시작 중 오류", e);
        }
    }

    /**
     * VMC 수신 중지 (안전한 버전)
     */
    public static void stopVMC() {
        logger.info("VMC 수신 중지 요청");

        try {
            if (vmcReceiver != null) {
                vmcReceiver.stopReceiving();
                logger.info("✓ VMC 리시버 중지");
            }

            if (vmcDataManager != null) {
                vmcDataManager.enableVMCOverride = false;
                logger.info("✓ VMC 오버라이드 비활성화");
            }

            logger.info("✅ VMC 수신 중지 완료");

        } catch (Exception e) {
            logger.error("VMC 수신 중지 중 오류", e);
        }
    }

    /**
     * VMC 상태 확인
     */
    public static boolean isVMCReady() {
        return (vmcReceiver != null) && (vmcDataManager != null);
    }

    /**
     * VMC 연결 상태 확인
     */
    public static boolean isVMCConnected() {
        return isVMCReady() && vmcReceiver.isConnected();
    }

    /**
     * VMC 활성화 상태 확인
     */
    public static boolean isVMCActive() {
        return isVMCConnected() && vmcDataManager.enableVMCOverride;
    }

    /**
     * VMC 토글 (키바인딩용)
     */
    public static void toggleVMC() {
        if (vmcReceiver != null && vmcReceiver.isConnected()) {
            vmcDataManager.enableVMCOverride = !vmcDataManager.enableVMCOverride;
            String status = vmcDataManager.enableVMCOverride ? "활성화" : "비활성화";
            logger.info("VMC 오버라이드 " + status);

            // 플레이어에게 메시지 표시 (선택사항)
            if (MCinstance.player != null) {
                MCinstance.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("VMC " + status),
                        true
                );
            }
        } else {
            logger.warn("VMC가 연결되지 않았습니다");
            if (MCinstance.player != null) {
                MCinstance.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("VMC 연결 안됨"),
                        true
                );
            }
        }
    }

    /**
     * VMC 수신 시작
     */


    // VMC 본 매핑 테이블 (VMC 본 이름 → MMD 본 이름)
    private static final Map<String, String> VMC_TO_MMD_BONE_MAP;
    private static final Map<String, String> VMC_TO_MMD_BLEND_MAP;

    static {
        // VMC 본 매핑 테이블 초기화
        Map<String, String> boneMap = new HashMap<>();
        boneMap.put("Hips", "下半身");
        boneMap.put("Spine", "上半身");
        boneMap.put("Chest", "上半身2");
        boneMap.put("UpperChest", "上半身3");
        boneMap.put("Neck", "首");
        boneMap.put("Head", "頭");
        boneMap.put("LeftShoulder", "左肩");
        boneMap.put("LeftUpperArm", "左腕");
        boneMap.put("LeftLowerArm", "左ひじ");
        boneMap.put("LeftHand", "左手首");
        boneMap.put("RightShoulder", "右肩");
        boneMap.put("RightUpperArm", "右腕");
        boneMap.put("RightLowerArm", "右ひじ");
        boneMap.put("RightHand", "右手首");
        boneMap.put("LeftUpperLeg", "左足");
        boneMap.put("LeftLowerLeg", "左ひざ");
        boneMap.put("LeftFoot", "左足首");
        boneMap.put("RightUpperLeg", "右足");
        boneMap.put("RightLowerLeg", "右ひざ");
        boneMap.put("RightFoot", "右足首");
        VMC_TO_MMD_BONE_MAP = Collections.unmodifiableMap(boneMap);

        // VMC 블렌드셰이프 매핑 테이블 초기화
        Map<String, String> blendMap = new HashMap<>();
        blendMap.put("Joy", "にっこり");
        blendMap.put("Angry", "怒り");
        blendMap.put("Sorrow", "困る");
        blendMap.put("Fun", "楽しい");
        blendMap.put("A", "あ");
        blendMap.put("I", "い");
        blendMap.put("U", "う");
        blendMap.put("E", "え");
        blendMap.put("O", "お");
        blendMap.put("Blink", "まばたき");
        blendMap.put("Blink_L", "ウィンク");
        blendMap.put("Blink_R", "ウィンク２");
        VMC_TO_MMD_BLEND_MAP = Collections.unmodifiableMap(blendMap);
    }

    private static String mapVMCBoneToMMD(String vmcBoneName) {
        return VMC_TO_MMD_BONE_MAP.getOrDefault(vmcBoneName, vmcBoneName);
    }

    private static String mapVMCBlendToMMD(String vmcBlendName) {
        return VMC_TO_MMD_BLEND_MAP.getOrDefault(vmcBlendName, vmcBlendName);
    }

    // VMC 상태 표시 GUI (수정된 버전)
    public static void renderVMCStatus(PoseStack poseStack) {
        if (MCinstance.player != null && VMCConfig.isDebugMode()) {
            String playerName = MCinstance.player.getName().getString();
            MMDModelManager.Model model = MMDModelManager.GetModel("EntityPlayer_" + playerName);

            if (model instanceof MMDModelManager.ModelWithEntityData) {
                boolean vmcConnected = vmcReceiver != null && vmcReceiver.isConnected();
                boolean vmcEnabled = vmcDataManager != null && vmcDataManager.enableVMCOverride;

                // 상태 텍스트 생성
                String statusText = "VMC: ";
                if (vmcEnabled && vmcConnected) {
                    statusText += "활성";
                } else if (vmcConnected) {
                    statusText += "연결됨";
                } else {
                    statusText += "연결 안됨";
                }

                // 채팅으로 상태 표시 (폰트 렌더링 대신 안전한 방법)
                MCinstance.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(statusText),
                        true // 액션바에 표시
                );
            }
        }
    }

    // VMC 디버그 정보 출력
    public static void printVMCDebugInfo() {
        if (MCinstance.player != null) {
            logger.info("=== VMC Debug Info ===");
            logger.info("VMC Connected: " + (vmcReceiver != null ? vmcReceiver.isConnected() : false));
            logger.info("VMC Enabled: " + (vmcDataManager != null ? vmcDataManager.enableVMCOverride : false));
            if (vmcDataManager != null) {
                logger.info("Bone Count: " + vmcDataManager.getBoneDataList().size());
                logger.info("BlendShape Count: " + vmcDataManager.getBlendShapeDataList().size());
            }
        }
    }

    // VMC 설정 파일 관리
    public static class VMCConfig {
        private static final String CONFIG_FILE = gameDirectory + "/KAIMyEntity/vmc_config.properties";
        private static Properties properties = new Properties();

        public static void loadConfig() {
            File configFile = new File(CONFIG_FILE);

            try {
                // 설정 디렉토리가 없으면 생성
                File configDir = configFile.getParentFile();
                if (!configDir.exists()) {
                    boolean created = configDir.mkdirs();
                    if (created) {
                        logger.info("VMC 설정 디렉토리 생성: " + configDir.getAbsolutePath());
                    } else {
                        logger.warn("VMC 설정 디렉토리 생성 실패: " + configDir.getAbsolutePath());
                    }
                }

                // 설정 파일이 없으면 기본 설정 생성
                if (!configFile.exists()) {
                    logger.info("VMC 설정 파일이 없음, 기본 설정 생성 중...");
                    saveDefaultConfig();
                }

                // 설정 파일 로드
                try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                    properties.load(fis);
                    logger.info("✓ VMC 설정 파일 로드 성공: " + CONFIG_FILE);
                }

            } catch (IOException e) {
                logger.warn("VMC 설정 파일 로드 실패, 기본 설정으로 생성", e);
                saveDefaultConfig();
            }
        }

        public static void saveDefaultConfig() {
            properties.setProperty("vmc.port", "39540");
            properties.setProperty("vmc.autoStart", "true");
            properties.setProperty("vmc.debugMode", "false");
            properties.setProperty("vmc.smoothing", "0.8");

            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                properties.store(fos, "VMC Configuration");
            } catch (IOException e) {
                logger.warn("Failed to save VMC config", e);
            }
        }

        public static int getPort() {
            return Integer.parseInt(properties.getProperty("vmc.port", "39540"));
        }

        public static boolean isAutoStart() {
            return Boolean.parseBoolean(properties.getProperty("vmc.autoStart", "true"));
        }

        public static boolean isDebugMode() {
            return Boolean.parseBoolean(properties.getProperty("vmc.debugMode", "false"));
        }

        public static float getSmoothingFactor() {
            return Float.parseFloat(properties.getProperty("vmc.smoothing", "0.8"));
        }
    }

    // 기존 메서드들...
    private static String validateFilename(String filename, String intendedDir) throws java.io.IOException {
        File f = new File(filename);
        String canonicalPath = f.getCanonicalPath();

        File iD = new File(intendedDir);
        String canonicalID = iD.getCanonicalPath();

        if (canonicalPath.startsWith(canonicalID)) {
            return canonicalPath;
        } else {
            throw new IllegalStateException("File is outside extraction target directory.");
        }
    }

    public static final void unzip(String filename, String targetDir) throws java.io.IOException {
        FileInputStream fis = new FileInputStream(filename);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
        ZipEntry entry;
        int entries = 0;
        long total = 0;
        try {
            while ((entry = zis.getNextEntry()) != null) {
                logger.info("Extracting: " + entry);
                int count;
                byte data[] = new byte[BUFFER];
                String name = validateFilename(targetDir+entry.getName(), ".");
                File targetFile = new File(name);
                if (entry.isDirectory()) {
                    logger.info("Creating directory " + name);
                    new File(name).mkdir();
                    continue;
                }
                if (!targetFile.getParentFile().exists()){
                    targetFile.getParentFile().mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(name);
                BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
                while (total + BUFFER <= TOOBIG && (count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                    total += count;
                }
                dest.flush();
                dest.close();
                zis.closeEntry();
                entries++;
                if (entries > TOOMANY) {
                    throw new IllegalStateException("Too many files to unzip.");
                }
                if (total + BUFFER > TOOBIG) {
                    throw new IllegalStateException("File being unzipped is too big.");
                }
            }
        } finally {
            zis.close();
        }
    }



    private static void checkKAIMyEntityFolder(){
        File KAIMyEntityFolder = new File(gameDirectory + "/KAIMyEntity");
        if (!KAIMyEntityFolder.exists()){
            logger.info("KAIMyEntity folder not found, try download from github!");
            KAIMyEntityFolder.mkdir();
            try{
                FileUtils.copyURLToFile(new URI("https://github.com/Gengorou-C/KAIMyEntity-C/releases/download/requiredFiles/KAIMyEntity.zip").toURL(), new File(gameDirectory + "/KAIMyEntity.zip"), 30000, 30000);
            }catch (IOException e){
                logger.info("Download KAIMyEntity.zip failed! (IOException)");
            }catch(URISyntaxException e){
                logger.info("Download KAIMyEntity.zip failed! (URISyntaxException)");
            }

            try{
                unzip(gameDirectory + "/KAIMyEntity.zip", gameDirectory + "/KAIMyEntity/");
            }catch (IOException e){
                logger.info("extract KAIMyEntity.zip failed!");
            }
        }
        return;
    }

    public static String calledFrom(int i){
        StackTraceElement[] steArray = Thread.currentThread().getStackTrace();
        if (steArray.length <= i) {
            return "";
        }
        return steArray[i].getClassName();
    }

    public static Vector3f str2Vec3f(String arg){
        Vector3f vector3f = new Vector3f();
        String[] splittedStr = arg.split(",");
        if (splittedStr.length != 3){
            return new Vector3f(0.0f);
        }
        vector3f.x = Float.valueOf(splittedStr[0]);
        vector3f.y = Float.valueOf(splittedStr[1]);
        vector3f.z = Float.valueOf(splittedStr[2]);
        return vector3f;
    }

    public static void drawText(String arg, int x, int y){
        PoseStack mat = new PoseStack();
        mat.setIdentity();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        mat.mulPose(RenderSystem.getModelViewMatrix());
        mat.pushPose();
        mat.popPose();
    }
}