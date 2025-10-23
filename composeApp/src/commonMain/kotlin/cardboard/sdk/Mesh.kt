package cardboard.sdk

import kotlin.jvm.JvmField

/**
 * Struct representing a mesh for rendering in Cardboard VR.
 */
data class Mesh (
    /**
     * Indices of the mesh, used to define the order in which vertices are connected.
     */
    @JvmField val indices: IntArray,
    /**
     * Vertices of the mesh, defining the 3D points in space (3 values per vertex).
     */
    @JvmField val vertices: FloatArray,
    /**
     * UV coordinates for texture mapping, defining how textures are applied to the mesh space (2 values per vertex).
     */
    @JvmField val uvs: FloatArray,
){
    /**
     * Compares this CardboardMesh instance with another for equality.
     * Two CardboardMesh instances are considered equal if their indices, vertices, and uvs arrays are equal.
     *
     * @param other The other object to compare with.
     * @return True if the two CardboardMesh instances are equal, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Mesh

        if (!indices.contentEquals(other.indices)) return false
        if (!vertices.contentEquals(other.vertices)) return false
        if (!uvs.contentEquals(other.uvs)) return false

        return true
    }

    /**
     * Generates a hash code for the CardboardMesh instance.
     * The hash code is based on the content of indices, vertices, and uvs arrays.
     *
     * @return An integer hash code for this CardboardMesh.
     */
    override fun hashCode(): Int {
        var result = indices.contentHashCode()
        result = 31 * result + vertices.contentHashCode()
        result = 31 * result + uvs.contentHashCode()
        return result
    }
}