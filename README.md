# BlueMap — BTC Fork

Fork de **[BlueMap](https://github.com/BlueMap-Minecraft/BlueMap)** (BlueColored) — carte web dynamique — adapté au serveur **BornToCraft** — Paper / Folia **26.2**.

## Nos ajouts / correctifs BTC
- **Support CraftEngine — furniture** : les meubles/furniture custom (entités ItemDisplay) sont rendus sur la carte via `CraftEngineRegistry` (`itemDisplay.getCraftEngineFurnitureId()` → modèle).
- **Support CraftEngine — blocs custom** : les blocs custom (stockés dans le PDC de chunk `craftengine:chunk_data`, parsés par `CraftEngineChunkData`) sont désormais rendus avec leur modèle CraftEngine au lieu de l'apparence vanilla du bloc-support (`BlockStateModelRenderer` → `CraftEngineRegistry.getModel(id)`). Testé unitairement (parsing NBT) ; _validation sur données serveur réelles encore à faire_.
- **Intégration BTC-CORE (MSPT)** : évaluée, non nécessaire — le rendu de blocs (y compris la lecture PDC `craftengine:chunk_data`) tourne déjà dans le pool de rendu asynchrone dédié de BlueMap (hors thread de tick/région), donc pas de contention à throttler.

## Build
```bash
./gradlew :BlueMapPaper:build  # jar dans implementations/paper/build/libs/
```

---
Base upstream : `BlueMap-Minecraft/BlueMap` · cible Minecraft **26.2**
