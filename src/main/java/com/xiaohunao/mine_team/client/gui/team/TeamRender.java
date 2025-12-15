package com.xiaohunao.mine_team.client.gui.team;

import com.google.common.collect.Maps;
import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.network.TeamActionPayload; // 这是一个新文件，下一步创建
import com.xiaohunao.mine_team.common.team.TeamManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TeamRender {
    private final EffectRenderingInventoryScreen<? extends AbstractContainerMenu> screen;

    private ImageButton teamIcon;
    private ImageButton teamPVPOn;
    private ImageButton teamPVPOff;
    private final Map<String, ImageButton> teamSmallIcons = Maps.newHashMap();

    public TeamRender(EffectRenderingInventoryScreen<? extends AbstractContainerMenu> screen) {
        this.screen = screen;
    }

    public void renderTeamIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (teamIcon == null || teamPVPOff == null || teamPVPOn == null || teamSmallIcons.isEmpty()) {
            return;
        }
        // 只有当主图标显示时才渲染
        if (this.teamIcon.visible) {
            this.teamIcon.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // 渲染 PvP 按钮 (状态控制可见性)
        if (this.teamPVPOff.visible) this.teamPVPOff.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.teamPVPOn.visible) this.teamPVPOn.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染颜色选择小图标
        renderTeamSmallIcon(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderTeamSmallIcon(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (ImageButton button : teamSmallIcons.values()) {
            if (button.visible) {
                button.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }

    public void initButton() {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;

        // --- 核心修改：从原版计分板获取队伍信息 ---
        Scoreboard scoreboard = localPlayer.getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(localPlayer.getScoreboardName());

        // 默认颜色 (如果没有队伍，或者是其他模组的队伍)
        DyeColor currentDyeColor = DyeColor.WHITE;

        // 只有当队伍是我们模组创建的 (前缀匹配) 才解析颜色
        if (team != null && team.getName().startsWith(TeamManager.TEAM_PREFIX)) {
            String colorName = team.getName().substring(TeamManager.TEAM_PREFIX.length());
            currentDyeColor = DyeColor.byName(colorName, DyeColor.WHITE);
        } else {
            // 如果玩家不在任何 MineTeam 队伍中，我们可以选择不渲染按钮，或者默认显示白色让玩家加入
            // 这里选择继续初始化，默认显示白色，方便玩家第一次加入
            currentDyeColor = DyeColor.WHITE;
        }

        String teamColor = currentDyeColor.getName();
        int iconSize = 16;
        int off = 6;

        // 1. 初始化主图标
        this.teamIcon = new ImageButton(screen.leftPos - iconSize, screen.topPos, iconSize, iconSize,
                createWidgetSprites("team/" + teamColor + "_team_icon"),
                button -> {
                    // 点击展开颜色选择板
                    this.teamIcon.visible = false;
                    this.teamPVPOn.visible = false;
                    this.teamPVPOff.visible = false;
                    visibleTeamSmallIcon(true);
                });

        // 2. 初始化 PvP 按钮
        // 注意：只有当玩家真的在队伍里时，点击才有意义，但为了简便我们允许点击，服务器会校验
        this.teamPVPOff = new ImageButton(screen.leftPos - iconSize, screen.topPos + iconSize + off, iconSize, iconSize,
                createWidgetSprites("team/pvp/" + teamColor + "_pvp_off"),
                button -> sendPvPPacket(true)); // 点击 OFF 按钮 -> 开启 PvP (FriendlyFire = true ?)
        // 原版逻辑：FriendlyFire=true 意味着可以打队友(PvP开启)。FriendlyFire=false 意味着不能打(PvP关闭/保护)。
        // 这里的按钮是 PVP_OFF (图标可能是一个盾牌)，点击它通常意味着“我想开启PvP”或者“我想关闭PvP”取决于图标设计。
        // 假设：PvP Off 按钮显示的时候，代表当前是关闭的。点击它意味着要开启。

        this.teamPVPOn = new ImageButton(screen.leftPos - iconSize, screen.topPos + iconSize + off, iconSize, iconSize,
                createWidgetSprites("team/pvp/" + teamColor + "_pvp_on"),
                button -> sendPvPPacket(false)); // 点击 ON 按钮 -> 关闭 PvP

        // 3. 初始化颜色选择板
        initSmallIcon(localPlayer);

        // 4. 更新按钮状态 (可见性)
        updateButtonsState(localPlayer, team);

        addRenderableWidget();
    }

    private void initSmallIcon(LocalPlayer localPlayer) {
        List<String> teamColors = Arrays.stream(DyeColor.values())
                .map(DyeColor::getName)
                .toList().reversed();

        int size = 8;
        int firstOff = MineTeam.IS_CONFLUENCE_LOADED ? 22 : 0;

        for (int i = 0; i < teamColors.size(); i++) {
            String newTeamColor = teamColors.get(i);
            int x = screen.leftPos - size - (i / 8) * size - (i / 8) * 2;
            int y = screen.topPos + (i % 8) * size + (i % 8) * 2;

            ImageButton teamSmallIconBtn = new ImageButton(x, y + firstOff, size, size,
                    createWidgetSprites("team/small/" + newTeamColor + "_team_small_icon"),
                    button -> {
                        // 点击颜色小图标 -> 发包换队
                        sendChangeTeamPacket(newTeamColor);
                        // UI 立即反馈 (虽然服务器还没回包，但为了流畅体验可以先切回去)
                        this.teamIcon.visible = true;
                        visibleTeamSmallIcon(false);
                        // 注意：这里不立即更新贴图，等待服务器数据同步(Scoreboard自动同步)会更稳健
                        // 但为了 UI 响应，我们可以手动刷新一下状态
                        // updateButtonsState... (需要等待 tick 更新)
                    });
            teamSmallIconBtn.visible = false;
            teamSmallIcons.put(newTeamColor, teamSmallIconBtn);
        }
    }

    public void addRenderableWidget() {
        screen.addRenderableWidget(this.teamIcon);
        screen.addRenderableWidget(this.teamPVPOn);
        screen.addRenderableWidget(this.teamPVPOff);
        for (ImageButton button : teamSmallIcons.values()) {
            screen.addRenderableWidget(button);
        }
    }

    /**
     * 根据当前队伍状态更新按钮的可见性和贴图
     */
    private void updateButtonsState(LocalPlayer player, PlayerTeam team) {
        if (team == null) {
            // 没队，默认显示 PvP Off，显示白色图标
            this.teamPVPOn.visible = false;
            this.teamPVPOff.visible = true;
            return;
        }

        // 1. 更新 PvP 按钮可见性
        // allowFriendlyFire = true -> 可以打队友 -> PvP 是开启的 -> 显示 PvP On 按钮 (点击可关闭)
        boolean isPvPEnabled = team.isAllowFriendlyFire();
        this.teamPVPOn.visible = isPvPEnabled;
        this.teamPVPOff.visible = !isPvPEnabled;

        // 2. 更新贴图 (根据队伍颜色)
        if (team.getName().startsWith(TeamManager.TEAM_PREFIX)) {
            String colorName = team.getName().substring(TeamManager.TEAM_PREFIX.length());
            setImageButtonSprites(this.teamIcon, "team/" + colorName + "_team_icon");
            setImageButtonSprites(this.teamPVPOn, "team/pvp/" + colorName + "_pvp_on");
            setImageButtonSprites(this.teamPVPOff, "team/pvp/" + colorName + "_pvp_off");
        }
    }

    // --- 网络通信 ---

    private void sendChangeTeamPacket(String colorName) {
        // 发送换队请求：Action 0 = Join Team
        PacketDistributor.sendToServer(new TeamActionPayload(0, colorName, false));
    }

    private void sendPvPPacket(boolean enablePvP) {
        // 发送PvP请求：Action 1 = Set PvP
        PacketDistributor.sendToServer(new TeamActionPayload(1, "", enablePvP));
    }

    // --- 辅助方法 ---

    private void visibleTeamSmallIcon(boolean visible) {
        for (ImageButton button : teamSmallIcons.values()) {
            button.visible = visible;
        }
    }

    private void setImageButtonSprites(ImageButton button, String path) {
        button.sprites = createWidgetSprites(path);
    }

    private WidgetSprites createWidgetSprites(String path) {
        return new WidgetSprites(MineTeam.asResource(path), MineTeam.asResource(path));
    }
}