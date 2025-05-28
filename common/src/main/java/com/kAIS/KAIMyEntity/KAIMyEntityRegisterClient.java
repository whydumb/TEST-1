@Environment(EnvType.CLIENT)
public class KAIMyEntityRegisterClient {
    static final Logger logger = LogManager.getLogger();
    
    // 기존 키바인딩들...
    static KeyMapping keyToggleVMC = new KeyMapping("key.toggleVMC", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.title");
    
    static KeyMapping[] keyBindings = new KeyMapping[]{
        keyCustomAnim1, keyCustomAnim2, keyCustomAnim3, keyCustomAnim4, 
        keyReloadModels, keyResetPhysics, keyReloadProperties, keyToggleVMC // VMC 토글 추가
    };

    public static void Register() {
        Minecraft MCinstance = Minecraft.getInstance();
        
        // 기존 키바인딩 등록...
        for (KeyMapping i : keyBindings)
            KeyBindingHelper.registerKeyBinding(i);

        // VMC 토글 키바인딩 이벤트
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyToggleVMC.consumeClick()) {
                KAIMyEntityClient.toggleVMC();
            }
        });
        
        // 기존 이벤트들...
        
        logger.info("KAIMyEntityRegisterClient.Register() finished");
    }
}