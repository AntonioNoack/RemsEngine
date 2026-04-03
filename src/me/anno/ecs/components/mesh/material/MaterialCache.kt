package me.anno.ecs.components.mesh.material

import me.anno.ecs.prefab.PrefabByFileCache

object MaterialCache : PrefabByFileCache<BaseMaterial>(BaseMaterial::class, "Material")