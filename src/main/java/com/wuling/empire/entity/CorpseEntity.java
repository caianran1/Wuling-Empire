package com.wuling.empire.entity;

import com.wuling.empire.Config;
import com.wuling.empire.item.ModItems;
import com.wuling.empire.item.SpiritBeadItem;
import com.wuling.empire.item.SpiritQuality;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 倒地尸体实体。
 *
 * 服务端职责：
 *  1. 保存死亡怪物的外观数据（NBT），供客户端还原模型
 *  2. 保存该尸体承载的灵珠列表
 *  3. 计时，超时后消失（可配置是否把灵珠掉地上）
 *
 * 客户端职责：
 *  1. 由服务端在进 Tracking 时同步来的 NBT 还原出原怪物实体用于渲染（见 CorpseRenderer）
 *
 * 交互：玩家右键 -> 灵珠直接进入背包（在 ModEvents 中处理）
 */
public class CorpseEntity extends Entity {

    private static final String KEY_BEADS = "Beads";
    private static final String KEY_MOB_DATA = "MobData";
    private static final String KEY_LIFE = "Life";
    private static final String KEY_YAW = "VisualYaw";
    private static final String KEY_DRAGON = "Dragon";

    /** 尸体承载的灵珠 */
    private final List<ItemStack> beads = new ArrayList<>();
    /** 死亡怪物的存档数据，用于客户端还原外观 */
    private CompoundTag mobData = new CompoundTag();
    /** 死亡怪物的外观朝向 */
    private float visualYaw = 0.0F;
    /** 是否末影龙尸体（Part 3）：不参与偏航旋转、带紫色光柱、大命中箱、采集后保留 */
    private boolean dragon = false;
    /** 已经存在的 tick 数 */
    private int life = 0;

    /** 客户端专用：由 mobData 还原出的用于渲染的实体（不加入世界、不 tick） */
    private Entity displayEntity;

    public CorpseEntity(EntityType<? extends CorpseEntity> type, Level level) {
        super(type, level);
    }

    /** 服务端构造入口：在怪物死亡处生成尸体 */
    public static CorpseEntity create(Level level, LivingEntity dead) {
        CorpseEntity corpse = new CorpseEntity(ModEntities.SPIRIT_CORPSE.get(), level);
        corpse.captureAppearance(dead);
        corpse.setPos(dead.getX(), dead.getY(), dead.getZ());
        corpse.setDeltaMovement(dead.getDeltaMovement().multiply(0.2D, 0.0D, 0.2D));
        corpse.fillBeads(dead);

        // Part 3：末影龙死亡生成专属尸体，掉落一颗极品灵珠（来源=末影龙）
        if (dead instanceof EnderDragon) {
            corpse.dragon = true;
            corpse.beads.clear();
            ItemStack bead = new ItemStack(ModItems.BEADS.get(SpiritQuality.JI).get());
            SpiritBeadItem.setSource(bead, "minecraft:ender_dragon");
            corpse.addBead(bead);
        }
        return corpse;
    }

    public boolean isDragon() {
        return dragon;
    }

    @Override
    protected void defineSynchedData() {
        // 尸体不使用额外的 EntityDataAccessor：
        // 服务端 NBT 体积大且只在实体出现时需要一次，走 CorpseDataPacket 更合适
    }

    // ===================== 外观 =====================

    private void captureAppearance(LivingEntity dead) {
        CompoundTag tag = new CompoundTag();
        dead.saveWithoutId(tag);

        // 去掉运行时无关或体积较大的字段，减小同步包体
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("Rotation");
        tag.remove("Health");
        tag.remove("HurtTime");
        tag.remove("HurtByTimestamp");
        tag.remove("DeathTime");
        tag.remove("AbsorptionAmount");
        tag.remove("Attributes");
        tag.remove("ActiveEffects");
        tag.remove("Effects");
        tag.remove("Leash");
        tag.remove("Passengers");
        tag.remove("Riding");
        tag.remove("Fire");
        tag.remove("Air");

        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        if (key != null) {
            tag.putString("id", key.toString());
        }

        this.mobData = tag;
        this.visualYaw = dead.getYRot();
        this.setYRot(dead.getYRot());
    }

    // ===================== 外观 =====================

    public CompoundTag getMobData() {
        return mobData;
    }

    /** 客户端：由 CorpseDataPacket 写入服务端同步过来的外观数据 */
    public void setMobData(CompoundTag tag, float yaw) {
        this.mobData = tag == null ? new CompoundTag() : tag;
        this.visualYaw = yaw;
        this.displayEntity = null;
    }

    public float getVisualYaw() {
        return visualYaw;
    }

    /**
     * 客户端：根据同步来的 NBT 还原实体用于渲染。
     * 返回的实体不加入世界、不 tick，仅作为模型数据源。
     */
    @Nullable
    public Entity getOrCreateDisplay() {
        if (this.displayEntity != null) {
            return this.displayEntity;
        }
        Level level = this.level();
        if (level == null || this.mobData == null || !this.mobData.contains("id")) {
            return null;
        }

        Entity created = EntityType.loadEntityRecursive(this.mobData, level, Function.identity());
        if (created == null) {
            ResourceLocation fallbackId = ResourceLocation.tryParse(this.mobData.getString("id"));
            if (fallbackId == null) {
                return null;
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(fallbackId);
            created = type == null ? null : type.create(level);
        }
        if (created == null) {
            return null;
        }

        // 关键：LivingEntityRenderer 渲染时的朝向取自 yBodyRot / yHeadRot，
        // 而不是 render() 传入的 yaw 参数。所以这里统一归零，
        // 让「展示用实体」永远以标准的南向姿态渲染，朝向由 CorpseRenderer 的变
        // 换矩阵统一处理。
        created.setDeltaMovement(Vec3.ZERO);
        created.setNoGravity(true);
        created.setYRot(0.0F);
        created.setXRot(0.0F);
        if (created instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setYBodyRot(0.0F);
            mob.setYHeadRot(0.0F);
        } else if (created instanceof LivingEntity living) {
            living.setYBodyRot(0.0F);
            living.setYHeadRot(0.0F);
        }

        this.displayEntity = created;
        return created;
    }

    // ===================== 灵珠 =====================

    public List<ItemStack> getBeads() {
        return beads;
    }

    public void addBead(ItemStack stack) {
        if (!stack.isEmpty()) {
            this.beads.add(stack);
        }
    }

    public int beadCount() {
        int total = 0;
        for (ItemStack stack : this.beads) {
            total += stack.getCount();
        }
        return total;
    }

    public boolean hasBeads() {
        return !this.beads.isEmpty();
    }

    /** 取走全部灵珠并清空内部列表（调用方负责发放给玩家） */
    public List<ItemStack> takeBeads() {
        List<ItemStack> copy = new ArrayList<>(this.beads);
        this.beads.clear();
        return copy;
    }

    /**
     * 服务端用：按设定，一只怪只掉 1 颗灵珠，品质由 rollQuality 决定。
     * 怪物强度只影响品质高低，不影响数量。
     */
    public void fillBeads(LivingEntity dead) {
        ItemStack bead = new ItemStack(ModItems.BEADS.get(randomQuality(dead)).get());

        // 记录来源生物：这是第二部「灵珠种类决定修炼方向」和
        // 第四部「极品 XX 灵珠」分物种定价的依据
        ResourceLocation sourceId = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        SpiritBeadItem.setSource(bead, sourceId == null ? "" : sourceId.toString());

        this.addBead(bead);
    }

    /**
     * 品质随机：权重 = 基础权重 * (强度系数 ^ 品质序号)
     * 强度系数由怪物最大生命值推定，越硬的怪越容易出高品质灵珠。
     * 这是一个怪的实力差体此刻品质而非数量。
     */
    private SpiritQuality randomQuality(LivingEntity dead) {
        double influence = Config.STRENGTH_INFLUENCE.get();
        double hp = dead.getMaxHealth();
        double base = Math.max(0.05D, 1.0D + ((hp - 20.0D) / 40.0D) * influence);

        SpiritQuality[] values = SpiritQuality.values();
        double[] weights = new double[values.length];
        double total = 0.0D;
        for (int i = 0; i < values.length; i++) {
            double w = Math.max(0.0D, values[i].weight() * Math.pow(base, values[i].tier()));
            weights[i] = w;
            total += w;
        }
        if (total <= 0.0D) {
            return SpiritQuality.FAN;
        }

        double roll = this.random.nextDouble() * total;
        double acc = 0.0D;
        for (int i = 0; i < values.length; i++) {
            acc += weights[i];
            if (roll < acc) {
                return values[i];
            }
        }
        return SpiritQuality.FAN;
    }

    // ===================== 逻辑 =====================

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            this.life++;
        }

        // 尸体继承 Entity 而非 LivingEntity，没有内置物理，这里手动施加重力
        if (this.getDeltaMovement().y > -1.0D) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.06D, 0.0D));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.75D, 0.98D, 0.75D));

        if (!this.level().isClientSide() && this.life > Config.CORPSE_LIFETIME_TICKS.get()) {
            expire();
        }
    }

    private void expire() {
        if (Config.DROP_BEADS_TO_GROUND_ON_TIMEOUT.get()) {
            for (ItemStack stack : this.beads) {
                ItemEntity item = new ItemEntity(this.level(), this.getX(), this.getY() + 0.2D, this.getZ(), stack);
                item.setDefaultPickUpDelay();
                this.level().addFreshEntity(item);
            }
        }
        this.beads.clear();
        this.discard();
    }

    // ===================== 基础属性 =====================

    @Override
    public boolean isPickable() {
        // 必须可被射线命中，否则玩家右键不到它
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    /**
     * 末影龙尸体体型巨大，默认的极小命中箱会让玩家点不到、无法采集灵珠。
     * Entity.getBoundingBox() 是 final 不可覆写，这里改为覆写 getDimensions，
     * 让刷新尺寸时生成一个以尸体为中心的大包围盒，确保右键一定能命中。
     */
    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        if (this.dragon) {
            // 宽度 16（半径 8）、高度 12 的大包围盒
            return EntityDimensions.scalable(16.0F, 12.0F);
        }
        return super.getDimensions(pPose);
    }

    // ===================== 存档 =====================

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.beads.clear();
        if (tag.contains(KEY_BEADS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(KEY_BEADS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = ItemStack.of(list.getCompound(i));
                if (!stack.isEmpty()) {
                    this.beads.add(stack);
                }
            }
        }
        if (tag.contains(KEY_MOB_DATA, Tag.TAG_COMPOUND)) {
            this.mobData = tag.getCompound(KEY_MOB_DATA);
        }
        this.life = tag.getInt(KEY_LIFE);
        this.visualYaw = tag.getFloat(KEY_YAW);
        this.dragon = tag.getBoolean(KEY_DRAGON);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ItemStack stack : this.beads) {
            list.add(stack.save(new CompoundTag()));
        }
        tag.put(KEY_BEADS, list);
        tag.put(KEY_MOB_DATA, this.mobData);
        tag.putInt(KEY_LIFE, this.life);
        tag.putFloat(KEY_YAW, this.visualYaw);
        tag.putBoolean(KEY_DRAGON, this.dragon);
    }
}
