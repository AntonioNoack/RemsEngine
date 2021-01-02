package me.anno.objects.particles.forces.impl

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.objects.inspectable.InspectableAnimProperty
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.Vectors.times
import org.joml.Vector3f
import org.joml.Vector4f
import org.kdotjpg.OpenSimplexNoise
import java.util.*

class NoisyLorentzForce :
    PerParticleForce(
        Dict["Noisy Lorentz Force", "org.force.lorentz.noisy"],
        Dict["Circular motion by velocity, randomized by location", "org.force.lorentz.noisy.desc"]
    ) {

    lateinit var nx: OpenSimplexNoise
    lateinit var ny: OpenSimplexNoise
    lateinit var nz: OpenSimplexNoise

    var seed = initRandomizers(0)

    val fieldScale = AnimatedProperty.vec4(Vector4f(1f))

    fun initRandomizers(seed: Long): Long {
        val random = Random(seed)
        nx = OpenSimplexNoise(random.nextLong())
        ny = OpenSimplexNoise(random.nextLong())
        nz = OpenSimplexNoise(random.nextLong())
        return seed
    }

    fun getMagneticField(position: Vector3f, time: Double): Vector3f {
        val scale = fieldScale[time]
        val px = (position.x * scale.x).toDouble()
        val py = (position.y * scale.y).toDouble()
        val pz = (position.z * scale.z).toDouble()
        val pw = (time * scale.w)
        return Vector3f(
            nx.eval(px, py, pz, pw).toFloat(),
            ny.eval(px, py, pz, pw).toFloat(),
            nz.eval(px, py, pz, pw).toFloat()
        )
    }

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        val velocity = state.dPosition
        val position = state.position
        val localMagneticField = getMagneticField(position, time)
        return velocity.cross(localMagneticField * strength[time], Vector3f())
    }

    override fun listProperties(): List<InspectableAnimProperty> {
        return super.listProperties() + listOf(
            InspectableAnimProperty(
                fieldScale,
                "Field Scale",
                "How quickly the field is changing; in x,y,z and time direction"
            )
        )
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        getGroup("Force Field", "forces") += vi("Seed", "For the random component", null, seed, style) { seed = it }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeLong("seed", nx.seed)
        writer.writeObject(this, "fieldScale", fieldScale)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "fieldScale" -> fieldScale.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readLong(name: String, value: Long) {
        when (name) {
            "seed" -> seed = initRandomizers(value)
            else -> super.readLong(name, value)
        }
    }

    override fun getClassName() = "NoisyLorentzForce"

}