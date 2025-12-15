package com.xiaohunao.mine_team;

import com.mojang.logging.LogUtils;
import com.xiaohunao.mine_team.common.config.MineTeamConfig;
// import com.xiaohunao.mine_team.common.init.MTAttachmentTypes; // 已删除，不再需要导入
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(MineTeam.MODID)
public class MineTeam {
    public static final String MODID = "mine_team";
    public static final Logger LOGGER = LogUtils.getLogger();
    // 检查兼容性模组是否存在
    public static final boolean IS_CONFLUENCE_LOADED = ModList.get().isLoaded("confluence");

    public MineTeam(IEventBus modEventBus, ModContainer modContainer) {
        // 核心修改：删除了附件类型的注册，因为我们改用了原版计分板
        // MTAttachmentTypes.ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(this::onFMLCommonSetup);

        // 注册配置文件
        modContainer.registerConfig(ModConfig.Type.COMMON, MineTeamConfig.CONFIG, "mine_team.toml");
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @SubscribeEvent
    public void onFMLCommonSetup(FMLCommonSetupEvent event) {
        // 初始化驯服材料配置
        MineTeamConfig.loadTamingMaterials();
    }
}