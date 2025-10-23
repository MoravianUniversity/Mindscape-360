package vr.app

import cardboard.Mesh
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin


/**
 * Generate an inward-facing sphere mesh with the given radius.
 *
 * The lon and lat counts determine the resolution of the sphere, with higher counts resulting in a
 * smoother sphere.
 */
fun sphere(radius: Float, lonCount: Int = 64, latCount: Int = 32): Mesh =
    truncatedSphere(radius, -1.0f, lonCount, latCount)

/**
 * Generate an inward-facing dome mesh with the given radius.
 *
 * The lon and lat counts determine the resolution of the dome, with higher counts resulting in a
 * smoother dome. The given lat count is based on the full sphere so it will be halved for the dome.
 */
fun dome(radius: Float, lonCount: Int = 64, latCount: Int = 32): Mesh =
    truncatedSphere(radius, 0.0f, lonCount, latCount)

/**
 * Generate an inward-facing truncated sphere mesh with the given radius and minimum latitude.
 *
 * The lon and lat counts determine the resolution of the sphere, with higher counts resulting
 * in a smoother sphere. They apply as if the sphere were a full sphere.
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun truncatedSphere(radius: Float, minLat: Float, lonCount: Int = 64, latCount: Int = 32): Mesh {
    // based on https://www.songho.ca/opengl/gl_sphere.html
    val latCountInv = 1.0f / latCount
    val latStep = PI * latCountInv
    val lonCountInv = 1.0f / lonCount
    val lonStep = 2 * PI * lonCountInv

    val n = (latCount+1)*(lonCount+1)
    val normals = FloatArray(n*3)
    val uvs = FloatArray(n*2)

    var normalIndex = 0
    var uvIndex = 0

    for (i in 0..latCount) {
        val angle = PI / 2 - i * latStep   // pi/2 (bottomLat) to -pi/2
        val nxy = cos(angle)
        val nz = sin(angle)

        // first and last vertices have same normal but different uv
        for (j in 0..lonCount) {
            val angle = j * lonStep        // 0 to 2pi

            // normalized vertex normal
            normals[normalIndex++] = nxy * cos(angle)
            normals[normalIndex++] = -nz
            normals[normalIndex++] = nxy * sin(angle)

            // vertex tex coord (s, t) range between [0, 1]
            uvs[uvIndex++] = j * lonCountInv
            uvs[uvIndex++] = i * latCountInv
        }
    }

    // generate vertices by multiplying by the radius
    // the y coordinate is adjusted based on the minimum latitude
    val vertices = FloatArray(normals.size) {
        if (it % 3 == 1) { max(normals[it], minLat) } else { normals[it] } * radius
    }
    val indices = sphereIndices(lonCount, latCount)
    return Mesh(indices, vertices, normals, uvs)
}

/**
 * Generate indices for a sphere mesh. The triangles face inwards.
 */
@OptIn(ExperimentalUnsignedTypes::class)
private fun sphereIndices(lonCount: Int, latCount: Int): UShortArray {
    require(lonCount >= 3 && latCount >= 2) { "lonCount must be at least 3 and latCount at least 2" }
    val step = (lonCount + 1).toUShort()
    val indices = UShortArray(latCount * lonCount * 6 - 6)
    var ii = 0
    var k1: UShort
    var k2: UShort

    // top vertices are a series of single triangles
    k1 = 0u
    k2 = step
    for (j in 0 until lonCount) {
        indices[ii++] = k1 + 1; indices[ii++] = k2 + 1; indices[ii++] = k2
        k1++; k2++
    }

    // middle vertices are a series of quads
    for (i in 1 until latCount-1) {
        k1 = step * i
        k2 = (k1 + step).toUShort()
        for (j in 0 until lonCount) {
            indices[ii++] = k1;     indices[ii++] = k1 + 1; indices[ii++] = k2
            indices[ii++] = k1 + 1; indices[ii++] = k2 + 1; indices[ii++] = k2
            k1++; k2++
        }
    }

    k1 = (step*latCount-step).toUShort()
    k2 = (k1 + step).toUShort()
    for (j in 0 until lonCount) {
        indices[ii++] = k1; indices[ii++] = k1 + 1; indices[ii++] = k2
        k1++; k2++
    }
    return indices
}

private inline operator fun UShort.times(other: Int): UShort = (this * other.toUShort()).toUShort()
private inline operator fun UShort.plus(other: Int): UShort = (this + other.toUShort()).toUShort()

private const val PI = 3.14159265358979323846f
