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
package de.bluecolored.bluemap.core.world.mca.entity;

import de.bluecolored.bluenbt.NBTName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuppressWarnings("FieldMayBeFinal")
public class ItemDisplayEntity extends MCAEntity {

    @NBTName("item")
    private ItemData item;

    @NBTName("BukkitValues")
    private java.util.Map<String, Object> bukkitValues;

    @NBTName("transformation")
    private Transformation transformation;

    public String getCraftEngineFurnitureId() {
        if (bukkitValues == null)
            return null;
        Object id = bukkitValues.get("craftengine:furniture_id");
        return id != null ? id.toString() : null;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    public static class ItemData {
        @NBTName("id")
        private String id;

        @NBTName("components")
        private ItemComponents components;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    public static class ItemComponents {
        @NBTName("minecraft:item_model")
        private String itemModel;
        
        @NBTName("minecraft:custom_model_data")
        private CustomModelData customModelData;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    public static class CustomModelData {
        @NBTName("strings")
        private java.util.List<String> strings;
        
        @NBTName("floats")
        private java.util.List<Float> floats;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    public static class Transformation {
        @NBTName("translation")
        private float[] translation;
        
        @NBTName("scale")
        private float[] scale;
        
        @NBTName("left_rotation")
        private float[] leftRotation;
        
        @NBTName("right_rotation")
        private float[] rightRotation;

        public float[] getTranslationOrDefault() {
            return translation != null ? translation : new float[]{0f, 0f, 0f};
        }

        public float[] getScaleOrDefault() {
            return scale != null ? scale : new float[]{1f, 1f, 1f};
        }
    }
}
