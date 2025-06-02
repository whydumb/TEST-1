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

    // 초기화 상태 추적 변수들
    private static boolean initializationAttempted = false;
    private static boolean initializationSuccessful = false;
    private static String initializationError = null;
    private static long initializationTime = 0;
    private static String loadedLibraryPath = null;

    public static NativeFunc GetInst() {
        if (!initializationAttempted) {
            attemptInitialization();
        }

        if (!initializationSuccessful) {
            String errorMsg = "Native library not available";
            if (initializationError != null) {
                errorMsg += ": " + initializationError;
            }
            throw new RuntimeException(errorMsg);
        }

        return inst;
    }

    private static synchronized void attemptInitialization() {
        if (initializationAttempted) {
            return; // 이미 시도함
        }

        initializationAttempted = true;
        long startTime = System.currentTimeMillis();

        try {
            logger.info("=== Native Library Initialization Starting ===");
            logSystemInfo();

            inst = new NativeFunc();
            inst.Init();

            // 초기화 검증
            verifyInitialization();

            initializationTime = System.currentTimeMillis() - startTime;
            initializationSuccessful = true;

            logger.info("=== Native Library Initialization Successful ===");
            logger.info("Initialization completed in " + initializationTime + "ms");

        } catch (Throwable e) {  // Exception → Throwable로 변경
            initializationError = e.getMessage();
            initializationTime = System.currentTimeMillis() - startTime;

            logger.error("=== Native Library Initialization Failed ===", e);
            logger.error("Initialization failed after " + initializationTime + "ms");

            // 상세한 오류 분석 및 해결책 제공
            analyzeAndSuggestSolutions(e);

            inst = null;
        }
    }

    private static void logSystemInfo() {
        logger.info("System Information:");
        logger.info("  OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        logger.info("  Architecture: " + System.getProperty("os.arch"));
        logger.info("  Java Version: " + System.getProperty("java.version"));
        logger.info("  Java Vendor: " + System.getProperty("java.vendor"));
        logger.info("  Java Home: " + System.getProperty("java.home"));
        logger.info("  Game Directory: " + gameDirectory);
        logger.info("  Runtime Path: " + RuntimePath);
        logger.info("  Android Detected: " + isAndroid);
        logger.info("  Linux Detected: " + isLinux);
        logger.info("  Windows Detected: " + isWindows);
    }

    private static void verifyInitialization() throws Exception {
        if (inst == null) {
            throw new RuntimeException("Native instance is null after initialization");
        }

        try {
            // 기본 기능 테스트
            String version = inst.GetVersion();
            logger.info("Native library version: " + version);

            // 버전 호환성 검사
            if (!version.equals(libraryVersion)) {
                logger.warn("Version mismatch detected!");
                logger.warn("  Expected: " + libraryVersion);
                logger.warn("  Actual: " + version);
                logger.warn("This may cause compatibility issues. Consider updating the library.");

                // 버전 불일치 시 기존 파일 백업
                try {
                    String backupName = "KAIMyEntitySaba_" + version.replace("-", "_") + "_backup.dll";
                    Files.move(
                            Paths.get(gameDirectory, "KAIMyEntitySaba.dll"),
                            Paths.get(gameDirectory, backupName)
                    );
                    logger.info("Old version backed up as: " + backupName);
                } catch (Exception e) {
                    logger.debug("Failed to backup old version", e);
                }
            }

            logger.info("Native library verification successful");

        } catch (Exception e) {
            logger.error("Native library verification failed", e);
            throw new RuntimeException("Library loaded but basic functionality test failed", e);
        }
    }

    private static void analyzeAndSuggestSolutions(Throwable e) {  // Exception → Throwable로 변경
        logger.error("=== Error Analysis and Solutions ===");

        if (e instanceof UnsatisfiedLinkError) {
            UnsatisfiedLinkError linkError = (UnsatisfiedLinkError) e;
            String message = linkError.getMessage();

            logger.error("UnsatisfiedLinkError detected: " + message);
            logger.error("");
            logger.error("Common causes and solutions:");

            if (message.contains("can't find dependent libraries") ||
                    message.contains("The specified procedure could not be found")) {
                logger.error("1. MISSING VISUAL C++ RUNTIME:");
                logger.error("   - Download and install 'Microsoft Visual C++ Redistributable'");
                logger.error("   - Required version: Visual Studio 2019 or newer (x64)");
                logger.error("   - Download from: https://aka.ms/vs/17/release/vc_redist.x64.exe");

            } else if (message.contains("wrong ELF class") || message.contains("architecture")) {
                logger.error("2. ARCHITECTURE MISMATCH:");
                logger.error("   - Your Java is " + System.getProperty("os.arch"));
                logger.error("   - Make sure the DLL matches your Java architecture (32-bit vs 64-bit)");

            } else if (message.contains("Access is denied") || message.contains("permission")) {
                logger.error("3. PERMISSION ISSUE:");
                logger.error("   - Run Minecraft as administrator");
                logger.error("   - Check if antivirus is blocking the DLL");
                logger.error("   - Ensure the game directory is writable");

            } else {
                logger.error("4. GENERAL SOLUTIONS:");
                logger.error("   - Delete KAIMyEntitySaba.dll and restart (auto-download)");
                logger.error("   - Check if the DLL file is corrupted (size should be > 1MB)");
                logger.error("   - Temporarily disable antivirus and try again");
            }

        } else if (e instanceof SecurityException) {
            logger.error("SECURITY EXCEPTION:");
            logger.error("   - Java security policy is blocking native library loading");
            logger.error("   - Check Java security settings");
            logger.error("   - Try running with -Djava.security.policy=all.policy");

        } else {
            logger.error("UNKNOWN ERROR TYPE:");
            logger.error("   - Check the full error message above");
            logger.error("   - Consider reporting this issue to the mod developers");
        }

        logger.error("=== End of Error Analysis ===");
    }

    // 초기화 관련 정보 제공 메서드들
    public static boolean isInitialized() {
        return initializationSuccessful;
    }

    public static boolean hasInitializationFailed() {
        return initializationAttempted && !initializationSuccessful;
    }

    public static String getInitializationError() {
        return initializationError;
    }

    public static long getInitializationTime() {
        return initializationTime;
    }

    public static String getLoadedLibraryPath() {
        return loadedLibraryPath;
    }

    // 강제 재초기화 (디버깅용)
    public static synchronized void forceReinitialization() {
        logger.info("Forcing reinitialization of native library...");

        initializationAttempted = false;
        initializationSuccessful = false;
        initializationError = null;
        initializationTime = 0;
        inst = null;

        attemptInitialization();
    }

    private void DownloadSingleFile(URL url, File file) throws IOException {
        logger.info("Downloading: " + url + " -> " + file.getAbsolutePath());

        if (file.exists()) {
            try {
                logger.info("File exists, testing if it's loadable...");
                System.load(file.getAbsolutePath());
                loadedLibraryPath = file.getAbsolutePath();
                logger.info("Existing file is loadable, skipping download");
                return; // File exist and loadable
            } catch (Error e) {
                logger.warn("Existing file is corrupted or incompatible, re-downloading...");
                logger.debug("Load error details", e);
            }
        }

        try {
            // 기존 파일 정리
            if (file.exists()) {
                boolean deleted = file.delete();
                logger.info("Old file deletion: " + (deleted ? "success" : "failed"));
            }

            // 새 파일 생성
            boolean created = file.createNewFile();
            logger.info("New file creation: " + (created ? "success" : "failed"));

            // 다운로드 실행
            logger.info("Starting download from: " + url);
            FileUtils.copyURLToFile(url, file, 30000, 30000);

            // 다운로드 검증
            if (!file.exists()) {
                throw new IOException("Downloaded file does not exist");
            }

            long fileSize = file.length();
            logger.info("Download completed, file size: " + fileSize + " bytes");

            if (fileSize == 0) {
                throw new IOException("Downloaded file is empty");
            }

            if (fileSize < 1024) {
                throw new IOException("Downloaded file is too small (likely incomplete): " + fileSize + " bytes");
            }

            // 로드 테스트
            System.load(file.getAbsolutePath());
            loadedLibraryPath = file.getAbsolutePath();
            logger.info("Downloaded file loaded successfully");

        } catch (IOException e) {
            // 다운로드 실패 시 정리
            if (file.exists()) {
                file.delete();
            }

            logger.error("Download failed for: " + url);
            logger.error("Target file: " + file.getAbsolutePath());
            logger.error("Error details: " + e.getMessage());
            logger.error("");
            logger.error("Possible solutions:");
            logger.error("1. Check your internet connection");
            logger.error("2. Check if the download URL is accessible");
            logger.error("3. Try downloading the file manually and placing it in the game directory");
            logger.error("4. Check if firewall/antivirus is blocking the download");

            throw new IOException("Failed to download native library: " + e.getMessage(), e);
        }
    }

    private void DownloadRuntime() throws Exception {
        logger.info("Downloading platform-specific runtime libraries...");

        try {
            if (isWindows) {
                logger.info("Downloading Windows runtime...");
                DownloadSingleFile(
                        new URI(urlMap.get(runtimeUrlRes.windows)).toURL(),
                        new File(gameDirectory, "KAIMyEntitySaba.dll")
                );
            } else if (isLinux && !isAndroid) {
                logger.error("Linux platform is not currently supported!");
                logger.error("Only Windows and Android are supported at this time");
                throw new UnsupportedOperationException("Linux platform not supported");
            } else if (isLinux && isAndroid) {
                logger.info("Downloading Android runtime...");
                DownloadSingleFile(
                        new URI(urlMap.get(runtimeUrlRes.android_arch64_libc)).toURL(),
                        new File(RuntimePath, "libc++_shared.so")
                );
                DownloadSingleFile(
                        new URI(urlMap.get(runtimeUrlRes.android_arch64)).toURL(),
                        new File(RuntimePath, "KAIMyEntitySaba.so")
                );
            } else {
                throw new UnsupportedOperationException("Unsupported platform: " + System.getProperty("os.name"));
            }

            logger.info("Runtime download completed successfully");

        } catch (Exception e) {
            logger.error("Runtime download failed", e);
            throw new Exception("Failed to download required runtime libraries", e);
        }
    }

    private void LoadLibrary(File file) {
        logger.info("Loading native library: " + file.getAbsolutePath());

        try {
            if (!file.exists()) {
                throw new UnsatisfiedLinkError("Library file does not exist: " + file.getAbsolutePath());
            }

            if (!file.canRead()) {
                throw new UnsatisfiedLinkError("Library file is not readable: " + file.getAbsolutePath());
            }

            long fileSize = file.length();
            logger.info("Library file size: " + fileSize + " bytes");

            if (fileSize == 0) {
                throw new UnsatisfiedLinkError("Library file is empty: " + file.getAbsolutePath());
            }

            System.load(file.getAbsolutePath());
            loadedLibraryPath = file.getAbsolutePath();
            logger.info("Library loaded successfully");

        } catch (UnsatisfiedLinkError e) {
            logger.error("Failed to load library: " + file.getAbsolutePath());
            logger.error("Error: " + e.getMessage());
            logger.info("Will attempt to download a fresh copy...");
            throw e;
        }
    }

    private void Init() {
        logger.info("Initializing native function system...");

        try {
            if (isWindows) {
                logger.info("Windows environment detected");
                LoadLibrary(new File(gameDirectory, "KAIMyEntitySaba.dll"));

            } else if (isLinux && !isAndroid) {
                logger.info("Linux environment detected");
                LoadLibrary(new File(gameDirectory, "KAIMyEntitySaba.so"));

            } else if (isLinux && isAndroid) {
                logger.info("Android environment detected");
                LoadLibrary(new File(RuntimePath, "libc++_shared.so"));
                LoadLibrary(new File(RuntimePath, "KAIMyEntitySaba.so"));

            } else {
                throw new UnsupportedOperationException("Unsupported platform");
            }

            logger.info("Native function system initialized successfully");

        } catch (Error e) {
            logger.warn("Initial library loading failed, attempting download...");

            try {
                DownloadRuntime();
                logger.info("Download completed, retrying initialization...");

                // 다운로드 후 재시도
                Init();

            } catch (Exception downloadException) {
                logger.error("Download and retry failed", downloadException);
                throw new RuntimeException("Failed to initialize native library after download attempt", e);
            }
        } catch (Exception e) {  // Exception catch 블록 추가
            logger.error("Unexpected error during initialization", e);
            throw new RuntimeException("Unexpected initialization error", e);
        }
    }

    enum runtimeUrlRes {
        windows, android_arch64, android_arch64_libc
    }

    // ========== 모든 기존 Native 메서드들 ==========

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

    // 기본 시스템 함수들
    public native String GetVersion();
    public native byte ReadByte(long data, long pos);
    public native void CopyDataToByteBuffer(ByteBuffer buffer, long data, long pos);

    // 모델 관리 함수들
    public native long LoadModelPMX(String filename, String dir, long layerCount);
    public native long LoadModelPMD(String filename, String dir, long layerCount);
    public native void DeleteModel(long model);
    public native void UpdateModel(long model);

    // 버텍스 및 지오메트리 함수들
    public native long GetVertexCount(long model);
    public native long GetPoss(long model);
    public native long GetNormals(long model);
    public native long GetUVs(long model);
    public native long GetIndexElementSize(long model);
    public native long GetIndexCount(long model);
    public native long GetIndices(long model);

    // 머티리얼 관리 함수들
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

    // 서브메시 관리 함수들
    public native long GetSubMeshCount(long model);
    public native int GetSubMeshMaterialID(long model, long pos);
    public native int GetSubMeshBeginIndex(long model, long pos);
    public native int GetSubMeshVertexCount(long model, long pos);

    // 물리 시뮬레이션 함수들
    public native void ResetModelPhysics(long model);

    // 텍스처 관리 함수들
    public native long LoadTexture(String filename);
    public native void DeleteTexture(long tex);
    public native int GetTextureX(long tex);
    public native int GetTextureY(long tex);
    public native long GetTextureData(long tex);
    public native boolean TextureHasAlpha(long tex);

    // 모프 (표정) 제어 함수들
    public native void SetMorphWeight(long model, String morphName, float weight);
    public native float GetMorphWeight(long model, String morphName);
    public native void SetMorphWeights(long model, String[] morphNames, float[] weights);
    public native String[] GetAllMorphNames(long model);

    // 본 제어 함수들
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
}