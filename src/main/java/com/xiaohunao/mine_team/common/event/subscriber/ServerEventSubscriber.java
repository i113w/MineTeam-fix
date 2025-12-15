package com.xiaohunao.mine_team.common.event.subscriber;

import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.team.TeamManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = MineTeam.MODID)
public class ServerEventSubscriber {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (server != null) {
            // 服务器启动完成，加载计分板数据后
            // 自动检查并创建所需的 16 色队伍
            TeamManager.initTeams(server);
        }
    }
}