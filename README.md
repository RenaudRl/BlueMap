# BlueMap — BTC Fork

Fork de **[BlueMap](https://github.com/BlueMap-Minecraft/BlueMap)** (BlueColored) — carte web dynamique — adapté au serveur **BornToCraft** — Paper / Folia **26.2**.

## Nos ajouts / correctifs BTC
- **Support CraftEngine — furniture** : les meubles/furniture custom (entités ItemDisplay) sont rendus sur la carte via `CraftEngineRegistry` (`itemDisplay.getCraftEngineFurnitureId()` → modèle).
- _(à venir)_ **Blocs custom CraftEngine** : les blocs rendus par block-entity/display (stockés dans le PDC de chunk `craftengine:chunk_data`) ne sont pas encore remappés — chantier de rendu à part (nécessite test serveur).

## Build
```bash
./gradlew :BlueMapPaper:build  # jar dans implementations/paper/build/libs/
```

---
Base upstream : `BlueMap-Minecraft/BlueMap` · cible Minecraft **26.2**
