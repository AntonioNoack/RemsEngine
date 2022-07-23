package me.anno.ecs.components.mesh.spline

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import org.joml.Vector3d
import kotlin.math.atan2

class SplineCrossing : ProceduralMesh() {

    var profile = 0

    // todo crossing area-plane calculation

    // todo consists of the main area, and then, given by the profile,
    // todo outer paths from edge to edge
    // todo these outer paths probably just could be done using SplineMeshes :)
    // todo *half ones

    var autoSort = false

    override fun generateMesh(mesh: Mesh) {
        var streets = entity!!.children.mapNotNull { it.getComponent(SplineControlPoint::class) }
        when (streets.size) {
            0, 1 -> {
                // todo if streets has size 0 or 1, generate an end piece
            }
            2 -> {
                // todo just connect them normally...
                // todo this would be a spline mesh task...
            }
            else -> {
                /*val center = Vector3d()
                val tmp = Vector3d()
                for (street in streets) {
                    center.add(street.localToParentPos(tmp))
                    center.add(street.getP1(tmp))
                }
                center.div(streets.size * 2.0)
                // auto sort by angle?
                if (autoSort) {
                    streets = streets.sortedBy {
                        it.localToParentPos(tmp)
                        atan2(tmp.y, tmp.x)
                    }
                }*/

            }
        }
        TODO("Not yet implemented")
    }

    override fun clone(): ProceduralMesh {
        val clone = SplineCrossing()
        copy(clone)
        return clone
    }

}