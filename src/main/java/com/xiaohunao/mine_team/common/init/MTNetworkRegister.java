package com.xiaohunao.mine_team.common.init;

import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.network.TeamActionPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// 移除 bus = EventBusSubscriber.Bus.MOD，NeoForge 现在会自动识别
@EventBusSubscriber(modid = MineTeam.MODID)
public class MTNetworkRegister {
    public static final String VERSION = "1.0.0";

    @SubscribeEvent
    public static void registerPayload(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);

        // 注册唯一的动作包 (客户端 -> 服务端)
        registrar.playToServer(
                TeamActionPayload.TYPE,
                TeamActionPayload.STREAM_CODEC,
                TeamActionPayload::serverHandle
        );
    }
}