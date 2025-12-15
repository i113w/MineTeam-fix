package com.xiaohunao.mine_team.common.event.subscriber;

import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.config.MineTeamConfig;
import com.xiaohunao.mine_team.common.entity.goal.TeamOwnerHurtTargetGoal; // 导入我们的AI
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob; // 导入 Mob
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = MineTeam.MODID)
public class MobTeamEventSubscriber {
    @SubscribeEvent
    public static void onPlayerInteractEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (MineTeam.IS_CONFLUENCE_LOADED) return;
        Level level = event.getLevel();

        if (level.isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        ItemStack itemstack = player.getItemInHand(event.getHand());
        Entity target = event.getTarget();
        Ingredient tamingMaterial = MineTeamConfig.getTamingMaterial(target.getType());

        if (tamingMaterial.test(itemstack) && target instanceof LivingEntity livingEntity) {
            ServerLevel serverLevel = (ServerLevel) level;
            Scoreboard scoreboard = serverLevel.getScoreboard();

            PlayerTeam targetTeam = scoreboard.getPlayersTeam(target.getStringUUID());
            PlayerTeam playerTeam = scoreboard.getPlayersTeam(player.getScoreboardName());

            if (targetTeam == null && playerTeam != null) {
                itemstack.consume(1, player);

                // 1. 加入队伍
                scoreboard.addPlayerToTeam(livingEntity.getStringUUID(), playerTeam);

                // 2. 增强属性 (跟随距离)
                var followAttribute = livingEntity.getAttribute(Attributes.FOLLOW_RANGE);
                if (followAttribute != null) {
                    double newRange = 128.0;
                    if (followAttribute.getBaseValue() < newRange) {
                        followAttribute.setBaseValue(newRange);
                    }
                }
                livingEntity.setHealth(livingEntity.getMaxHealth());

                // 3. --- 核心修改：注入 AI ---
                // 只有 Mob (怪物/动物) 才有 AI 系统，玩家没有
                if (livingEntity instanceof Mob mob) {
                    // targetSelector 是负责“选择攻击目标”的 AI 列表
                    // addGoal(优先级, 目标): 1 是最高优先级
                    // 这样一旦队伍里有人挨打，这个 Goal 就会立刻接管，让它去攻击敌人
                    mob.targetSelector.addGoal(1, new TeamOwnerHurtTargetGoal(mob));
                }

                // 视觉反馈
                livingEntity.setGlowingTag(true);
            }
        }
    }
}