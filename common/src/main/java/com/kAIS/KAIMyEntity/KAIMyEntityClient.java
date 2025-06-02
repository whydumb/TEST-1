package com.kAIS.KAIMyEntity;

import com.kAIS.KAIMyEntity.renderer.MMDModelManager;
import com.kAIS.KAIMyEntity.renderer.MMDTextureManager;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

public class KAIMyEntityClient {
    public static final Logger logger = LogManager.getLogger();
    public static int usingMMDShader = 0;
    public static boolean reloadProperties = false;
    static final Minecraft MCinstance = Minecraft.getInstance();
    static final String gameDirectory = MCinstance.gameDirectory.getAbsolutePath();
    static final int BUFFER = 512;
    static final long TOOBIG = 0x6400000;
    static final int TOOMANY = 1024;

    public static void initClient() {
        logger.info("KAIMyEntity InitClient starting (minimal version)...");

        try {
            checkKAIMyEntityFolder();
            MMDModelManager.Init();
            MMDTextureManager.Init();

            logger.info("KAIMyEntityClient initialized successfully (minimal version)");
        } catch (Exception e) {
            logger.error("Failed to initialize KAIMyEntityClient", e);
        }
    }

    public static void shutdown() {
        logger.info("KAIMyEntityClient shutdown complete");
    }

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