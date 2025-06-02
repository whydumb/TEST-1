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
    // VSeeFace 전용 머리 각도 설정 함수
    public native void SetHeadAngle(long model, float rotX, float rotY, float rotZ, boolean invertY);

    // 고급 표정 제어 함수들
    public native void SetEyeBlinkWeight(long model, float leftWeight, float rightWeight);
    public native void SetEyeLookDirection(long model, float leftX, float leftY, float rightX, float rightY);

    // 입술 동기화를 위한 비셈 함수
    public native void SetVisemeWeights(long model, float[] visemeWeights);

    // 모든 모프 이름 가져오기 (디버깅용)
    public native String[] GetAvailableMorphNames(long model);
    public native String[] GetAvailableBoneNames(long model);

    // VSeeFace 데이터 일괄 적용 (최적화된 버전)
    public native void ApplyVSeeFaceDataOptimized(long model,
                                                  float headRotX, float headRotY, float headRotZ,
                                                  float eyeBlinkLeft, float eyeBlinkRight,
                                                  float eyeLookLeftX, float eyeLookLeftY,
                                                  float eyeLookRightX, float eyeLookRightY,
                                                  float mouthA, float mouthI, float mouthU,
                                                  float mouthE, float mouthO);

    // 표정 보간 함수 (스무딩용)
    public native void SetMorphInterpolation(long model, boolean enableInterpolation, float interpolationSpeed);

    // 본 회전 보간 함수
    public native void SetBoneInterpolation(long model, boolean enableInterpolation, float interpolationSpeed);

    // 물리 시뮬레이션 일시 정지/재개
    public native void PausePhysics(long model, boolean pause);

    // 특정 본의 물리 시뮬레이션만 리셋
    public native void ResetBonePhysics(long model, String boneName);

    // 모델의 현재 상태 저장/복원 (포즈 저장용)
    public native long SaveModelPose(long model);
    public native void RestoreModelPose(long model, long savedPose);
    public native void DeleteSavedPose(long savedPose);
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

    public native void ResetModelPhysics(long model);

    public native long LoadTexture(String filename);

    public native void DeleteTexture(long tex);

    public native int GetTextureX(long tex);

    public native int GetTextureY(long tex);

    public native long GetTextureData(long tex);

    public native boolean TextureHasAlpha(long tex);

    public native void SetMorphWeight(long model, String morphName, float weight);
    public native float GetMorphWeight(long model, String morphName);
    public native void SetMorphWeights(long model, String[] morphNames, float[] weights);
    public native String[] GetAllMorphNames(long model);

    public native void SetBoneRotation(long model, String boneName, float x, float y, float z);
    public native void SetBonePosition(long model, String boneName, float x, float y, float z);


    // 블렌드 셰이프 일괄 적용
    public native void ApplyFacialExpression(long model,
                                             float eyeBlinkLeft, float eyeBlinkRight,
                                             float mouthOpen, float mouthSmile, float mouthSad,
                                             float browUp, float browDown);

    // VSeeFace 전용 빠른 적용 함수
    public native void ApplyVSeeFaceData(long model,
                                         float headRotX, float headRotY, float headRotZ,
                                         float eyeBlinkLeft, float eyeBlinkRight,
                                         float mouthA, float mouthI, float mouthU, float mouthE, float mouthO);


    enum runtimeUrlRes {
        windows,android_arch64, android_arch64_libc
    }
}
