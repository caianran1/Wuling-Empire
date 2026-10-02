package com.wuling.empire.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.wuling.empire.WulingEmpire;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 快捷键注册（都归在「武灵帝国」按键分类下）。
 *
 * <ul>
 *   <li><b>Shift + M</b> = 凝聚武灵（生成实体实物 / 召唤僵尸）</li>
 *   <li><b>Shift + N</b> = 打开武灵升级面板</li>
 *   <li><b>Shift + P</b> = 让召唤出的僵尸消散（腐肉武灵专用）</li>
 * </ul>
 *
 * Shift 在 {@link ClientTickHandler} 里额外判断，
 * 因为 1.20.1 的 {@code KeyMapping} 不支持修饰键。
 *
 * <p>三个快捷键都<b>不需要携带武灵绑定器</b>（0.2.20 起）——
 * 绑定器只是「选种类」的入口。
 */
@Mod.EventBusSubscriber(modid = WulingEmpire.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeyMappings {

    private ModKeyMappings() {
    }

    public static final String CATEGORY = "key.categories.wulingdiguo";

    /** Shift + M：凝聚出实体武灵 / 召唤僵尸 */
    public static final KeyMapping CONDENSE = new KeyMapping(
            "key.wulingdiguo.condense",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY);

    /** Shift + N：打开武灵升级面板 */
    public static final KeyMapping PANEL = new KeyMapping(
            "key.wulingdiguo.panel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY);

    /** Shift + P：让召唤出的僵尸消散 */
    public static final KeyMapping DISMISS = new KeyMapping(
            "key.wulingdiguo.dismiss",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY);

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CONDENSE);
        event.register(PANEL);
        event.register(DISMISS);
    }
}
