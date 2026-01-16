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

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Registry for CraftEngine furniture and custom blocks.
 * Parses CraftEngine YAML configuration files to map furniture IDs to model paths.
 */
public class CraftEngineRegistry {

    private static final Map<String, ResourcePath<Model>> MODEL_MAPPING = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    /**
     * Loads CraftEngine configurations from the specified root directory.
     * Typically "plugins/CraftEngine/packs".
     */
    public static synchronized void load(Path pluginsDir) {
        if (initialized || pluginsDir == null || !Files.exists(pluginsDir)) return;
        
        Path craftEngineDir = pluginsDir.resolve("CraftEngine/packs");
        if (!Files.exists(craftEngineDir)) {
             // Fallback to simpler path if structure differs
             craftEngineDir = pluginsDir.resolve("CraftEngine");
        }

        if (!Files.exists(craftEngineDir)) {
             Logger.global.logDebug("CraftEngine directory not found at: " + craftEngineDir);
             return;
        }

        Logger.global.logInfo("Loading CraftEngine configurations from: " + craftEngineDir);

        try (Stream<Path> stream = Files.walk(craftEngineDir)) {
            stream.filter(path -> path.toString().endsWith(".yml"))
                  .forEach(CraftEngineRegistry::parseFile);
        } catch (IOException e) {
            Logger.global.logError("Failed to walk CraftEngine directories", e);
        }
        
        initialized = true;
    }

    private static void parseFile(Path file) {
        try {
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(file)
                    .build();
            ConfigurationNode root = loader.load();

            // CraftEngine 3.x / 4.x structure often puts items/blocks under "items" or "blocks"
            // We are looking for "model.path" or "items.<id>.model.path"
            
            // Check "items" section
            if (!root.node("items").virtual()) {
                parseSection(root.node("items"));
            }
            
            // Check "blocks" section
            if (!root.node("blocks").virtual()) {
                parseSection(root.node("blocks"));
            }

        } catch (ConfigurateException e) {
            Logger.global.logDebug("Failed to parse CraftEngine file: " + file);
        }
    }

    private static void parseSection(ConfigurationNode section) {
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : section.childrenMap().entrySet()) {
            String id = entry.getKey().toString();
            ConfigurationNode node = entry.getValue();
            
            // Look for model path
            // Structure: model -> path: minecraft:block/custom/xxx
            ConfigurationNode modelNode = node.node("model");
            if (!modelNode.virtual()) {
                String path = modelNode.node("path").getString();
                if (path != null) {
                    register(id, path);
                    continue; // Found it
                }
            }
        }
    }

    private static void register(String id, String modelPath) {
        // ID format: default:chair or craftengine:chair
        // Model path: minecraft:block/custom/chair
        
        ResourcePath<Model> resourcePath = new ResourcePath<>(modelPath);
        MODEL_MAPPING.put(id, resourcePath);
        Logger.global.logDebug("Registered CraftEngine model: " + id + " -> " + modelPath);
    }
    
    public static ResourcePath<Model> getModel(String furnitureId) {
        return MODEL_MAPPING.get(furnitureId);
    }
}
