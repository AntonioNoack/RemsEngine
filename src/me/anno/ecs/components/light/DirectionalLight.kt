package me.anno.ecs.components.light

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes.drawArrowZ
import me.anno.engine.gui.LineShapes.drawBox
import me.anno.gpu.pipeline.Pipeline
import me.anno.mesh.Shapes
import me.anno.utils.types.AABBs.all
import me.anno.utils.types.AABBs.transformUnion
import me.anno.utils.types.Matrices.set2
import org.joml.*

class DirectionalLight : LightComponent(LightType.DIRECTIONAL) {

    /**
     * typically a directional light will be the sun;
     * it's influence should be over the whole scene, while its shadows may not
     *
     * with cutoff > 0, it is cutoff, as if it was a plane light
     * */
    @Range(0.0, 1.0)
    var cutoff = 0f

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        if (cutoff <= 0f) {
            aabb.all()
        } else {
            val mesh = getLightPrimitive()
            mesh.ensureBuffer()
            mesh.aabb.transformUnion(globalTransform, aabb)
        }
        return true
    }

    override fun updateShadowMap(
        cascadeScale: Double,
        worldScale: Double,
        cameraMatrix: Matrix4f,
        drawTransform: Matrix4x3d,
        pipeline: Pipeline,
        resolution: Int,
        position: Vector3d,
        rotation: Quaterniond
    ) {
        cameraMatrix.set2(drawTransform).invert()
        cameraMatrix.setTranslation(0f, 0f, 0f)
        val sx = (1.0 / (cascadeScale * worldScale)).toFloat()
        val sz = (1.0 / (worldScale)).toFloat()
        // z must be mapped from [-1,1] to [0,1]
        // additionally it must be scaled to match the world size
        cameraMatrix.scaleLocal(sx, sx, sz * 0.5f)
        cameraMatrix.m32(0.5f)
        pipeline.frustum.defineOrthographic(drawTransform, resolution, position, rotation)
    }

    override fun getLightPrimitive(): Mesh = Shapes.cube

    // v0 is not used
    override fun getShaderV1(): Float = shadowMapPower.toFloat()
    override fun getShaderV2(): Float = if (cutoff > 0f) 1f / cutoff else 0f

    override fun drawShape() {
        drawBox(entity)
        drawArrowZ(entity, +1.0, -1.0)
    }

    override fun clone(): DirectionalLight {
        val clone = DirectionalLight()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as DirectionalLight
        clone.cutoff = cutoff
    }

    override val className: String = "DirectionalLight"

    companion object {

        private fun getCutoff(cutoffContinue: String?): String {
            return if (cutoffContinue != null) {
                "" +
                        "#define invCutoff data2.a\n" +
                        // box cutoff: max(max(abs(dir.x),abs(dir.y)),abs(dir.z))
                        // sphere cutoff:
                        "if(invCutoff > 0.0){\n" +
                        "   float cut = min(invCutoff * (1.0 - dot(dir,dir)), 1.0);\n" +
                        "   if(cut <= 0.0) { $cutoffContinue; }\n" +
                        "   lightColor *= cut;\n" +
                        "}\n"
            } else ""
        }

        fun getShaderCode(cutoffContinue: String?, withShadows: Boolean): String {
            return "" +
                    getCutoff(cutoffContinue) +
                    "NdotL = localNormal.z;\n" + // dot(lightDirWS, globalNormal) = dot(lightDirLS, localNormal)
                    // inv(W->L) * vec4(0,0,1,0) =
                    // transpose(m3x3(W->L)) * vec3(0.0,0.0,1.0)
                    "lightDirWS = normalize(vec3(WStoLightSpace[0][2],WStoLightSpace[1][2],WStoLightSpace[2][2]));\n" +
                    (if (withShadows) "" +
                            "if(shadowMapIdx0 < shadowMapIdx1){\n" +
                            // when we are close to the edge, we blend in
                            "   float edgeFactor = min(20.0*(1.0-max(abs(dir.x),abs(dir.y))),1.0);\n" +
                            "   if(edgeFactor > 0.0){\n" +
                            "       #define shadowMapPower data2.b\n" +
                            "       float invShadowMapPower = 1.0/shadowMapPower;\n" +
                            "       vec2 shadowDir = dir.xy;\n" +
                            "       vec2 nextDir = shadowDir * shadowMapPower;\n" +
                            // find the best shadow map
                            // blend between the two best shadow maps, if close to the border?
                            // no, the results are already very good this way :)
                            // at least at the moment, the seams are not obvious
                            "       while(abs(nextDir.x)<1.0 && abs(nextDir.y)<1.0 && shadowMapIdx0+1<shadowMapIdx1){\n" +
                            "           shadowMapIdx0++;\n" +
                            "           shadowDir = nextDir;\n" +
                            "           nextDir *= shadowMapPower;\n" +
                            "       }\n" +
                            "       float depthFromShader = dir.z*.5+.5;\n" +
                            "       if(depthFromShader > 0.0){\n" +
                            // do the shadow map function and compare
                            "           float depthFromTex = texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy, depthFromShader);\n" +
                            // todo this will become proportional to the distance to the shadow throwing surface
                            // "           float coc = 1.0 / texture_array_size_shadowMapPlanar(shadowMapIdx0, 0).x;\n" +
                            // "           float val = texture_array_shadowMapPlanar(shadowMapIdx0, shadowDir.xy).r;\n" +
                            // "           diffuseColor = vec3(val,val,dir.z);\n" + // nice for debugging
                            "           " +
                            "           lightColor *= 1.0 - edgeFactor * depthFromTex;\n" +
                           /* "           lightColor *= 1.0 - edgeFactor * (" +
                            "texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy-vec2(coc,0), depthFromShader) +" +
                            "texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy-vec2(0,coc), depthFromShader) +" +
                            "texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy+vec2(coc,0), depthFromShader) +" +
                            "texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy+vec2(0,coc), depthFromShader) +" +
                            "texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy+vec2(+coc,+coc), depthFromShader) +" +
                            "texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy+vec2(+coc,-coc), depthFromShader) +" +
                            "texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy+vec2(-coc,+coc), depthFromShader) +" +
                            "texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy+vec2(-coc,-coc), depthFromShader) +" +
                            "texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy, depthFromShader)" +
                            ") * 0.111111;\n" +*/
                            "       }\n" +
                            "   }\n" +
                            "}\n"
                    else "") +
                    "effectiveDiffuse = lightColor;\n" +
                    "effectiveSpecular = lightColor;\n"
        }


    }

}