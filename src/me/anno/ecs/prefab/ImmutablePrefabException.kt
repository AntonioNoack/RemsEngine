package me.anno.ecs.prefab

import me.anno.io.files.FileReference
import java.lang.Exception

class ImmutablePrefabException(src: FileReference): Exception("Prefab from '$src' cannot be modified, inherit from it!")