package com.xiaohunao.mine_team.common.team;

import com.xiaohunao.mine_team.MineTeam;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TeamManager {

    public static final String TEAM_PREFIX = MineTeam.MODID + "_";

    // 仇恨冷却时间：600 tick (30秒)
    private static final long HATE_COOLDOWN_TICKS = 2400;

    private static final Map<String, LivingEntity> lastHurtMap = new HashMap<>();
    private static final Map<String, Long> lastHurtTimestampMap = new HashMap<>();

    public static void initTeams(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();

        for (DyeColor color : DyeColor.values()) {
            String teamName = getTeamName(color);
            PlayerTeam playerTeam = scoreboard.getPlayerTeam(teamName);

            if (playerTeam == null) {
                playerTeam = scoreboard.addPlayerTeam(teamName);
            }

            playerTeam.setDisplayName(net.minecraft.network.chat.Component.translatable("color.minecraft." + color.getName()));

            ChatFormatting formatting = dyeToChatFormatting(color);
            if (formatting != null) {
                playerTeam.setColor(formatting);
            }

            // 只有新建时才强制设置
            if (playerTeam.getCollisionRule() == net.minecraft.world.scores.Team.CollisionRule.ALWAYS) {
                playerTeam.setAllowFriendlyFire(false);
                playerTeam.setSeeFriendlyInvisibles(true);
            }
        }
    }

    /**
     * 将 DyeColor 转换为最接近的 ChatFormatting
     * 已恢复为 16 个独立分支
     */
    public static ChatFormatting dyeToChatFormatting(DyeColor color) {
        return switch (color) {
            case WHITE -> ChatFormatting.WHITE;
            case ORANGE -> ChatFormatting.GOLD;
            case MAGENTA -> ChatFormatting.LIGHT_PURPLE;
            case LIGHT_BLUE -> ChatFormatting.AQUA;
            case YELLOW -> ChatFormatting.YELLOW;
            case LIME -> ChatFormatting.GREEN;
            case PINK -> ChatFormatting.LIGHT_PURPLE;
            case GRAY -> ChatFormatting.DARK_GRAY;
            case LIGHT_GRAY -> ChatFormatting.GRAY;
            case CYAN -> ChatFormatting.DARK_AQUA;
            case PURPLE -> ChatFormatting.DARK_PURPLE;
            case BLUE -> ChatFormatting.BLUE;
            case BROWN -> ChatFormatting.GOLD;
            case GREEN -> ChatFormatting.DARK_GREEN;
            case RED -> ChatFormatting.RED;
            case BLACK -> ChatFormatting.BLACK;
        };
    }

    public static String getTeamName(DyeColor color) {
        return TEAM_PREFIX + color.getName();
    }

    @Nullable
    public static PlayerTeam getTeam(Entity entity) {
        if (entity == null) return null;
        return entity.getTeam() instanceof PlayerTeam pt ? pt : null;
    }

    // --- 仇恨系统 (Game Time) ---

    public static void setLastHurtByMob(PlayerTeam team, LivingEntity target) {
        if (team == null || target == null) return;
        lastHurtMap.put(team.getName(), target);
        // 记录当前游戏时间
        lastHurtTimestampMap.put(team.getName(), target.level().getGameTime());
    }

    @Nullable
    public static LivingEntity getLastHurtByMob(PlayerTeam team) {
        if (team == null) return null;

        String teamName = team.getName();
        LivingEntity target = lastHurtMap.get(teamName);

        if (target == null) return null;

        // 清理检查：目标死亡或被移除
        if (!target.isAlive() || target.isRemoved()) {
            lastHurtMap.remove(teamName);
            return null;
        }

        // 超时检查：基于 GameTime (30秒)
        long lastTick = lastHurtTimestampMap.getOrDefault(teamName, 0L);
        long currentTick = target.level().getGameTime();

        if (currentTick - lastTick > HATE_COOLDOWN_TICKS) {
            lastHurtMap.remove(teamName);
            return null;
        }

        return target;
    }

    public static long getLastHurtTimestamp(PlayerTeam team) {
        if (team == null) return 0L;
        return lastHurtTimestampMap.getOrDefault(team.getName(), 0L);
    }
}