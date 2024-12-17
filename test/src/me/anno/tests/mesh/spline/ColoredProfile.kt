package me.anno.tests.mesh.spline

import me.anno.ecs.components.mesh.spline.SplineProfile
import me.anno.io.files.FileReference

data class ColoredProfile(val profile: SplineProfile, val materialRef: FileReference)
