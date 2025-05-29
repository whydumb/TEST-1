
package com.kAIS.KAIMyEntity.renderer;

import com.kAIS.KAIMyEntity.KAIMyEntityClient;
import com.kAIS.KAIMyEntity.NativeFunc;
import com.kAIS.KAIMyEntity.vmc.VMCDataManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;
import org.joml.Quaternionf;

import net.minecraft.client.Minecraft;

public class MMDModelManager {
    static final Logger logger = LogManager.getLogger();
    static final Minecraft MCinstance = Minecraft.getInstance();
    static Map<String, Model> models;
    static String gameDirectory = MCinstance.gameDirectory.getAbsolutePath();

    public static void Init() {
        models = new HashMap<>();
        logger.info("MMDModelManager.Init() finished");
    }

    public static IMMDModel LoadModel(String modelName, long layerCount) {
        //Model path
        File modelDir = new File(gameDirectory + "/KAIMyEntity/" + modelName);
        String modelDirStr = modelDir.getAbsolutePath();

        String modelFilenameStr;
        boolean isPMD;
        File pmxModelFilename = new File(modelDir, "model.pmx");
        if (pmxModelFilename.isFile()) {
            modelFilenameStr = pmxModelFilename.getAbsolutePath();
            isPMD = false;
        } else {
            File pmdModelFilename = new File(modelDir, "model.pmd");
            if (pmdModelFilename.isFile()) {
                modelFilenameStr = pmdModelFilename.getAbsolutePath();
                isPMD = true;
            } else {
                return null;
            }
        }
        return MMDModelOpenGL.Create(modelFilenameStr, modelDirStr, isPMD, layerCount);
    }

    public static Model GetModel(String modelName, String uuid) {
        Model model = models.get(modelName + uuid);
        if (model == null) {
            IMMDModel m = LoadModel(modelName, 3);
            if (m == null)
                return null;
            MMDAnimManager.AddModel(m);
            AddModel(modelName + uuid, m, modelName);
            model = models.get(modelName + uuid);
        }
        return model;
    }

    public static Model GetModel(String modelName){
        return GetModel(modelName, "");
    }

    public static void AddModel(String Name, IMMDModel model, String modelName) {
        NativeFunc nf = NativeFunc.GetInst();
        EntityData ed = new EntityData();
        ed.stateLayers = new EntityData.EntityState[3];
        ed.playCustomAnim = false;
        ed.rightHandMat = nf.CreateMat();
        ed.leftHandMat = nf.CreateMat();
        ed.matBuffer = ByteBuffer.allocateDirect(64); //float * 16

        ModelWithEntityData m = new ModelWithEntityData();
        m.entityName = Name;
        m.model = model;
        m.modelName = modelName;
        m.entityData = ed;

        // VMC 데이터 초기화
        m.vmcData = new VMCData();

        model.ResetPhysics();
        model.ChangeAnim(MMDAnimManager.GetAnimModel(model, "idle"), 0);
        models.put(Name, m);
    }

    public static void ReloadModel() {
        for (Model i : models.values())
            DeleteModel(i);
        models = new HashMap<>();
    }

    static void DeleteModel(Model model) {
        MMDModelOpenGL.Delete((MMDModelOpenGL) model.model);

        //Unregister animation user
        MMDAnimManager.DeleteModel(model.model);
    }

    public static class Model {
        public IMMDModel model;
        String entityName;
        String modelName;
        public Properties properties = new Properties();
        boolean isPropertiesLoaded = false;

        public void loadModelProperties(boolean forceReload){
            if (isPropertiesLoaded && !forceReload)
                return;
            String path2Properties = gameDirectory + "/KAIMyEntity/" + modelName + "/model.properties";
            try {
                InputStream istream = new FileInputStream(path2Properties);
                properties.load(istream);
            } catch (IOException e) {
                logger.warn( "KAIMyEntity/" + modelName + "/model.properties not found" );
            }
            isPropertiesLoaded = true;
            KAIMyEntityClient.reloadProperties = false;
        }
    }

    public static class ModelWithEntityData extends Model {
        public EntityData entityData;
        public VMCData vmcData; // VMC 데이터 추가
    }

    /**
     * VMC 관련 데이터를 저장하는 클래스
     */
    public static class VMCData {
        // VMC 활성화 상태
        public boolean enableVMCOverride = false;

        // VMC 본 데이터 (본 이름 -> 본 데이터)
        public Map<String, VMCBoneData> vmcBones = new ConcurrentHashMap<>();

        // VMC 블렌드셰이프 데이터 (블렌드셰이프 이름 -> 가중치)
        public Map<String, Float> vmcBlendShapes = new ConcurrentHashMap<>();

        // 마지막 업데이트 시간
        public volatile long lastUpdateTime = 0;

        /**
         * VMC 연결 상태 확인
         */
        public boolean isVMCConnected() {
            VMCDataManager manager = VMCDataManager.getInstance();
            return manager.isVMCConnected();
        }

        /**
         * VMC 데이터가 유효한지 확인 (최근 1초 이내)
         */
        public boolean isDataValid() {
            return System.currentTimeMillis() - lastUpdateTime < 1000;
        }

        /**
         * VMC 본 데이터 업데이트
         */
        public void updateVMCBone(String boneName, Vector3f position, Quaternionf rotation) {
            VMCBoneData boneData = new VMCBoneData(boneName, position, rotation);
            vmcBones.put(boneName, boneData);
            lastUpdateTime = System.currentTimeMillis();
        }

        /**
         * VMC 블렌드셰이프 데이터 업데이트
         */
        public void updateVMCBlendShape(String blendShapeName, float weight) {
            vmcBlendShapes.put(blendShapeName, Math.max(0.0f, Math.min(1.0f, weight)));
            lastUpdateTime = System.currentTimeMillis();
        }

        /**
         * VMC 데이터 클리어
         */
        public void clearVMCData() {
            vmcBones.clear();
            vmcBlendShapes.clear();
            lastUpdateTime = 0;
        }
    }

    /**
     * VMC 본 데이터 클래스
     */
    public static class VMCBoneData {
        public String boneName;
        public Vector3f position;
        public Quaternionf rotation;
        public long timestamp;

        public VMCBoneData(String boneName, Vector3f position, Quaternionf rotation) {
            this.boneName = boneName;
            this.position = new Vector3f(position);
            this.rotation = new Quaternionf(rotation);
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * VMC 좌표계를 마인크래프트 좌표계로 변환
         */
        public Vector3f getMinecraftPosition() {
            // VMC는 Unity 좌표계 (Y-up, Z-forward)
            // Minecraft는 Y-up, Z는 남쪽이 양수
            return new Vector3f(-position.x, position.y, -position.z);
        }

        /**
         * VMC 회전을 마인크래프트 회전으로 변환
         */
        public Quaternionf getMinecraftRotation() {
            // 좌표계 변환에 따른 회전 조정
            return new Quaternionf(-rotation.x, rotation.y, -rotation.z, rotation.w);
        }
    }

    public static class EntityData {
        public static HashMap<EntityState, String> stateProperty = new HashMap<>() {{
            put(EntityState.Idle, "idle");
            put(EntityState.Walk, "walk");
            put(EntityState.Sprint, "sprint");
            put(EntityState.Air, "air");
            put(EntityState.OnClimbable, "onClimbable");
            put(EntityState.OnClimbableUp, "onClimbableUp");
            put(EntityState.OnClimbableDown, "onClimbableDown");
            put(EntityState.Swim, "swim");
            put(EntityState.Ride, "ride");
            put(EntityState.Ridden, "ridden");
            put(EntityState.Driven, "driven");
            put(EntityState.Sleep, "sleep");
            put(EntityState.ElytraFly, "elytraFly");
            put(EntityState.Die, "die");
            put(EntityState.SwingRight, "swingRight");
            put(EntityState.SwingLeft, "swingLeft");
            put(EntityState.Sneak, "sneak");
            put(EntityState.OnHorse, "onHorse");
            put(EntityState.Crawl, "crawl");
            put(EntityState.LieDown, "lieDown");
        }};
        public boolean playCustomAnim; //Custom animation played in layer 0.
        public long rightHandMat, leftHandMat;
        public EntityState[] stateLayers;
        ByteBuffer matBuffer;

        public enum EntityState {Idle, Walk, Sprint, Air, OnClimbable, OnClimbableUp, OnClimbableDown, Swim, Ride, Ridden, Driven, Sleep, ElytraFly, Die, SwingRight, SwingLeft, ItemRight, ItemLeft, Sneak, OnHorse, Crawl, LieDown}
    }
}