package com.xiaohunao.mine_team.common.event.subscriber;

import com.xiaohunao.mine_team.MineTeam;
import com.xiaohunao.mine_team.common.config.MineTeamConfig; // 确保导入 Config
import com.xiaohunao.mine_team.common.team.TeamManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = MineTeam.MODID)
public class LivingEventSubscriber {

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity attacker = event.getEntity();
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();

        if (attacker.level().isClientSide || newTarget == null) {
            return;
        }

        PlayerTeam attackerTeam = TeamManager.getTeam(attacker);
        PlayerTeam targetTeam = TeamManager.getTeam(newTarget);

        if (attackerTeam != null && targetTeam != null && attackerTeam.isAlliedTo(targetTeam)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingPvP(LivingIncomingDamageEvent event) {
        LivingEntity hurtEntity = event.getEntity();
        DamageSource source = event.getSource();
        Entity attackEntity = source.getEntity();

        if (hurtEntity.level() instanceof ServerLevel && attackEntity instanceof LivingEntity livingAttacker && attackEntity != hurtEntity) {
            PlayerTeam hurtTeam = TeamManager.getTeam(hurtEntity);
            PlayerTeam attackTeam = TeamManager.getTeam(livingAttacker);

            if (hurtTeam != null && attackTeam != null && hurtTeam.isAlliedTo(attackTeam)) {
                if (!hurtTeam.isAllowFriendlyFire()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onSetTeamLastHurtMob(LivingIncomingDamageEvent event) {
        LivingEntity hurtEntity = event.getEntity();
        DamageSource source = event.getSource();
        Entity attackEntity = source.getEntity();

        if (!(hurtEntity.level() instanceof ServerLevel)) return;

        // 修复点 1: 移除了多余的 'attackEntity == null' 检查
        if (!(attackEntity instanceof LivingEntity livingAttacker)) return;

        if (hurtEntity == livingAttacker) return;

        PlayerTeam hurtTeam = TeamManager.getTeam(hurtEntity);
        if (hurtTeam != null) {
            TeamManager.setLastHurtByMob(hurtTeam, livingAttacker);
        }

        if (MineTeamConfig.isTeamFocusFireEnabled()) {
            PlayerTeam attackTeam = TeamManager.getTeam(livingAttacker);
            if (attackTeam != null) {
                TeamManager.setLastHurtByMob(attackTeam, hurtEntity);
            }
        }
    }
}