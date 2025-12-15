package com.xiaohunao.mine_team.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = "mine_team")
public class TeamGetCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    // 预编译 UUID 的正则模式，提高运行效率
    // 匹配标准 UUID 格式 (8-4-4-4-12 十六进制数字)
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // ... 注册逻辑保持不变 ...
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandNode<CommandSourceStack> teamNode = dispatcher.getRoot().getChild("team");

        if (teamNode != null) {
            for (CommandNode<CommandSourceStack> child : teamNode.getChildren()) {
                Predicate<CommandSourceStack> oldReq = child.getRequirement();
                setRequirement(child, source -> source.hasPermission(2) && oldReq.test(source));
            }
            setRequirement(teamNode, source -> true);
        }

        dispatcher.register(
                Commands.literal("team")
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> executeGet(context, EntityArgument.getPlayer(context, "target"), true, false))
                                        .then(Commands.argument("showMembers", BoolArgumentType.bool())
                                                .executes(context -> executeGet(context, EntityArgument.getPlayer(context, "target"), BoolArgumentType.getBool(context, "showMembers"), false))
                                                .then(Commands.argument("showAllEntities", BoolArgumentType.bool())
                                                        .executes(context -> executeGet(context, EntityArgument.getPlayer(context, "target"), BoolArgumentType.getBool(context, "showMembers"), BoolArgumentType.getBool(context, "showAllEntities")))
                                                )
                                        )
                                )
                        )
        );
    }

    private static void setRequirement(CommandNode<CommandSourceStack> node, Predicate<CommandSourceStack> newRequirement) {
        try {
            Field reqField = CommandNode.class.getDeclaredField("requirement");
            reqField.setAccessible(true);
            reqField.set(node, newRequirement);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Failed to modify command requirement via reflection for node: {}", node.getName(), e);
        }
    }

    private static int executeGet(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer, boolean showMembers, boolean showAllEntities) {
        Scoreboard scoreboard = context.getSource().getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(targetPlayer.getScoreboardName());

        if (team == null) {
            context.getSource().sendFailure(
                    Component.translatable("message.mine_team.no_team", targetPlayer.getDisplayName())
            );
            return 0;
        }

        Component teamName = team.getDisplayName().copy().withStyle(team.getColor());
        context.getSource().sendSuccess(() ->
                        Component.translatable("message.mine_team.in_team", targetPlayer.getDisplayName(), teamName),
                false
        );

        if (!showMembers) {
            return 1;
        }

        Collection<String> members = team.getPlayers();
        List<String> displayMembers = new ArrayList<>();

        for (String memberName : members) {
            // 跳过自己
            if (memberName.equals(targetPlayer.getScoreboardName())) continue;

            // 核心逻辑修改：
            // 如果开启了 showAllEntities，则无条件添加
            // 如果没开启，则检查是否为 UUID
            // - 是 UUID -> 说明是实体 -> 隐藏
            // - 不是 UUID -> 说明是玩家名 -> 显示
            if (showAllEntities || !UUID_PATTERN.matcher(memberName).matches()) {
                displayMembers.add(memberName);
            }
        }

        if (displayMembers.isEmpty()) {
            context.getSource().sendSuccess(() ->
                            Component.translatable("message.mine_team.no_other_members").withStyle(ChatFormatting.GRAY),
                    false
            );
        } else {
            String memberListStr = String.join(", ", displayMembers);
            context.getSource().sendSuccess(() ->
                            Component.translatable("message.mine_team.other_members", memberListStr).withStyle(ChatFormatting.GRAY),
                    false
            );
        }
        return 1;
    }
}