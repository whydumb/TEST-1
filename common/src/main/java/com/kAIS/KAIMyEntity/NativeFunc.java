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
                return;
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
                LoadLibrary(new File(gameDirectory, "KAIMyEntitySaba.dll"));
            }
            if (isLinux && !isAndroid) {
                logger.info("Linux Env Detected!");
                LoadLibrary(new File(gameDirectory, "KAIMyEntitySaba.so"));
            }
            if (isLinux && isAndroid) {
                logger.info("Android Env Detected!");
                LoadLibrary(new File(RuntimePath, "libc++_shared.so"));
                LoadLibrary(new File(RuntimePath, "KAIMyEntitySaba.so"));
            }
        } catch (Error e) {
            try {
                DownloadRuntime();
            } catch (Exception ex) {
                throw e;
            }
        }
    }

    // 기본 네이티브 함수들
    public native String GetVersion();
    public native byte ReadByte(long data, long pos);
    public native void CopyDataToByteBuffer(ByteBuffer buffer, long data, long pos);
    public native long LoadModelPMX(String filename, String dir, long layerCount);
    public native long LoadModelPMD(String filename, String dir, long layerCount);
    public native void DeleteModel(long model);
    public native void UpdateModel(long model);

    // 버텍스 및 지오메트리 관련
    public native long GetVertexCount(long model);
    public native long GetPoss(long model);
    public native long GetNormals(long model);
    public native long GetUVs(long model);
    public native long GetIndexElementSize(long model);
    public native long GetIndexCount(long model);
    public native long GetIndices(long model);

    // 머티리얼 관련
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

    // 서브메시 관련
    public native long GetSubMeshCount(long model);
    public native int GetSubMeshMaterialID(long model, long pos);
    public native int GetSubMeshBeginIndex(long model, long pos);
    public native int GetSubMeshVertexCount(long model, long pos);

    // 애니메이션 관련
    public native void ChangeModelAnim(long model, long anim, long layer);
    public native void ResetModelPhysics(long model);
    public native long LoadAnimation(long model, String filename);
    public native void DeleteAnimation(long anim);

    // 매트릭스 관련
    public native long CreateMat();
    public native void DeleteMat(long mat);
    public native void GetRightHandMat(long model, long mat);
    public native void GetLeftHandMat(long model, long mat);

    // 텍스처 관련
    public native long LoadTexture(String filename);
    public native void DeleteTexture(long tex);
    public native int GetTextureX(long tex);
    public native int GetTextureY(long tex);
    public native long GetTextureData(long tex);
    public native boolean TextureHasAlpha(long tex);

    // 헤드 앵글 제어
    public native void SetHeadAngle(long model, float x, float y, float z, boolean flag);

    // VMC 관련 네이티브 함수들
    public native long GetBoneIndex(long modelPtr, String boneName);
    public native void SetBonePosition(long modelPtr, long boneIndex, float x, float y, float z);
    public native void SetBoneRotation(long modelPtr, long boneIndex, float x, float y, float z, float w);
    public native void UpdateBones(long modelPtr);

    // 모프(표정) 관련
    public native long GetMorphIndex(long modelPtr, String morphName);
    public native void SetMorphWeight(long modelPtr, long morphIndex, float weight);
    public native void UpdateMorphs(long modelPtr);

    // VMC 통합 함수들
    public native void SetBoneTransform(long model, String boneName,
                                        float px, float py, float pz,
                                        float rx, float ry, float rz, float rw);

    public native void SetBlendShapeValue(long model, String blendShapeName, float value);

    public native void EnableVMCMode(long model, boolean enabled);

    public native boolean IsBoneExists(long model, String boneName);

    public native String[] GetBoneNames(long model);

    public native String[] GetBlendShapeNames(long model);

    enum runtimeUrlRes {
        windows, android_arch64, android_arch64_libc
    }
}