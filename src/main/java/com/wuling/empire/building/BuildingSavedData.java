package com.wuling.empire.building;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 「哪些方块是玩家亲手放的」台账，<b>每个维度一份</b>，落盘在
 * {@code <存档>/data/wuling_building.dat}。
 *
 * <h3>为什么非要存这份台账</h3>
 * 建筑系统要区分「玩家搭的」和「自然生成的」—— 用户口径是
 * 「自然产生的地形或建筑除外」。但原版方块没有通用 NBT，没法在方块自己身上做记号，
 * 所以只能在世界存档里另记一份位置清单。
 *
 * <p>台账要持久化（而不是只在内存里），是因为「持续判定」：方块放好、过了 2 秒判定之后，
 * 它下方被挖掉时仍然要掉。服务器重启后台账还在，这条规则才不会断。
 *
 * <h3>内存占用</h3>
 * 用 {@link LongSet} 存 {@link BlockPos#asLong()} 打包值，一个位置 8 字节 + 哈希表开销。
 * 十万块建筑约 2~3 MB，可接受。台账只在「玩家放置」和「方块消失」两个时机写入，
 * 不参与逐 tick 扫描。
 */
public class BuildingSavedData extends SavedData {

    private static final String DATA_NAME = "wuling_building";
    private static final String TAG_MANAGED = "Managed";

    /** 玩家放置过的方块位置（{@link BlockPos#asLong()} 打包） */
    private final LongSet managed = new LongOpenHashSet();

    public static BuildingSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                BuildingSavedData::load, BuildingSavedData::new, DATA_NAME);
    }

    public static BuildingSavedData load(CompoundTag tag) {
        BuildingSavedData data = new BuildingSavedData();
        for (long packed : tag.getLongArray(TAG_MANAGED)) {
            data.managed.add(packed);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray(TAG_MANAGED, managed.toLongArray());
        return tag;
    }

    /** 这个位置是不是玩家放的方块 */
    public boolean isManaged(BlockPos pos) {
        return managed.contains(pos.asLong());
    }

    /** 登记一个玩家放置的方块 */
    public void manage(BlockPos pos) {
        if (managed.add(pos.asLong())) {
            setDirty();
        }
    }

    /** 划掉一个已经不在的位置（被挖 / 被炸 / 坠落移除） */
    public void unmanage(BlockPos pos) {
        if (managed.remove(pos.asLong())) {
            setDirty();
        }
    }

    /** 已登记的方块数（调试 / 排查用） */
    public int size() {
        return managed.size();
    }
}
