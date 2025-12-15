package com.xiaohunao.mine_team.common.entity.goal;

import com.xiaohunao.mine_team.common.team.TeamManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class TeamOwnerHurtTargetGoal extends TargetGoal {
    private LivingEntity teamLastHurt;
    private long timestamp;

    public TeamOwnerHurtTargetGoal(Mob tameLivingEntity) {
        super(tameLivingEntity, false);
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // 1. 获取当前生物的原版队伍
        PlayerTeam myTeam = TeamManager.getTeam(this.mob);
        if (myTeam == null) return false;

        // 2. 从 TeamManager 获取该队伍记录的“最后攻击者”
        this.teamLastHurt = TeamManager.getLastHurtByMob(myTeam);
        if (this.teamLastHurt == null) return false;

        // 3. 检查时间戳，防止重复触发同一个仇恨事件
        long lastAttackedTime = TeamManager.getLastHurtTimestamp(myTeam);
        if (lastAttackedTime == this.timestamp) return false;

        // 4. 标准攻击检查 (目标必须活着、在范围内、非旁观者等)
        if (!this.canAttack(this.teamLastHurt, TargetingConditions.DEFAULT)) return false;

        // 5. 确保是敌人 (防止内讧)
        return isEnemy(this.mob, this.teamLastHurt);
    }

    /**
     * 判断两者是否为敌人
     */
    private boolean isEnemy(LivingEntity attacker, LivingEntity target) {
        PlayerTeam attackerTeam = TeamManager.getTeam(attacker);
        PlayerTeam targetTeam = TeamManager.getTeam(target);

        // 如果其中一个没队，或者队伍不同且不结盟，则视为敌人
        if (attackerTeam == null || targetTeam == null) return true;
        // isAlliedTo 是原版方法，检查是否同队或同盟
        return !attackerTeam.isAlliedTo(targetTeam);
    }

    @Override
    public void start() {
        // 获取“最后攻击者”所在的队伍
        PlayerTeam enemyTeam = TeamManager.getTeam(this.teamLastHurt);

        // --- 斗蛐蛐核心逻辑 ---

        // 如果攻击者属于某个队伍，我们不一定非要打攻击者本人，而是打该队伍离我最近的成员
        if (enemyTeam != null) {
            double range = this.getFollowDistance();
            // 搜索范围：以自身为中心
            AABB searchBox = this.mob.getBoundingBox().inflate(range, 10.0, range);

            List<LivingEntity> potentialTargets = this.mob.level().getEntitiesOfClass(
                    LivingEntity.class,
                    searchBox,
                    entity -> {
                        // 筛选条件：活着的 + 属于那个特定敌对队伍的
                        if (entity == null || !entity.isAlive()) return false;
                        PlayerTeam otherTeam = TeamManager.getTeam(entity);
                        // 比较队伍名称是否一致
                        return otherTeam != null && otherTeam.getName().equals(enemyTeam.getName());
                    }
            );

            // 在所有潜在目标中，找到离我自己最近的一个
            LivingEntity closestEnemy = potentialTargets.stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(this.mob)))
                    // 如果找不到其他队员，就回退去打原本的攻击者
                    .orElse(this.teamLastHurt);

            this.mob.setTarget(closestEnemy);
        } else {
            // 如果攻击者没有队伍（比如野生怪物），直接锁定它
            this.mob.setTarget(this.teamLastHurt);
        }

        // --- 逻辑结束 ---

        // 更新时间戳，标记该仇恨已处理
        PlayerTeam myTeam = TeamManager.getTeam(this.mob);
        if (myTeam != null) {
            this.timestamp = TeamManager.getLastHurtTimestamp(myTeam);
        }

        super.start();
    }
}