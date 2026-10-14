package me.anno.fonts.msdf

import me.anno.fonts.Font
import me.anno.fonts.FontImpl
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.io.json.generic.JsonScanner
import me.anno.maths.Packing.pack64
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.structures.arrays.ShortArrayList
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.toFloat
import me.anno.utils.types.Strings.toInt
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f
import speiger.primitivecollections.IntToIntHashMap
import speiger.primitivecollections.LongToDoubleHashMap
import java.io.InputStream

@Suppress("unused")
class MSDFFont(val image: FileReference) : FontImpl<MSDFFont>() {

    companion object {

        private val LOGGER = LogManager.getLogger(MSDFFont::class)

        private const val ADVANCE = 0
        private const val PLANE_LEFT = 1
        private const val PLANE_BOTTOM = 2
        private const val PLANE_RIGHT = 3
        private const val PLANE_TOP = 4
        private const val ATLAS_LEFT = 0
        private const val ATLAS_BOTTOM = 1
        private const val ATLAS_RIGHT = 2
        private const val ATLAS_TOP = 3
    }

    var width = 0
    var height = 0

    var baselineY = 1f
    var lineHeight = 1.2f
    var baseSize = 12f

    private val kerning = LongToDoubleHashMap(0.0)
    private val unicodeToIndex = IntToIntHashMap(-1)
    private val floats = FloatArrayList(5 * 96)
    private val shorts = ShortArrayList(4 * 96)

    fun add(
        unicode: Int, advance: Float,
        plane: Vector4f,
        atlas: Vector4f,
    ) {
        val index = unicodeToIndex.size
        unicodeToIndex[unicode] = index
        floats.add(advance)
        floats.add(plane)
        shorts.add(atlas.x.toInt().toShort())
        shorts.add(atlas.y.toInt().toShort())
        shorts.add(atlas.z.toInt().toShort())
        shorts.add(atlas.w.toInt().toShort())
    }

    fun putKerning(a: Int, b: Int, value: Float) {
        kerning[pack64(a, b)] = value.toDouble()
    }

    fun getKerning(a: Int, b: Int): Float {
        return kerning[pack64(a, b)].toFloat()
    }

    fun getIndex(unicode: Int): Int = unicodeToIndex[unicode]
    fun getAdvance(index: Int): Float = floats[index * 5 + ADVANCE]
    fun getPlaneLeft(index: Int): Float = floats[index * 5 + PLANE_LEFT]
    fun getPlaneRight(index: Int): Float = floats[index * 5 + PLANE_RIGHT]
    fun getPlaneTop(index: Int): Float = floats[index * 5 + PLANE_TOP]
    fun getPlaneBottom(index: Int): Float = floats[index * 5 + PLANE_BOTTOM]
    fun getAtlasLeft(index: Int): Short = shorts[index * 4 + ATLAS_LEFT]
    fun getAtlasRight(index: Int): Short = shorts[index * 4 + ATLAS_RIGHT]
    fun getAtlasTop(index: Int): Short = shorts[index * 4 + ATLAS_TOP]
    fun getAtlasBottom(index: Int): Short = shorts[index * 4 + ATLAS_BOTTOM]

    fun read(input: InputStream) {
        val scanner = JsonScanner(input)
        val plane = Vector4f()
        val atlas = Vector4f()
        scanner.scanObject { k1 ->
            when (k1) {
                "atlas" -> scanner.scanObject { k2 ->
                    when (k2) {
                        "width" -> width = scanner.readNumber().toInt()
                        "height" -> height = scanner.readNumber().toInt()
                        "size" -> baseSize = scanner.readNumber().toFloat()
                        else -> scanner.skipValue()
                    }
                }
                "metrics" -> scanner.scanObject { k2 ->
                    when (k2) {
                        "lineHeight" -> lineHeight = scanner.readNumber().toFloat()
                        "ascender" -> baselineY = scanner.readNumber().toFloat()
                        else -> scanner.skipValue()
                    }
                }
                "glyphs" -> scanner.scanArray {
                    var unicode = 0
                    var advance = 0.35f
                    plane.set(0f)
                    atlas.set(0f)
                    scanner.scanObject { k2 ->
                        when (k2) {
                            "unicode" -> unicode = scanner.readNumber().toInt()
                            "advance" -> advance = scanner.readNumber().toFloat()
                            "planeBounds" -> readBounds(scanner, plane)
                            "atlasBounds" -> readBounds(scanner, atlas)
                            else -> scanner.skipValue()
                        }
                    }
                    add(unicode, advance, plane, atlas)
                }
                "kerning" -> scanner.scanArray {
                    var unicode1 = 0
                    var unicode2 = 0
                    var advance = 0f
                    scanner.scanObject { k2 ->
                        when (k2) {
                            "unicode1" -> unicode1 = scanner.readNumber().toInt()
                            "unicode2" -> unicode2 = scanner.readNumber().toInt()
                            "advance" -> advance = scanner.readNumber().toFloat()
                            else -> scanner.skipValue()
                        }
                    }
                    if (unicode1 > 0 && unicode2 > 0 &&
                        advance != 0f
                    ) putKerning(unicode1, unicode2, advance)
                }
                else -> scanner.skipValue()
            }
        }

        LOGGER.info("Read MSDF: $width x $height, ${unicodeToIndex.size} glyphs, ${kerning.size}x kerning")
    }

    private fun readBounds(scanner: JsonScanner, dst: Vector4f) {
        scanner.scanObject { k3 ->
            val idx = when (k3) {
                "left" -> 0
                "bottom" -> 1
                "right" -> 2
                "top" -> 3
                else -> -1
            }
            val value = scanner.readNumber().toFloat()
            if (idx >= 0) dst[idx] = value
        }
    }

    override fun getTextLength(font: Font, codepoint: Int): Int {
        return getTextLengthF(font, codepoint).roundToIntOr()
    }

    fun getTextLengthF(font: Font, codepoint: Int): Float {
        val index = getIndex(codepoint)
        if (index < 0) return (0.35f * font.size)

        val left = getPlaneLeft(index)
        val right = getPlaneRight(index)
        return right - left
    }

    override fun getTextLength(font: Font, codepointA: Int, codepointB: Int): Int {
        val kerning = getKerning(codepointA, codepointB)
        val lenA = getTextLengthF(font, codepointA)
        val lenB = getTextLengthF(font, codepointB)
        val length = lenA + kerning + lenB
        return length.roundToIntOr()
    }

    override fun drawGlyph(
        image: IntImage, x0: Int, x1: Int, y0: Int, y1: Int,
        strictBounds: Boolean, font: Font, fallbackFonts: MSDFFont,
        fontIndex: Int, codepoint: Int, textColor: Int,
        backgroundColor: Int, portableImages: Boolean,
    ) {
        throw NotImplementedError("Use Canvas.drawText with this as fontImpl")
    }

    override fun getBaselineY(font: Font): Float {
        return baselineY * font.size
    }

    override fun getLineHeight(font: Font): Float {
        return lineHeight * font.size
    }

    // todo support fallbacks?
    override fun getFallbackFonts(font: Font): MSDFFont = this
    override fun getSupportLevel(fonts: MSDFFont, codepoint: Int, lastSupportLevel: Int): Int = 0
}