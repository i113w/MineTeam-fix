package com.xiaohunao.mine_team.common.network;

import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.team.TeamManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TeamActionPayload(int action, String data, boolean state) implements CustomPacketPayload {

    // 定义包的唯一 ID
    public static final Type<TeamActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MineTeam.MODID, "team_action"));

    // 定义数据如何传输 (int, String, boolean)
    public static final StreamCodec<ByteBuf, TeamActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TeamActionPayload::action,
            ByteBufCodecs.STRING_UTF8, TeamActionPayload::data,
            ByteBufCodecs.BOOL, TeamActionPayload::state,
            TeamActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 服务端处理逻辑：收到包后做什么
     */
    public static void serverHandle(final TeamActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            Scoreboard scoreboard = player.getServer().getScoreboard();

            // Action 0: 切换队伍
            if (payload.action == 0) {
                String colorName = payload.data; // 例如 "red"
                // 校验颜色名是否合法
                DyeColor color = DyeColor.byName(colorName, null);
                if (color != null) {
                    String teamName = TeamManager.getTeamName(color); // 转换成 "mine_team_red"
                    PlayerTeam team = scoreboard.getPlayerTeam(teamName);

                    // 如果队伍存在，把玩家加进去
                    if (team != null) {
                        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                    }
                }
            }
            // Action 1: 切换 PvP 状态
            else if (payload.action == 1) {
                PlayerTeam team = scoreboard.getPlayersTeam(player.getScoreboardName());
                // 只有当玩家在 MineTeam 队伍中时才允许修改
                if (team != null && team.getName().startsWith(TeamManager.TEAM_PREFIX)) {
                    // 设置友军伤害 (注意：friendlyFire=true 意味着开启PvP，=false 意味着关闭PvP)
                    team.setAllowFriendlyFire(payload.state);
                }
            }
        });
    }
}