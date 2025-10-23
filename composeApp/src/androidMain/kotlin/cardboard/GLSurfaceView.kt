package cardboard

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.EGL14
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.*
import android.opengl.GLUtils
import javax.microedition.khronos.egl.*

fun getGLSurfaceView(context: Context, version: Int = 30): GLSurfaceView {
    class ConfigChooser : EGLConfigChooser {
        private fun choose(egl: EGL10, display: EGLDisplay, antialias: Boolean = true): EGLConfig? {
            val attribList = intArrayOf(
                EGL10.EGL_LEVEL, 0,
                EGL10.EGL_RENDERABLE_TYPE, if (version <= 20) EGL14.EGL_OPENGL_ES2_BIT else EGL_OPENGL_ES3_BIT,
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
                EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER,
                EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8, EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_SAMPLE_BUFFERS, if (antialias) 1 else 0,
                EGL10.EGL_SAMPLES, if (antialias) 4 else 0,  // This is for 4x MSAA.
                EGL10.EGL_NONE
            )
            val output = IntArray(1)
            val configs = arrayOfNulls<EGLConfig>(1)
            eglCheck(egl.eglChooseConfig(display, attribList, configs, 1, output))
            return if (output[0] == 0) { null } else { configs[0] }
        }
        override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig? {
            choose(egl, display, true)?.let { return it }
            choose(egl, display, false)?.let { return it }
            return null
        }
    }

    class ContextFactory : EGLContextFactory {
        override fun createContext(egl: EGL10, display: EGLDisplay, config: EGLConfig): EGLContext {
            val major = version / 10
            val minor = version % 10
            val attribList = if (/*eglVersion(egl, display) < 1.5 ||*/ minor == 0) {
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, major, EGL10.EGL_NONE)
            } else {
                intArrayOf(EGL_CONTEXT_MAJOR_VERSION, major, EGL_CONTEXT_MINOR_VERSION, minor, EGL10.EGL_NONE)
            }
            return eglCheck(egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attribList))
        }
        override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) { eglCheck(egl.eglDestroyContext(display, context)) }
    }

    return GLSurfaceView(context).apply {
        holder.setFormat(PixelFormat.RGBA_8888)
        setEGLContextFactory(ContextFactory())
        setEGLConfigChooser(ConfigChooser())
        // TODO: can I just use:
        //  setEGLContextClientVersion(3)
        //  setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        //  but doesn't set EGL_SAMPLE_BUFFERS or EGL_SAMPLES, so no MSAA
    }
}

// Only available in EGL15 (Android 10 / API 29)
private const val EGL_CONTEXT_MAJOR_VERSION = 0x3098 // same as EGL14.EGL_CONTEXT_CLIENT_VERSION
private const val EGL_CONTEXT_MINOR_VERSION = 0x30FB
private const val EGL_OPENGL_ES3_BIT = 0x00000040

private fun <T> eglCheck(value: T): T {
    if (EGL14.eglGetError() != EGL10.EGL_SUCCESS) {
        throw RuntimeException(GLUtils.getEGLErrorString(EGL14.eglGetError()))
    }
    return value
}
private fun eglVersion(egl: EGL10, display: EGLDisplay?) =
    egl.eglQueryString(display, EGL10.EGL_VERSION).split(' ')[0].toFloat()
