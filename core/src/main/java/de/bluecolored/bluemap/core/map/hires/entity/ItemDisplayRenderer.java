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
package de.bluecolored.bluemap.core.map.hires.entity;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.CraftEngineRegistry;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.entitystate.Part;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.util.math.VectorM2f;
import de.bluecolored.bluemap.core.util.math.VectorM3f;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import de.bluecolored.bluemap.core.world.mca.entity.ItemDisplayEntity;
import lombok.Getter;

import java.util.function.Function;

@SuppressWarnings("DuplicatedCode")
public class ItemDisplayRenderer implements EntityRenderer {
    private static final float SCALE = 1f / 16f;

    @Getter
    private final ResourcePack resourcePack;
    @Getter
    private final Function<Key, Model> modelProvider;
    @Getter
    private final TextureGallery textureGallery;
    @Getter
    private final RenderSettings renderSettings;

    private final VectorM3f[] corners = new VectorM3f[8];
    private final VectorM2f[] rawUvs = new VectorM2f[4];
    private final VectorM2f[] uvs = new VectorM2f[4];
    private final Color tintColor = new Color();

    private Model modelResource;
    private TileModelView tileModel;
    private int sunLight, blockLight;

    public ItemDisplayRenderer(ResourcePack resourcePack, TextureGallery textureGallery,
            RenderSettings renderSettings) {
        this.resourcePack = resourcePack;
        this.modelProvider = resourcePack.getModels()::get;
        this.textureGallery = textureGallery;
        this.renderSettings = renderSettings;

        for (int i = 0; i < corners.length; i++)
            corners[i] = new VectorM3f(0, 0, 0);
        for (int i = 0; i < rawUvs.length; i++)
            rawUvs[i] = new VectorM2f(0, 0);
    }

    @Override
    public void render(Entity entity, BlockNeighborhood block, Part part, TileModelView tileModel) {
        if (!(entity instanceof ItemDisplayEntity itemDisplay)) {
            return;
        }

        String craftEngineFurnitureId = itemDisplay.getCraftEngineFurnitureId();
        ResourcePath<Model> modelPath = null;

        if (craftEngineFurnitureId != null) {
            modelPath = CraftEngineRegistry.getModel(craftEngineFurnitureId);

            if (modelPath == null) {
                modelPath = resolveCraftEngineModelPath(craftEngineFurnitureId);
            }
        }

        if (modelPath == null) {
            ItemDisplayEntity.ItemData item = itemDisplay.getItem();
            if (item != null) {
                 modelPath = resolveModelPath(item);
            }
        }

        if (modelPath == null) {
            return;
        }

        Model model = modelPath.getResource(modelProvider);
        if (model == null) {
            return;
        }

        this.modelResource = model;
        this.tileModel = tileModel;

        LightData blockLightData = block.getLightData();
        this.sunLight = blockLightData.getSkyLight();
        this.blockLight = blockLightData.getBlockLight();

        if (block.isRemoveIfCave() &&
                (renderSettings.isCaveDetectionUsesBlockLight() ? Math.max(blockLight, sunLight) : sunLight) == 0) {
            return;
        }

        int modelStart = this.tileModel.getStart();

        Element[] elements = modelResource.getElements();
        if (elements != null) {
            for (Element element : elements) {
                buildModelElementResource(element, this.tileModel.initialize());
            }
        }

        this.tileModel.initialize(modelStart);

        ItemDisplayEntity.Transformation transformation = itemDisplay.getTransformation();
        if (transformation != null) {
            applyTransformation(transformation);
        }
    }

    private ResourcePath<Model> resolveCraftEngineModelPath(String furnitureId) {
        String namespace = "minecraft";
        String path = furnitureId;

        if (furnitureId.contains(":")) {
            String[] parts = furnitureId.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        }

        ResourcePath<Model> modelPath = new ResourcePath<>(namespace, "item/custom/" + path);
        if (modelPath.getResource(modelProvider) != null) return modelPath;

        modelPath = new ResourcePath<>("minecraft", "item/custom/" + path);
        if (modelPath.getResource(modelProvider) != null) return modelPath;

        modelPath = new ResourcePath<>(namespace, "item/" + path);
        if (modelPath.getResource(modelProvider) != null) return modelPath;

        modelPath = new ResourcePath<>(namespace, "block/custom/" + path);
        if (modelPath.getResource(modelProvider) != null) return modelPath;

        return null;
    }

    private ResourcePath<Model> resolveModelPath(ItemDisplayEntity.ItemData item) {
        ItemDisplayEntity.ItemComponents components = item.getComponents();

        if (components != null && components.getItemModel() != null) {
            return new ResourcePath<>(components.getItemModel());
        }

        String itemId = item.getId();
        if (itemId != null) {
            String namespace = "minecraft";
            String path = itemId;
            if (itemId.contains(":")) {
                String[] parts = itemId.split(":", 2);
                namespace = parts[0];
                path = parts[1];
            }
            return new ResourcePath<>(namespace, "item/" + path);
        }

        return null;
    }

    private void applyTransformation(ItemDisplayEntity.Transformation transformation) {
        float[] translation = transformation.getTranslationOrDefault();
        float[] scale = transformation.getScaleOrDefault();

        MatrixM4f matrix = new MatrixM4f()
                .translate(translation[0], translation[1], translation[2])
                .scale(scale[0], scale[1], scale[2]);

        float[] leftRot = transformation.getLeftRotation();
        if (leftRot != null && leftRot.length >= 4) {
            applyQuaternionRotation(matrix, leftRot);
        }

        float[] rightRot = transformation.getRightRotation();
        if (rightRot != null && rightRot.length >= 4) {
            applyQuaternionRotation(matrix, rightRot);
        }

        tileModel.transform(matrix);
    }

    private void applyQuaternionRotation(MatrixM4f matrix, float[] quaternion) {
        float x = quaternion[0], y = quaternion[1], z = quaternion[2], w = quaternion[3];
        float yaw = (float) Math.atan2(2.0 * (w * y + x * z), 1.0 - 2.0 * (y * y + x * x));
        float pitch = (float) Math.asin(Math.max(-1.0, Math.min(1.0, 2.0 * (w * x - z * y))));
        float roll = (float) Math.atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (x * x + z * z));
        matrix.rotateYXZ((float) Math.toDegrees(pitch), (float) Math.toDegrees(yaw), (float) Math.toDegrees(roll));
    }

    private final MatrixM4f modelElementTransform = new MatrixM4f();

    private void buildModelElementResource(Element element, TileModelView blockModel) {
        Vector3f from = element.getFrom();
        Vector3f to = element.getTo();

        float minX = from.getX(), minY = from.getY(), minZ = from.getZ();
        float maxX = to.getX(), maxY = to.getY(), maxZ = to.getZ();

        VectorM3f[] c = corners;
        c[0].set(minX, minY, minZ);
        c[1].set(minX, minY, maxZ);
        c[2].set(maxX, minY, minZ);
        c[3].set(maxX, minY, maxZ);
        c[4].set(minX, maxY, minZ);
        c[5].set(minX, maxY, maxZ);
        c[6].set(maxX, maxY, minZ);
        c[7].set(maxX, maxY, maxZ);

        int modelStart = blockModel.getStart();
        createElementFace(element, Direction.DOWN, c[0], c[2], c[3], c[1]);
        createElementFace(element, Direction.UP, c[5], c[7], c[6], c[4]);
        createElementFace(element, Direction.NORTH, c[2], c[0], c[4], c[6]);
        createElementFace(element, Direction.SOUTH, c[1], c[3], c[7], c[5]);
        createElementFace(element, Direction.WEST, c[0], c[1], c[5], c[4]);
        createElementFace(element, Direction.EAST, c[3], c[2], c[6], c[7]);
        blockModel.initialize(modelStart);

        blockModel.transform(modelElementTransform
                .copy(element.getRotation().getMatrix())
                .scale(SCALE, SCALE, SCALE));
    }

    private void createElementFace(Element element, Direction faceDir, VectorM3f c0, VectorM3f c1, VectorM3f c2, VectorM3f c3) {
        Face face = element.getFaces().get(faceDir);
        if (face == null) return;

        tileModel.initialize();
        tileModel.add(2);

        TileModel tileModel = this.tileModel.getTileModel();
        int face1 = this.tileModel.getStart();
        int face2 = face1 + 1;

        tileModel.setPositions(face1, c0.x, c0.y, c0.z, c1.x, c1.y, c1.z, c2.x, c2.y, c2.z);
        tileModel.setPositions(face2, c0.x, c0.y, c0.z, c2.x, c2.y, c2.z, c3.x, c3.y, c3.z);

        ResourcePath<Texture> texturePath = face.getTexture().getTexturePath(modelResource.getTextures()::get);
        int textureId = textureGallery.get(texturePath);
        tileModel.setMaterialIndex(face1, textureId);
        tileModel.setMaterialIndex(face2, textureId);

        Vector4f uvRaw = face.getUv();
        float uvx = uvRaw.getX() / 16f, uvy = uvRaw.getY() / 16f, uvz = uvRaw.getZ() / 16f, uvw = uvRaw.getW() / 16f;

        rawUvs[0].set(uvx, uvw);
        rawUvs[1].set(uvz, uvw);
        rawUvs[2].set(uvz, uvy);
        rawUvs[3].set(uvx, uvy);

        int rotationSteps = Math.floorDiv(face.getRotation(), 90) % 4;
        if (rotationSteps < 0) rotationSteps += 4;
        for (int i = 0; i < 4; i++) uvs[i] = rawUvs[(rotationSteps + i) % 4];

        tileModel.setUvs(face1, uvs[0].x, uvs[0].y, uvs[1].x, uvs[1].y, uvs[2].x, uvs[2].y);
        tileModel.setUvs(face2, uvs[0].x, uvs[0].y, uvs[2].x, uvs[2].y, uvs[3].x, uvs[3].y);

        if (face.getTintindex() >= 0) {
            tintColor.set(1f, 1f, 1f, 1f, true);
            tileModel.setColor(face1, tintColor.r, tintColor.g, tintColor.b);
            tileModel.setColor(face2, tintColor.r, tintColor.g, tintColor.b);
        } else {
            tileModel.setColor(face1, 1f, 1f, 1f);
            tileModel.setColor(face2, 1f, 1f, 1f);
        }

        int emissiveBlockLight = Math.max(blockLight, element.getLightEmission());
        tileModel.setBlocklight(face1, emissiveBlockLight);
        tileModel.setBlocklight(face2, emissiveBlockLight);

        tileModel.setSunlight(face1, sunLight);
        tileModel.setSunlight(face2, sunLight);

        tileModel.setAOs(face1, 1f, 1f, 1f);
        tileModel.setAOs(face2, 1f, 1f, 1f);
    }
}
