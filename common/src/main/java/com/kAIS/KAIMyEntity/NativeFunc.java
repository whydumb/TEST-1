package com.kAIS.KAIMyEntity;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public native long GetBoneIndex(long modelPtr, String boneName);
public native void SetBonePosition(long modelPtr, long boneIndex, float x, float y, float z);
public native void SetBoneRotation(long modelPtr, long boneIndex, float x, float y, float z, float w);
public native void UpdateBones(long modelPtr);

// 모프(표정) 관련
public native long GetMorphIndex(long modelPtr, String morphName);
public native void SetMorphWeight(long modelPtr, long morphIndex, float weight);
public native void UpdateMorphs(long modelPtr);


public class NativeFunc {
    public static final Logger logger = LogManager.getLogger();
    private static final String RuntimePath = new File(System.getProperty("java.home")).getParent();
    private static final Minecraft MCinstance = Minecraft.getInstance();
    private static final String gameDirectory = MCinstance.gameDirectory.getAbsolutePath();
    private static final boolean isAndroid = new File("/system/build.prop").exists();
    private static final boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux");
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
    private static final HashMap<runtimeUrlRes, String> urlMap = new HashMap<runtimeUrlRes, String>() {
        {
            put(runtimeUrlRes.windows, "https://github.com/Gengorou-C/KAIMyEntitySaba/releases/download/20240314/KAIMyEntitySaba.dll");
            put(runtimeUrlRes.android_arch64, "https://github.com.cnpmjs.org/asuka-mio/KAIMyEntitySaba/releases/download/crossplatform/KAIMyEntitySaba.so");
            put(runtimeUrlRes.android_arch64_libc, "https://github.com.cnpmjs.org/asuka-mio/KAIMyEntitySaba/releases/download/crossplatform/libc++_shared.so");
        }
    };
    static NativeFunc inst;
    static final String libraryVersion = "C-20240314";

    public static NativeFunc GetInst() {
        if (inst == null) {
            inst = new NativeFunc();
            inst.Init();
            if(!inst.GetVersion().equals(libraryVersion)){
                logger.warn("Incompatible Version dll. / loaded ver -> " + inst.GetVersion() + " / required ver -> "+ libraryVersion);
                logger.warn("Please restart or download dll.");
                try{
                    Files.move(Paths.get(gameDirectory, "KAIMyEntitySaba.dll"),Paths.get(gameDirectory, "KAIMyEntitySaba.dll.old"));
                }catch(Exception e){
                    logger.info(e);
                }
            }
        }
        return inst;
    }

    private void DownloadSingleFile(URL url, File file) throws IOException {
        if (file.exists()) {
            try {
                System.load(file.getAbsolutePath());
                return; //File exist and loadable
            } catch (Error e) {
                logger.info("\"" + file.getAbsolutePath() + "\" broken! Trying recover it!");
            }
        }
        try {
            file.delete();
            file.createNewFile();
            FileUtils.copyURLToFile(url, file, 30000, 30000);
            System.load(file.getAbsolutePath());
        } catch (IOException e) {
            file.delete();
            logger.info("Download \"" + url.getPath() + "\" failed!");
            logger.info("Cannot download runtime!");
            logger.info("Check you internet connection and restart game!");
            e.printStackTrace();
            throw e;
        }
    }

    private void DownloadRuntime() throws Exception {
        if (isWindows) {
            DownloadSingleFile(new URI(urlMap.get(runtimeUrlRes.windows)).toURL(), new File(gameDirectory, "KAIMyEntitySaba.dll"));
        }
        if (isLinux && !isAndroid) {
            logger.info("Not support!");
            throw new Error();
        }
        if (isLinux && isAndroid) {
            DownloadSingleFile(new URI(urlMap.get(runtimeUrlRes.android_arch64_libc)).toURL(), new File(RuntimePath, "libc++_shared.so"));
            DownloadSingleFile(new URI(urlMap.get(runtimeUrlRes.android_arch64)).toURL(), new File(RuntimePath, "KAIMyEntitySaba.so"));
        }
    }

    private void LoadLibrary(File file) {
        try {
            System.load(file.getAbsolutePath());
        } catch (Error e) {
            logger.info("Runtime \"" + file.getAbsolutePath() + "\" not found, try download from github!");
            throw e;
        }
    }

    private void Init() {
        try {
            if (isWindows) {
                logger.info("Win32 Env Detected!");
                LoadLibrary(new File(gameDirectory, "KAIMyEntitySaba.dll"));//WIN32
            }
            if (isLinux && !isAndroid) {
                logger.info("Linux Env Detected!");
                LoadLibrary(new File(gameDirectory, "KAIMyEntitySaba.so"));//Linux
            }
            if (isLinux && isAndroid) {
                logger.info("Android Env Detected!");
                LoadLibrary(new File(RuntimePath, "libc++_shared.so"));
                LoadLibrary(new File(RuntimePath, "KAIMyEntitySaba.so"));//Android
            }
        } catch (Error e) {
            try {
                DownloadRuntime();
            } catch (Exception ex) {
                throw e;
            }
        }
    }

    public native String GetVersion();

    public native byte ReadByte(long data, long pos);

    public native void CopyDataToByteBuffer(ByteBuffer buffer, long data, long pos);

    public native long LoadModelPMX(String filename, String dir, long layerCount);

    public native long LoadModelPMD(String filename, String dir, long layerCount);

    public native void DeleteModel(long model);

    public native void UpdateModel(long model);

    public native long GetVertexCount(long model);

    public native long GetPoss(long model);

    public native long GetNormals(long model);

    public native long GetUVs(long model);

    public native long GetIndexElementSize(long model);

    public native long GetIndexCount(long model);

    public native long GetIndices(long model);

    public native long GetMaterialCount(long model);

    public native String GetMaterialTex(long model, long pos);

    public native String GetMaterialSpTex(long model, long pos);

    public native String GetMaterialToonTex(long model, long pos);

    public native long GetMaterialAmbient(long model, long pos);

    public native long GetMaterialDiffuse(long model, long pos);

    public native long GetMaterialSpecular(long model, long pos);

    public native float GetMaterialSpecularPower(long model, long pos);

    public native float GetMaterialAlpha(long model, long pos);

    public native long GetMaterialTextureMulFactor(long model, long pos);

    public native long GetMaterialTextureAddFactor(long model, long pos);

    public native int GetMaterialSpTextureMode(long model, long pos);

    public native long GetMaterialSpTextureMulFactor(long model, long pos);

    public native long GetMaterialSpTextureAddFactor(long model, long pos);

    public native long GetMaterialToonTextureMulFactor(long model, long pos);

    public native long GetMaterialToonTextureAddFactor(long model, long pos);

    public native boolean GetMaterialBothFace(long model, long pos);

    public native long GetSubMeshCount(long model);

    public native int GetSubMeshMaterialID(long model, long pos);

    public native int GetSubMeshBeginIndex(long model, long pos);

    public native int GetSubMeshVertexCount(long model, long pos);

    public native void ChangeModelAnim(long model, long anim, long layer);

    public native void ResetModelPhysics(long model);

    public native long CreateMat();

    public native void DeleteMat(long mat);

    public native void GetRightHandMat(long model, long mat);

    public native void GetLeftHandMat(long model, long mat);

    public native long LoadTexture(String filename);

    public native void DeleteTexture(long tex);

    public native int GetTextureX(long tex);

    public native int GetTextureY(long tex);

    public native long GetTextureData(long tex);

    public native boolean TextureHasAlpha(long tex);

    public native long LoadAnimation(long model, String filename);

    public native void DeleteAnimation(long anim);

    public native void SetHeadAngle(long model, float x, float y, float z, boolean flag);

	public native void SetBoneTransform(long model, String boneName, 
                                       float px, float py, float pz, 
                                       float rx, float ry, float rz, float rw);
    
    public native void SetBlendShapeValue(long model, String blendShapeName, float value);
    
    public native void EnableVMCMode(long model, boolean enabled);
    
    public native boolean IsBoneExists(long model, String boneName);
    
    public native String[] GetBoneNames(long model);
    
    public native String[] GetBlendShapeNames(long model);

    enum runtimeUrlRes {
        windows,android_arch64, android_arch64_libc
    }
}

private void setVMCBoneTransform(NativeFunc nf, long modelPtr, String boneName, VMCBoneData boneData) {
    try {
        // 본이 존재하는지 확인
        if (nf.IsBoneExists(modelPtr, boneName)) {
            Vector3f mcPos = boneData.getMinecraftPosition();
            Quaternionf mcRot = boneData.getMinecraftRotation();
            
            // 네이티브 함수로 본 변환 적용
            nf.SetBoneTransform(modelPtr, boneName, 
                               mcPos.x, mcPos.y, mcPos.z,
                               mcRot.x, mcRot.y, mcRot.z, mcRot.w);
        }
    } catch (Exception e) {
        logger.warn("Failed to set VMC bone transform for " + boneName, e);
    }
}

private void setVMCBlendShape(NativeFunc nf, long modelPtr, String blendShapeName, float value) {
    try {
        // BlendShape 값 설정 (0.0 ~ 1.0 범위로 제한)
        float clampedValue = Math.max(0.0f, Math.min(1.0f, value));
        nf.SetBlendShapeValue(modelPtr, blendShapeName, clampedValue);
    } catch (Exception e) {
        logger.warn("Failed to set VMC blend shape " + blendShapeName, e);
    }
}

// VMC 상태 표시 GUI 추가
public static void renderVMCStatus(PoseStack poseStack) {
    Minecraft MCinstance = Minecraft.getInstance();
    if (MCinstance.player != null) {
        String playerName = MCinstance.player.getName().getString();
        MMDModelManager.Model model = MMDModelManager.GetModel("EntityPlayer_" + playerName);
        
        if (model instanceof MMDModelManager.ModelWithEntityData mwed) {
            boolean vmcConnected = mwed.vmcData.isVMCConnected();
            boolean vmcEnabled = mwed.vmcData.enableVMCOverride;
            
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

private String mapVMCBoneToMMD(String vmcBoneName) {
    return VMC_TO_MMD_BONE_MAP.getOrDefault(vmcBoneName, vmcBoneName);
}

private String mapVMCBlendToMMD(String vmcBlendName) {
    return VMC_TO_MMD_BLEND_MAP.getOrDefault(vmcBlendName, vmcBlendName);
}

// 개선된 VMC 데이터 적용 함수
private void applyVMCDataToModel(MMDModelManager.ModelWithEntityData mwed) {
    NativeFunc nf = NativeFunc.GetInst();
    long modelPtr = mwed.model.GetModelLong();
    
    // VMC 모드 활성화
    nf.EnableVMCMode(modelPtr, true);
    
    // 본 데이터 적용
    for (Map.Entry<String, MMDModelManager.VMCBoneData> entry : mwed.vmcData.vmcBones.entrySet()) {
        String vmcBoneName = entry.getKey();
        String mmdBoneName = mapVMCBoneToMMD(vmcBoneName);
        MMDModelManager.VMCBoneData boneData = entry.getValue();
        
        // 데이터가 너무 오래된 경우 스킵 (1초 이상)
        if (System.currentTimeMillis() - boneData.timestamp > 1000) {
            continue;
        }
        
        setVMCBoneTransform(nf, modelPtr, mmdBoneName, boneData);
    }
    
    // BlendShape 데이터 적용
    for (Map.Entry<String, Float> entry : mwed.vmcData.vmcBlendShapes.entrySet()) {
        String vmcBlendName = entry.getKey();
        String mmdBlendName = mapVMCBlendToMMD(vmcBlendName);
        float value = entry.getValue();
        
        setVMCBlendShape(nf, modelPtr, mmdBlendName, value);
    }
}

// VMC 디버그 정보 출력
public static void printVMCDebugInfo() {
    Minecraft MCinstance = Minecraft.getInstance();
    if (MCinstance.player != null) {
        String playerName = MCinstance.player.getName().getString();
        MMDModelManager.Model model = MMDModelManager.GetModel("EntityPlayer_" + playerName);
        
        if (model instanceof MMDModelManager.ModelWithEntityData mwed) {
            logger.info("=== VMC Debug Info ===");
            logger.info("VMC Connected: " + mwed.vmcData.isVMCConnected());
            logger.info("VMC Enabled: " + mwed.vmcData.enableVMCOverride);
            logger.info("Bone Count: " + mwed.vmcData.vmcBones.size());
            logger.info("BlendShape Count: " + mwed.vmcData.vmcBlendShapes.size());
            logger.info("Last Update: " + (System.currentTimeMillis() - mwed.vmcData.lastUpdateTime) + "ms ago");
            
            // 본 정보 출력
            for (String boneName : mwed.vmcData.vmcBones.keySet()) {
                MMDModelManager.VMCBoneData boneData = mwed.vmcData.vmcBones.get(boneName);
                logger.info("Bone " + boneName + ": pos=" + boneData.position + ", rot=" + boneData.rotation);
            }
            
            // BlendShape 정보 출력
            for (Map.Entry<String, Float> entry : mwed.vmcData.vmcBlendShapes.entrySet()) {
                logger.info("BlendShape " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}

// VMC 설정 파일 관리
public static class VMCConfig {
    private static final String CONFIG_FILE = gameDirectory + "/KAIMyEntity/vmc_config.properties";
    private static Properties properties = new Properties();
    
    static {
        loadConfig();
    }
    
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