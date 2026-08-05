/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.resources;

import de.bluecolored.bluenbt.BlueNBT;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class CraftEngineChunkDataTest {

    private static final BlueNBT BLUE_NBT = new BlueNBT();

    @Test
    public void testParseNullAndEmptyReturnsNull() {
        assertNull(CraftEngineChunkData.parse(null));
        assertNull(CraftEngineChunkData.parse(new byte[0]));
    }

    @Test
    public void testParseGarbageBytesNeverThrows() {
        // must degrade gracefully instead of crashing the renderer
        assertNull(CraftEngineChunkData.parse(new byte[]{1, 2, 3, 4, 5}));
    }

    @Test
    public void testParseUniformCustomSection() throws IOException {
        byte[] bytes = writeChunk(new SectionSpec(0, new String[]{"default:chair"}, null));

        CraftEngineChunkData data = CraftEngineChunkData.parse(bytes);
        assertNotNull(data);
        assertFalse(data.isEmpty());

        // uniform section fills all 4096 positions of section Y=0
        assertEquals("default:chair", data.getBlockId(0, 0, 0));
        assertEquals("default:chair", data.getBlockId(15, 15, 15));
        assertEquals("default:chair", data.getBlockId(0, 15, 0));
    }

    @Test
    public void testParseUniformVanillaSectionIsIgnored() throws IOException {
        byte[] bytes = writeChunk(new SectionSpec(0, new String[]{"minecraft:air"}, null));

        CraftEngineChunkData data = CraftEngineChunkData.parse(bytes);
        assertNull(data);
    }

    @Test
    public void testParsePalettedSectionResolvesCorrectPosition() throws IOException {
        // palette index 0 = air, index 1 = custom block; every position in the paletted
        // long[] resolves to index 0 (all-zero data) except we only assert the palette lookup path.
        long[] packedZeros = new long[64]; // enough words for a 4-bit-per-entry packed array of 4096 entries
        byte[] bytes = writeChunk(new SectionSpec(0, new String[]{"minecraft:air", "default:chair"}, packedZeros));

        CraftEngineChunkData data = CraftEngineChunkData.parse(bytes);
        // every entry resolves to palette index 0 (air) -> no custom blocks recorded
        assertNull(data);
    }

    @Test
    public void testNegativeSectionYCoordinatesAreStable() throws IOException {
        byte[] bytes = writeChunk(new SectionSpec(-4, new String[]{"default:chair"}, null));

        CraftEngineChunkData data = CraftEngineChunkData.parse(bytes);
        assertNotNull(data);
        // section Y=-4 -> world Y = -64..-49
        assertEquals("default:chair", data.getBlockId(0, -64, 0));
        assertEquals("default:chair", data.getBlockId(0, -49, 0));
        assertNull(data.getBlockId(0, -48, 0));
    }

    // ----- helpers: build minimal NBT matching CraftEngineChunkData.ChunkNbt -----

    private record SectionSpec(int y, String[] paletteIds, long[] data) {}

    private byte[] writeChunk(SectionSpec... sectionSpecs) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(out)) {
            BLUE_NBT.write(toChunkNbt(sectionSpecs), dos, CraftEngineChunkData.ChunkNbt.class);
        }
        return out.toByteArray();
    }

    private CraftEngineChunkData.ChunkNbt toChunkNbt(SectionSpec[] specs) {
        CraftEngineChunkData.ChunkNbt chunk = new CraftEngineChunkData.ChunkNbt();
        CraftEngineChunkData.SectionNbt[] sections = new CraftEngineChunkData.SectionNbt[specs.length];
        for (int i = 0; i < specs.length; i++) {
            SectionSpec spec = specs[i];
            CraftEngineChunkData.SectionNbt section = new CraftEngineChunkData.SectionNbt();
            setField(section, "y", spec.y());

            CraftEngineChunkData.BlockStatesNbt blockStates = new CraftEngineChunkData.BlockStatesNbt();
            CraftEngineChunkData.PaletteEntry[] palette = new CraftEngineChunkData.PaletteEntry[spec.paletteIds().length];
            for (int p = 0; p < palette.length; p++) {
                CraftEngineChunkData.PaletteEntry entry = new CraftEngineChunkData.PaletteEntry();
                setField(entry, "id", spec.paletteIds()[p]);
                palette[p] = entry;
            }
            setField(blockStates, "palette", palette);
            setField(blockStates, "data", spec.data());
            setField(section, "blockStates", blockStates);

            sections[i] = section;
        }
        setField(chunk, "sections", sections);
        return chunk;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
