package com.wuling.empire.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wuling.empire.entity.CorpseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 尸体渲染器。
 *
 * 思路：不自己建模，而是用原版渲染器渲染「还原出来的原怪物模型」，
 * 再整体放倒成仰面躺地的姿态 —— 这样任何怪物都能正确显示，
 * 包括后续可能加入的自定义生物。
 *
 * 姿态推导（重要，改之前先看懂）：
 *  1. CorpseEntity#getOrCreateDisplay 把展示用实体的 yBodyRot / yHeadRot 归零，
 *     所以原版渲染器输出的始终是「标准直立、脚在原点、面朝南」的模型。
 *     记模型三轴：up = (0,1,0)，forward = (0,0,1)=南，side = (-1,0,0)。
 *  2. 我们要的最终效果：头朝向怪物死亡时的朝向 h(θ) = (-sinθ, 0, cosθ)，仰面朝天。
 *     即 up -> h，forward -> (0,1,0)。
 *  3. 满足条件的旋转矩阵可以分解成 P = Ry(180° - θ) · Rx(-90°)。
 *     PoseStack 的 mulPose 是从外向内叠加的，所以调用顺序必须是先 Y 后 X。
 *  4. 因为旋转是绕「实体坐标原点」（也就是脚底的高度）发生的，
 *     模型的身体厚度会以原点为中心上下各占一半，所以要先把整体在世界坐标下抬高一点，
 *     避免半个身子嵌进地里。
 */
public class CorpseRenderer extends EntityRenderer<CorpseEntity> {

    /** 抬高补偿：让躺着的身体贴地而不是半陷。数值可按观感微调 */
    private static final float HEIGHT_OFFSET = 0.30F;
    /** 略微缩小，看起来更像瘫倒而不是硬直 */
    private static final float SCALE = 0.95F;

    public CorpseRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4F;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void render(CorpseEntity corpse, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 阴影由基类负责
        super.render(corpse, entityYaw, partialTick, poseStack, buffer, packedLight);

        Entity display = corpse.getOrCreateDisplay();
        if (display == null) {
            return;
        }

        EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(display);
        if (renderer == null) {
            return;
        }

        poseStack.pushPose();

        // (1) 世界坐标系下抬高，绕 sop 原点旋转前先移位
        poseStack.translate(0.0D, (double) HEIGHT_OFFSET, 0.0D);
        // (2) 略微缩小
        poseStack.scale(SCALE, SCALE, SCALE);
        // (3) 头朝向死亡时的朝向
        //     末影龙尸体（Part 3）不加偏航旋转——它的体型是对称的，旋转反而显得扭着，
        //     直接放倒成标准仰躺姿态即可。
        if (!corpse.isDragon()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - corpse.getVisualYaw()));
        }
        // (4) 放倒
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));

        // 原版渲染器直接接管剩余工作
        renderer.render(display, 0.0F, partialTick, poseStack, buffer, packedLight);

        poseStack.popPose();

        // Part 3：末影龙尸体的紫色光柱（客户端粒子）
        if (corpse.isDragon()) {
            spawnLightPillar(corpse);
        }
    }

    /**
     * 末影龙尸体上方的紫色光柱：沿竖直方向升起一串末地传送门粒子，
     * 形成稳定的紫色光柱效果。每 4 tick 生成一批，避免过于密集。
     */
    private void spawnLightPillar(CorpseEntity corpse) {
        Level level = corpse.level();
        if (level == null) {
            return;
        }
        if (corpse.tickCount % 4 != 0) {
            return;
        }
        double cx = corpse.getX();
        double cz = corpse.getZ();
        for (int i = 0; i < 5; i++) {
            double ox = (level.random.nextDouble() - 0.5D) * 3.0D;
            double oz = (level.random.nextDouble() - 0.5D) * 3.0D;
            double oy = corpse.getY() + level.random.nextDouble() * 14.0D;
            level.addParticle(ParticleTypes.PORTAL,
                    cx + ox, oy, cz + oz, 0.0D, 0.06D, 0.0D);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CorpseEntity entity) {
        // 实际纹理由各怪物自己的渲染器提供，这里不会被用到
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
