package me.anno.ecs.components.light

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes.drawArrowZ
import me.anno.engine.ui.LineShapes.drawBox
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.ShaderLib
import me.anno.mesh.Shapes
import org.joml.AABBd
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

class DirectionalLight : LightComponent(LightType.DIRECTIONAL) {

    /**
     * typically a directional light will be the sun;
     * its influence should be over the whole scene, while its shadows may not
     *
     * with cutoff != 0, it is cut off, as if it was a plane light
     * */
    @Range(-1.0, 1.0)
    var cutoff = 0f

    @Suppress("unused")
    val hasCutoff: Boolean get() = cutoff != 0f

    // todo button to auto-position around scene
    //  (translation, scale)

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        if (cutoff == 0f) dstUnion.all()
        else super.fillSpace(globalTransform, dstUnion)
    }

    override fun updateShadowMap(
        cascadeScale: Float,
        dstCameraMatrix: Matrix4f,
        dstCameraPosition: Vector3d,
        cameraRotation: Quaternionf,
        cameraDirection: Vector3f,
        drawTransform: Matrix4x3,
        pipeline: Pipeline,
        resolution: Int
    ) {

        // todo allow to set the high-resolution point non-centered
        //  (we need less pixels behind the player, and more in front, so it makes sense to prioritize the front)

        // cascade style must only influence xy, not z
        dstCameraMatrix.set(drawTransform).invert()
        dstCameraMatrix.setTranslation(0f, 0f, 0f)
        val sx = 1.0 / cascadeScale
        val sz = 1.0

        // z must be mapped from [-1,1] to [0,1]
        // additionally it must be scaled to match the world size
        dstCameraMatrix.scaleLocal(sx.toFloat(), sx.toFloat(), (sz * 0.5).toFloat())
        dstCameraMatrix.m32 = sx.toFloat() // w
        dstCameraMatrix.determineProperties() // after writing a field, we need to recalculate them

        // is this correct if cascadeScale != 1.0? should be
        pipeline.frustum.defineOrthographic(
            drawTransform, resolution,
            dstCameraPosition, cameraRotation
        )

        // offset camera position accordingly
        val factor = (2f / cascadeScale - 1f)
        dstCameraPosition.sub(
            cameraDirection.x * factor,
            cameraDirection.y * factor,
            cameraDirection.z * factor // todo this might be wrong... but idk how much :/
        )
    }

    override fun getLightPrimitive(): Mesh = Shapes.cube11Smooth

    override fun getShaderV1(): Float = shadowMapPower
    override fun getShaderV2(): Float = if (cutoff == 0f) 0f else 1f / cutoff
    override fun getShaderV3(): Float {
        val transform = transform ?: return 0.5f
        return 1f - 0.5f / transform.getGlobalScaleZ()
    }

    override fun drawShape(pipeline: Pipeline) {
        drawBox(entity)
        drawArrowZ(entity, +1.0, -1.0)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is DirectionalLight) return
        dst.cutoff = cutoff
    }

    companion object {
        fun getShaderCode(cutoffContinue: String?, withShadows: Boolean): String {
            return "" +
                    (if (cutoffContinue != null) {
                        "" +
                                "float invCutoff = shaderV2;\n" +
                                // exactly 0.0 is impossible, if denormalized numbers are disabled (WebGL) ->
                                // set a small threshold instead
                                "if(invCutoff > 1e-9){\n" +
                                "   float cut = min(invCutoff * (1.0 - dot(lightPos,lightPos)), 1.0);\n" +
                                "   if(cut <= 0.0) { $cutoffContinue; }\n" +
                                "   lightColor *= cut;\n" +
                                "} else if(invCutoff < -1e-9){\n" +
                                "   float cut = min(-invCutoff * (1.0 - max(max(abs(lightPos.x),abs(lightPos.y)),abs(lightPos.z))), 1.0);\n" +
                                "   if(cut <= 0.0) { $cutoffContinue; }\n" +
                                "   lightColor *= cut;\n" +
                                "}\n"
                    } else "") +
                    "NdotL = lightNor.z;\n" + // dot(lightDirWS, globalNormal) = dot(lightDirLS, localNormal)
                    "lightColor *= max(NdotL, 0.0);\n" + // light looks much better with it

                    // to do implement screen-space shadows like screen-space ambient occlusion:
                    //     has sth blocking it, trace along the ray; if any hit, it's in shadow
                    // to do for this, we'd need two kinds of samplers for the same texture :/
                    /*
                    "vec3 sssRadius = 0.1 * length(finalPosition);\n" +
                    "vec3 sssPosition = sunDirWS * sssRadius + finalPosition;\n" +
                    "vec4 sssOffset = matMul(cameraMatrix, vec4(sssPosition, 0.0));\n" +
                    "vec2 sssUV = offset.xy/offset.w * 0.5 + 0.5;\n" +
                    "float sssShadow = sssUV.x >= 0.0 && sssUV.y >= 0.0 && sssUV.x <= 1.0 && sssUV.y <= 1.0 &&\n" +
                    "   sssTheoreticalDepth < sssActualDepth;\n" +*/

                    (if (withShadows) "" +
                            "if(shadowMapIdx0 < shadowMapIdx1 && receiveShadows && NdotL > 0.0){\n" +
                            // when we are close to the edge, we blend in
                            "   float edgeFactor = min(20.0*(1.0-max(abs(lightPos.x),abs(lightPos.y))),1.0);\n" +
                            "   if(edgeFactor > 0.0){\n" +
                            "       float shadowMapPower = shaderV1;\n" +
                            "       float invShadowMapPower = 1.0/shadowMapPower;\n" +
                            "       vec2 shadowDir = lightPos.xy;\n" +
                            "       vec2 nextDir = shadowDir * shadowMapPower;\n" +
                            // find the best shadow map
                            // blend between the two best shadow maps, if close to the border?
                            // no, the results are already very good this way :)
                            // at least at the moment, the seams are not obvious
                            "       float layerIdx=0.0;\n" +
                            "       while(abs(nextDir.x)<1.0 && abs(nextDir.y)<1.0 && shadowMapIdx0+1<shadowMapIdx1){\n" +
                            "           shadowMapIdx0++;\n" +
                            "           layerIdx++;\n" +
                            "           shadowDir = nextDir;\n" +
                            "           nextDir *= shadowMapPower;\n" +
                            "       }\n" +

                            // todo why does it need this scale factor and offset??
                            // todo can we integrate it into the matrix somehow??
                            // todo for large scale.z-values, the shadow becomes weaker... why???
                            //   must be our interpolation, I think... half of the pixels become shadowed,
                            //   because we would need a tighter/wider bias
                            "       float depthFromShader = (lightPos.z*.5 + shaderV3) + 0.005;\n" +

                            // do the shadow map function and compare
                            "       float depthFromTex = texture_array_depth_shadowMapPlanar(shadowMapIdx0, vec3(shadowDir.xy,layerIdx), NdotL, depthFromShader);\n" +
                            // todo this will become proportional to the distance to the shadow throwing surface
                            // "           float coc = 1.0 / texture_array_size_2d_shadowMapPlanar(shadowMapIdx0, 0).x;\n" +
                            // "           float val = texture_array_depth_shadowMapPlanar(shadowMapIdx0, shadowDir.xy).r;\n" +
                            // "           lightColor = vec3(val,val,dir.z);\n" + // nice for debugging
                            "       lightColor *= mix(1.0, depthFromTex * depthFromTex, edgeFactor);\n" +
                            "   }\n" +
                            "}\n"
                    else "") +
                    "effectiveDiffuse = lightColor;\n" +
                    "lightDir = vec3(0.0,0.0,-1.0);\n" +
                    "if(hasSpecular){\n" +
                    // good like that?
                    "   float NdotLi = reflect(viewDir, lightNor).z;\n" +
                    ShaderLib.invRoughness +
                    "   float x = max(NdotLi, 0.0), y = 1.0 + 256.0 * pow(invRoughness,2.0);\n" +
                    // pow(x,y) is the shape of sharpness; the divider is the integral from x=0 to x=1 over pow(x,y)*(1-x)
                    "   float lightEffect = pow(x,y) / (1.0/(y+1.0) - 1.0/(y+2.0));\n" +
                    "   effectiveSpecular = lightColor * lightEffect;\n" +
                    "}\n"
        }
    }
}