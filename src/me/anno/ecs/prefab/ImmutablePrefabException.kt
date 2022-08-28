package me.anno.ecs.prefab

import me.anno.io.files.FileReference

class ImmutablePrefabException(src: FileReference): Exception("Prefab from '$src' cannot be modified, inherit from it!")