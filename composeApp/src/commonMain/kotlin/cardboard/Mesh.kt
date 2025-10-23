package cardboard

import kotlin.jvm.JvmField

@OptIn(ExperimentalUnsignedTypes::class)
data class Mesh(
    val indices: UShortArray,
    @JvmField val vertices: FloatArray,
    @JvmField val normals: FloatArray? = null,
    @JvmField val uvs: FloatArray? = null,
) {
    val size: Int get() = indices.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Mesh

        if (!indices.contentEquals(other.indices)) return false
        if (!vertices.contentEquals(other.vertices)) return false
        if (!normals.contentEquals(other.normals)) return false
        if (!uvs.contentEquals(other.uvs)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indices.contentHashCode()
        result = 31 * result + vertices.contentHashCode()
        result = 31 * result + (normals?.contentHashCode() ?: 0)
        result = 31 * result + (uvs?.contentHashCode() ?: 0)
        return result
    }
}
