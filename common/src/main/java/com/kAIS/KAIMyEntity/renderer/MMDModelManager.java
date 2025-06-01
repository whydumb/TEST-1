package com.kAIS.KAIMyEntity.renderer;

import com.kAIS.KAIMyEntity.KAIMyEntityClient;
import com.kAIS.KAIMyEntity.NativeFunc;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public static IMMDModel LoadModel(String modelName) {
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
        return MMDModelOpenGL.Create(modelFilenameStr, modelDirStr, isPMD);
    }

    public static Model GetModel(String modelName, String uuid) {
        Model model = models.get(modelName + uuid);
        if (model == null) {
            IMMDModel m = LoadModel(modelName);
            if (m == null)
                return null;
            AddModel(modelName + uuid, m, modelName);
            model = models.get(modelName + uuid);
        }
        return model;
    }

    public static Model GetModel(String modelName){
        return GetModel(modelName, "");
    }

    public static void AddModel(String Name, IMMDModel model, String modelName) {
        Model m = new Model();
        m.entityName = Name;
        m.model = model;
        m.modelName = modelName;
        models.put(Name, m);
    }

    public static void ReloadModel() {
        for (Model i : models.values())
            DeleteModel(i);
        models = new HashMap<>();
    }

    static void DeleteModel(Model model) {
        MMDModelOpenGL.Delete((MMDModelOpenGL) model.model);
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
}