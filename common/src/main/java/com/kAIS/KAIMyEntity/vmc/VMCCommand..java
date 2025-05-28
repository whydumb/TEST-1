package com.kAIS.KAIMyEntity.vmc;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

/**
 * VMC 관련 게임 내 명령어
 */
public class VMCCommand {
    private static VMCReceiver vmcReceiver = new VMCReceiver();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vmc")
            .then(Commands.literal("start")
                .executes(VMCCommand::startVMC)
                .then(Commands.argument("port", IntegerArgumentType.integer(1024, 65535))
                    .executes(VMCCommand::startVMCWithPort)))
            .then(Commands.literal("stop")
                .executes(VMCCommand::stopVMC))
            .then(Commands.literal("toggle")
                .executes(VMCCommand::toggleVMC))
            .then(Commands.literal("status")
                .executes(VMCCommand::getVMCStatus)));
    }
    
    private static int startVMC(CommandContext<CommandSourceStack> context) {
        vmcReceiver.startReceiving();
        context.getSource().sendSuccess(() -> Component.literal("VMC 수신을 시작했습니다 (포트: 39539)"), false);
        return 1;
    }
    
    private static int startVMCWithPort(CommandContext<CommandSourceStack> context) {
        int port = IntegerArgumentType.getInteger(context, "port");
        vmcReceiver.startReceiving(port);
        context.getSource().sendSuccess(() -> Component.literal("VMC 수신을 시작했습니다 (포트: " + port + ")"), false);
        return 1;
    }
    
    private static int stopVMC(CommandContext<CommandSourceStack> context) {
        vmcReceiver.stopReceiving();
        VMCDataManager.getInstance().enableVMCOverride = false;
        context.getSource().sendSuccess(() -> Component.literal("VMC 수신을 중지했습니다"), false);
        return 1;
    }
    
    private static int toggleVMC(CommandContext<CommandSourceStack> context) {
        VMCDataManager vmcData = VMCDataManager.getInstance();
        
        if (!vmcData.isConnected()) {
            context.getSource().sendFailure(Component.literal("VMC가 연결되지 않았습니다. 먼저 /vmc start로 시작하세요."));
            return 0;
        }
        
        vmcData.enableVMCOverride = !vmcData.enableVMCOverride;
        String status = vmcData.enableVMCOverride ? "활성화" : "비활성화";
        context.getSource().sendSuccess(() -> Component.literal("VMC 오버라이드가 " + status + "되었습니다"), false);
        return 1;
    }
    
    private static int getVMCStatus(CommandContext<CommandSourceStack> context) {
        VMCDataManager vmcData = VMCDataManager.getInstance();
        String connectionStatus = vmcData.isConnected() ? "연결됨" : "연결 안됨";
        String overrideStatus = vmcData.enableVMCOverride ? "활성화" : "비활성화";
        
        context.getSource().sendSuccess(() -> Component.literal(
            "VMC 상태:\n" +
            "- 연결: " + connectionStatus + "\n" +
            "- 오버라이드: " + overrideStatus + "\n" +
            "- 본 데이터: " + vmcData.getBoneDataList().size() + "개\n" +
            "- 표정 데이터: " + vmcData.getBlendShapeDataList().size() + "개"
        ), false);
        return 1;
    }
}