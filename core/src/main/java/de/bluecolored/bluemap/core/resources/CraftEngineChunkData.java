package de.bluecolored.bluemap.core.resources;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.mca.PackedIntArrayAccess;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NBTName;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses CraftEngine's per-chunk custom-block data (stored in the chunk PersistentDataContainer under
 * {@code ChunkBukkitValues -> craftengine:chunk_data}) into a position → custom-block-id lookup.
 *
 * <p>CraftEngine serializes custom blocks in a paletted container per 16×16×16 section — exactly the
 * vanilla layout: a {@code palette} of {@code {id, properties}} entries plus a {@code data}
 * {@code long[]} of packed palette indices (see {@code DefaultSectionSerializer}). We replicate that
 * decoding here so BlueMap can overlay the custom model over the vanilla appearance block.</p>
 *
 * <p>Robust by design: any parse failure yields an empty result, so the map simply falls back to the
 * vanilla appearance blocks — never a crash.</p>
 */
public final class CraftEngineChunkData {

    private static final int BLOCKS_PER_SECTION = 4096;
    private static final BlueNBT BLUE_NBT = new BlueNBT();

    /** section-local key: {@code (sectionY << 12) | (y << 8) | (z << 4) | x} — all chunk-local. */
    private final Map<Integer, String> blockIdByPos;

    private CraftEngineChunkData(Map<Integer, String> blockIdByPos) {
        this.blockIdByPos = blockIdByPos;
    }

    /**
     * @return the CraftEngine custom-block id at the given chunk-local coordinates, or {@code null}
     *         if this position is not a CraftEngine custom block.
     */
    public @Nullable String getBlockId(int x, int y, int z) {
        return blockIdByPos.get(key(y, x & 0xF, z & 0xF));
    }

    public boolean isEmpty() {
        return blockIdByPos.isEmpty();
    }

    private static int key(int y, int x, int z) {
        // y may be negative; shift into a stable positive-ordered key.
        return ((y + 2048) << 8) | (z << 4) | x;
    }

    /**
     * Parses the raw {@code craftengine:chunk_data} byte array. Returns {@code null} when there is no
     * usable data (empty array, parse failure, or no custom blocks).
     */
    public static @Nullable CraftEngineChunkData parse(byte @Nullable [] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            ChunkNbt chunk = BLUE_NBT.read(in, ChunkNbt.class);
            if (chunk == null || chunk.sections == null || chunk.sections.length == 0) return null;

            Map<Integer, String> map = new HashMap<>();
            for (SectionNbt section : chunk.sections) {
                if (section == null || section.blockStates == null) continue;
                PaletteEntry[] palette = section.blockStates.palette;
                if (palette == null || palette.length == 0) continue;

                int sectionY = section.y;
                long[] data = section.blockStates.data;

                if (palette.length == 1) {
                    // uniform section — a single custom block filling it (rare but valid)
                    String id = palette[0].id;
                    if (isCustom(id)) fillSection(map, sectionY, id);
                    continue;
                }
                if (data == null || data.length == 0) continue;

                PackedIntArrayAccess blocks = new PackedIntArrayAccess(data, BLOCKS_PER_SECTION);
                for (int i = 0; i < BLOCKS_PER_SECTION; i++) {
                    int id = blocks.get(i);
                    if (id < 0 || id >= palette.length) continue;
                    String blockId = palette[id].id;
                    if (!isCustom(blockId)) continue;
                    int x = i & 0xF;
                    int z = (i >> 4) & 0xF;
                    int y = ((i >> 8) & 0xF) + (sectionY << 4);
                    map.put(key(y, x, z), blockId);
                }
            }
            return map.isEmpty() ? null : new CraftEngineChunkData(map);
        } catch (Throwable t) {
            Logger.global.noFloodDebug("craftengine-chunk-parse", "Failed to parse CraftEngine chunk data: " + t);
            return null;
        }
    }

    private static boolean isCustom(@Nullable String id) {
        // CraftEngine ids are namespaced (e.g. "default:chair"); empty/air entries are skipped.
        return id != null && !id.isEmpty() && !id.equals("minecraft:air");
    }

    private static void fillSection(Map<Integer, String> map, int sectionY, String id) {
        for (int i = 0; i < BLOCKS_PER_SECTION; i++) {
            int x = i & 0xF;
            int z = (i >> 4) & 0xF;
            int y = ((i >> 8) & 0xF) + (sectionY << 4);
            map.put(key(y, x, z), id);
        }
    }

    // ----- NBT model (mirrors DefaultChunkSerializer / DefaultSectionSerializer) -----

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class ChunkNbt {
        @NBTName("sections")
        private SectionNbt @Nullable [] sections = null;
    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class SectionNbt {
        @NBTName("Y")
        private int y = 0;
        @NBTName("block_states")
        private BlockStatesNbt blockStates = null;
    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class BlockStatesNbt {
        @NBTName("palette")
        private PaletteEntry[] palette = null;
        @NBTName("data")
        private long[] data = null;
    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class PaletteEntry {
        @NBTName("id")
        private String id = null;
    }
}
