package me.anno.ecs.components.cache

import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.ecs.components.mesh.Material

object MaterialCache: PrefabByFileCache<Material>(Material::class)