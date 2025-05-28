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
import java.util.Properties;
import java.io.FileOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
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
        vmcDataManager = VMCDataManager.getInstance();
        vmcReceiver = new VMCReceiver();

        // VMC 설정 로드
        VMCConfig.loadConfig();

        // 자동 시작이 설정되어 있으면 VMC 수신 시작
        if (VMCConfig.isAutoStart()) {
            startVMC();
        }

        logger.info("VMC system initialized");
    }

    /**
     * VMC 토글 (키바인딩용)
     */
    public static void toggleVMC() {
        if (vmcReceiver.isConnected()) {
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
    public static void startVMC() {
        if (vmcReceiver != null) {
            vmcReceiver.startReceiving(VMCConfig.getPort());
        }
    }

    /**
     * VMC 수신 중지
     */
    public static void stopVMC() {
        if (vmcReceiver != null) {
            vmcReceiver.stopReceiving();
        }
        vmcDataManager.enableVMCOverride = false;
    }

    // VMC 본 매핑 테이블 (VMC 본 이름 → MMD 본 이름)
    private static final Map<String, String> VMC_TO_MMD_BONE_MAP = Map.of(
            "Hips", "下半身",
            "Spine", "上半身",
            "Chest", "上半身2",
            "UpperChest", "上半身3",
            "Neck", "首",
            "Head", "頭",
            "LeftShoulder", "左肩",
            "LeftUpperArm", "左腕",
            "LeftLowerArm", "左ひじ",
            "LeftHand", "左手首",
            "RightShoulder", "右肩",
            "RightUpperArm", "右腕",
            "RightLowerArm", "右ひじ",
            "RightHand", "右手首",
            "LeftUpperLeg", "左足",
            "LeftLowerLeg", "左ひざ",
            "LeftFoot", "左足首",
            "RightUpperLeg", "右足",
            "RightLowerLeg", "右ひざ",
            "RightFoot", "右足首"
    );

    // VMC BlendShape 매핑 테이블 (VMC → MMD)
    private static final Map<String, String> VMC_TO_MMD_BLEND_MAP = Map.of(
            "Joy", "にっこり",
            "Angry", "怒り",
            "Sorrow", "困る",
            "Fun", "楽しい",
            "A", "あ",
            "I", "い",
            "U", "う",
            "E", "え",
            "O", "お",
            "Blink", "まばたき",
            "Blink_L", "ウィンク",
            "Blink_R", "ウィンク２"
    );

    private static String mapVMCBoneToMMD(String vmcBoneName) {
        return VMC_TO_MMD_BONE_MAP.getOrDefault(vmcBoneName, vmcBoneName);
    }

    private static String mapVMCBlendToMMD(String vmcBlendName) {
        return VMC_TO_MMD_BLEND_MAP.getOrDefault(vmcBlendName, vmcBlendName);
    }

    // VMC 상태 표시 GUI
    public static void renderVMCStatus(PoseStack poseStack) {
        if (MCinstance.player != null && VMCConfig.isDebugMode()) {
            String playerName = MCinstance.player.getName().getString();
            MMDModelManager.Model model = MMDModelManager.GetModel("EntityPlayer_" + playerName);

            if (model instanceof MMDModelManager.ModelWithEntityData) {
                boolean vmcConnected = vmcReceiver.isConnected();
                boolean vmcEnabled = vmcDataManager.enableVMCOverride;

                // 화면 우상단에 VMC 상태 표시
                String statusText = "VMC: ";
                int color;

                if (vmcEnabled && vmcConnected) {
                    statusText += "활성";
                    color = 0x00FF00; // 녹색
                } else if (vmcConnected) {
                    statusText += "연결됨";
                    color = 0xFFFF00; // 노란색
                } else {
                    statusText += "연결 안됨";
                    color = 0xFF0000; // 빨간색
                }

                int screenWidth = MCinstance.getWindow().getGuiScaledWidth();
                MCinstance.font.draw(poseStack, statusText, screenWidth - 100, 10, color);
            }
        }
    }

    // VMC 디버그 정보 출력
    public static void printVMCDebugInfo() {
        if (MCinstance.player != null) {
            logger.info("=== VMC Debug Info ===");
            logger.info("VMC Connected: " + vmcReceiver.isConnected());
            logger.info("VMC Enabled: " + vmcDataManager.enableVMCOverride);
            logger.info("Bone Count: " + vmcDataManager.getBoneDataList().size());
            logger.info("BlendShape Count: " + vmcDataManager.getBlendShapeDataList().size());
        }
    }

    // VMC 설정 파일 관리
    public static class VMCConfig {
        private static final String CONFIG_FILE = gameDirectory + "/KAIMyEntity/vmc_config.properties";
        private static Properties properties = new Properties();

        public static void loadConfig() {
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                properties.load(fis);
            } catch (IOException e) {
                // 기본 설정으로 생성
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