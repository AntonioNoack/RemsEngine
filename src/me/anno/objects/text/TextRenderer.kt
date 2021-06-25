package me.anno.objects.text

import me.anno.cache.keys.TextSegmentKey
import me.anno.fonts.PartResult
import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx3D
import me.anno.objects.attractors.EffectMorphing
import me.anno.objects.modes.TextRenderMode
import me.anno.studio.rems.Selection
import me.anno.ui.editor.sceneView.Grid
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Vectors.mulAlpha
import me.anno.utils.types.Vectors.times
import me.anno.video.MissingFrameException
import org.joml.*
import java.awt.font.TextLayout
import kotlin.math.max
import kotlin.math.min

object TextRenderer {

    fun draw(element: Text, stack: Matrix4fArrayList, time: Double, color: Vector4fc, superCall: () -> Unit) {

        val text = element.text[time]
        if (text.isBlank2()) {
            // element.super.onDraw(stack, time, color)
            superCall()
            return
        }

        val (lineSegmentsWithStyle, keys) = element.getSegments(text)

        val exampleLayout = lineSegmentsWithStyle.exampleLayout
        val scaleX = TextMesh.DEFAULT_LINE_HEIGHT / (exampleLayout.ascent + exampleLayout.descent)
        val scaleY = 1f / (exampleLayout.ascent + exampleLayout.descent)
        val width = lineSegmentsWithStyle.width * scaleX
        val height = lineSegmentsWithStyle.height * scaleY

        val lineOffset = -TextMesh.DEFAULT_LINE_HEIGHT * element.relativeLineSpacing[time]

        // min and max x are cached for long texts with thousands of lines (not really relevant)
        // actual text height vs baseline? for height

        val totalHeight = lineOffset * height

        val dx = element.getDrawDX(width, time)
        val dy = element.getDrawDY(lineOffset, totalHeight, time)

        val renderingMode = element.renderingMode
        val drawMeshes = renderingMode == TextRenderMode.MESH
        val drawSDFTextures = !drawMeshes

        // limit the characters
        // cursorLimit1 .. cursorLimit2
        val textLength = text.codePointCount(0, text.length)
        val startCursor = max(0, element.startCursor[time])
        var endCursor = min(textLength, element.endCursor[time])
        if (endCursor < 0) endCursor = textLength

        if (startCursor > endCursor) {
            // invisible
            // element.super.onDraw(stack, time, color)
            superCall()
            return
        }

        val lineBreakWidth = element.lineBreakWidth
        if (lineBreakWidth > 0f && !GFX.isFinalRendering && Selection.selectedTransform === element) {
            // draw the borders
            // why 0.81? correct x-scale? (off by ca ~ x0.9)
            val x0 = dx + width * 0.5f
            val minX = x0 - 0.81f * lineBreakWidth
            val maxX = x0 + 0.81f * lineBreakWidth
            val y0 = dy - lineOffset * 0.75f
            val y1 = y0 + totalHeight
            Grid.drawLine(stack, color, Vector3f(minX, y0, 0f), Vector3f(minX, y1, 0f))
            Grid.drawLine(stack, color, Vector3f(maxX, y0, 0f), Vector3f(maxX, y1, 0f))
        }

        val shadowColor = element.shadowColor[time]

        if (shadowColor.w() >= 0f) {
            val shadowSmoothness = element.shadowSmoothness[time]
            val shadowOffset = element.shadowOffset[time] * (1f / TextMesh.DEFAULT_FONT_HEIGHT)
            stack.next {
                stack.translate(shadowOffset)
                draw(
                    element, stack, time, color * shadowColor,
                    lineSegmentsWithStyle, drawMeshes, drawSDFTextures,
                    keys, exampleLayout,
                    width, lineOffset,
                    dx, dy, scaleX, scaleY,
                    startCursor, endCursor,
                    false, shadowSmoothness
                )
            }
        }

        draw(
            element, stack, time, color,
            lineSegmentsWithStyle, drawMeshes, drawSDFTextures,
            keys, exampleLayout,
            width, lineOffset,
            dx, dy, scaleX, scaleY,
            startCursor, endCursor,
            true, 0f
        )

    }

    private val oc0 = Vector4f()
    private val oc1 = Vector4f()
    private val oc2 = Vector4f()

    private fun correctColor(c0: Vector4fc, c1: Vector4f) {
        if (c1.w < 1f / 255f) {
            c1.set(c0.x(), c0.y(), c0.z(), c1.w)
        }
    }

    /**
     * if the alpha of a color is zero, it should be assigned the weighted sum if the neighbor colors
     * or the shader needs to be improved
     * let's just try the easiest way to correct the issue in 99% of all cases
     * */
    private fun correctColors(color: Vector4fc, oc0: Vector4f, oc1: Vector4f, oc2: Vector4f) {
        correctColor(color, oc0)
        correctColor(oc0, oc1)
        correctColor(oc1, oc2)
    }

    private fun getOutlineColors(
        element: Text,
        useExtraColors: Boolean, drawSDFTexture: Boolean, time: Double, color: Vector4fc,
        oc0: Vector4f, oc1: Vector4f, oc2: Vector4f
    ) {
        if (useExtraColors && drawSDFTexture) {
            val parentColor = element.parent?.getLocalColor(tmp0)
            if (parentColor != null) {
                oc0.set(parentColor).mul(element.outlineColor0[time])
                oc1.set(parentColor).mul(element.outlineColor1[time])
                oc2.set(parentColor).mul(element.outlineColor2[time])
            } else {
                oc0.set(element.outlineColor0[time])
                oc1.set(element.outlineColor1[time])
                oc2.set(element.outlineColor2[time])
            }
        } else {
            color.mulAlpha(element.outlineColor0[time].w(), oc0)
            color.mulAlpha(element.outlineColor1[time].w(), oc1)
            color.mulAlpha(element.outlineColor2[time].w(), oc2)
        }

        correctColors(color, oc0, oc1, oc2)

    }

    private val tmp0 = Vector4f()
    private fun draw(
        element: Text,
        stack: Matrix4fArrayList, time: Double, color: Vector4fc,
        lineSegmentsWithStyle: PartResult,
        drawMeshes: Boolean, drawSDFTexture: Boolean,
        keys: List<TextSegmentKey>, exampleLayout: TextLayout,
        width: Float, lineOffset: Float,
        dx: Float, dy: Float, scaleX: Float, scaleY: Float,
        startCursor: Int, endCursor: Int,
        useExtraColors: Boolean,
        extraSmoothness: Float
    ) {

        val oc0 = oc0
        val oc1 = oc1
        val oc2 = oc2

        getOutlineColors(element, useExtraColors, drawSDFTexture, time, color, oc0, oc1, oc2)

        var charIndex = 0

        firstTimeDrawing = true

        val textAlignment01 = element.textAlignment[time] * .5f + .5f
        for ((index, part) in lineSegmentsWithStyle.parts.withIndex()) {

            val startIndex = charIndex
            val partLength = part.codepointLength
            val endIndex = charIndex + partLength

            val localMin = max(0, startCursor - startIndex)
            val localMax = min(partLength, endCursor - startIndex)

            if (localMin < localMax) {

                val key = keys[index]

                val offsetX = (width - part.lineWidth * scaleX) * textAlignment01
                /* when (textAlignment) {
                    AxisAlignment.MIN -> 0f
                    AxisAlignment.CENTER -> (width - part.lineWidth * scaleX) / 2f
                    AxisAlignment.MAX -> (width - part.lineWidth * scaleX)
                } */

                val lineDeltaX = dx + part.xPos * scaleX + offsetX
                val lineDeltaY = dy + part.yPos * scaleY * lineOffset

                if (drawMeshes) {
                    drawMesh(
                        element, key, time, stack, color,
                        lineDeltaX, lineDeltaY,
                        localMin, localMax
                    )
                }

                if (drawSDFTexture) {
                    drawSDFTexture(
                        element, key, time, stack, color,
                        lineDeltaX, lineDeltaY,
                        localMin, localMax,
                        exampleLayout,
                        extraSmoothness,
                        oc0, oc1, oc2
                    )
                }

            }

            charIndex = endIndex

        }
    }

    var firstTimeDrawing = false

    private val scale0 = Vector2f()
    private fun drawSDFTexture(
        element: Text,
        key: TextSegmentKey, time: Double, stack: Matrix4fArrayList,
        color: Vector4fc, lineDeltaX: Float, lineDeltaY: Float,
        startIndex: Int, endIndex: Int,
        exampleLayout: TextLayout,
        extraSmoothness: Float,
        oc1: Vector4fc, oc2: Vector4fc, oc3: Vector4fc
    ) {

        val sdf2 = element.getSDFTexture(key)
        if (sdf2 == null) {
            if (GFX.isFinalRendering) throw MissingFrameException("Text-Texture (291) ${element.font}: '${element.text}'")
            element.needsUpdate = true
            return
        }

        val sdfResolution = SignedDistanceField.sdfResolution

        sdf2.charByChar = element.renderingMode != TextRenderMode.SDF_JOINED
        sdf2.roundCorners = element.roundSDFCorners

        val hasUVAttractors = element.children.any { it is EffectMorphing }

        sdf2.draw(startIndex, endIndex) { _, sdf, xOffset ->

            val texture = sdf?.texture
            if (texture != null && texture.isCreated) {

                val baseScale =
                    TextMesh.DEFAULT_LINE_HEIGHT / sdfResolution / (exampleLayout.ascent + exampleLayout.descent)

                val scaleX = 0.5f * texture.w * baseScale
                val scaleY = 0.5f * texture.h * baseScale
                val scale = scale0

                val sdfOffset = sdf.offset
                val offset = Vector2f(
                    (lineDeltaX + xOffset) * scaleX,
                    lineDeltaY * scaleY
                ).add(sdfOffset)

                /**
                 * character- and alignment offset
                 * */
                val charAlignOffsetX = lineDeltaX + xOffset
                val charAlignOffsetY = lineDeltaY + 0f

                /**
                 * offset, because the textures are always centered; don't start from the bottom left
                 * (text mesh does)
                 * */
                val sdfX = sdfOffset.x * scaleX
                val sdfY = sdfOffset.y * scaleY

                offset.set(charAlignOffsetX + sdfX, charAlignOffsetY + sdfY)
                scale.set(scaleX, scaleY)

                if (firstTimeDrawing) {

                    val outline = element.outlineWidths[time] * sdfResolution
                    outline.y = max(0f, outline.y) + outline.x
                    outline.z = max(0f, outline.z) + outline.y
                    outline.w = max(0f, outline.w) + outline.z

                    val s0 = Vector4f(extraSmoothness).add(element.outlineSmoothness[time])

                    val smoothness = s0 * sdfResolution

                    val outlineDepth = element.outlineDepth[time]

                    GFXx3D.drawOutlinedText(
                        element, time,
                        stack, offset, scale,
                        texture, color, 5,
                        arrayOf(
                            color, oc1, oc2, oc3,
                            Vector4f(0f)
                        ),
                        floatArrayOf(-1e3f, outline.x, outline.y, outline.z, outline.w),
                        floatArrayOf(0f, smoothness.x, smoothness.y, smoothness.z, smoothness.w),
                        outlineDepth, hasUVAttractors
                    )

                    firstTimeDrawing = false

                } else GFXx3D.drawOutlinedText(stack, offset, scale, texture, hasUVAttractors)

            } else if (sdf?.isValid != true) {

                if (GFX.isFinalRendering) throw MissingFrameException("Text-Texture I '${element.font}': '${key.text}'")
                element.needsUpdate = true

            }

        }
    }

    private fun drawMesh(
        element: Text,
        key: TextSegmentKey, time: Double, stack: Matrix4fArrayList,
        color: Vector4fc, lineDeltaX: Float, lineDeltaY: Float,
        startIndex: Int, endIndex: Int
    ) {

        val textMesh = element.getTextMesh(key)
        if (textMesh == null) {
            if (GFX.isFinalRendering) throw MissingFrameException("Text-Mesh II '${element.font}': '${key.text}'")
            element.needsUpdate = true
            return
        }

        textMesh.draw(startIndex, endIndex) { buffer, _, xOffset ->
            buffer!!
            val offset = Vector3f(lineDeltaX + xOffset, lineDeltaY, 0f)
            if (firstTimeDrawing) {
                GFXx3D.draw3DText(element, time, offset, stack, buffer, color)
                firstTimeDrawing = false
            } else {
                GFXx3D.draw3DTextWithOffset(buffer, offset)
            }
        }

    }


}