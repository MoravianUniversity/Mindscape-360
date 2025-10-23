package cardboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Build.VERSION
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import cardboard.sdk.Cardboard
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

actual typealias NativeContext = Context

@Composable
actual fun rememberNativeRenderView(): NativeRenderView {
    val context = LocalContext.current
    return remember { AndroidGLNativeRenderView(getGLSurfaceView(context)) }
}

class AndroidGLNativeRenderView(val view: GLSurfaceView): NativeRenderView() {
    override val context: Context get() = view.context
    private val activity get() = context.activity
    private val window get() = activity.window
    private val lifecycle get() = context.lifecycleOwner.lifecycle

    private inner class GLRenderer: GLSurfaceView.Renderer {
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) { init() }
        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) { resize(width, height) }
        override fun onDrawFrame(gl: GL10?) { draw() }
    }
    private inner class LifecycleObserver: DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) { resume() }
        override fun onPause(owner: LifecycleOwner) { pause() }
    }
    private val observer = LifecycleObserver()

    init {
        Cardboard.ensureInitialized(context)

        view.preserveEGLContextOnPause = true
        view.setRenderer(GLRenderer())
        @SuppressLint("ClickableViewAccessibility")
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                //view.queueEvent(::trigger)  // puts the trigger call on the GL thread
                trigger()  // puts the trigger call on the main thread
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }
    }

    override fun onInit() {
        super.onInit()
        activity.runOnUiThread {
            view.doOnAttach { lifecycle.addObserver(observer) }
            view.doOnDetach { destroy() }
        }
    }
    override fun onDestroy() {
        lifecycle.removeObserver(observer)
        super.onDestroy()  // this should be on the GL thread but that thread is not running anymore at this point (and the context has been destroyed so all OpenGL calls will fail anyways)
    }

    override fun onResume() {
        // On Android P and below, checks for activity to READ_EXTERNAL_STORAGE. When it is not granted,
        // the application will request them. For Android Q and above, READ_EXTERNAL_STORAGE is optional
        // and scoped storage will be used instead. If it is provided (but not checked) and there are
        // device parameters saved in external storage those will be migrated to scoped storage.
        if (VERSION.SDK_INT <= Build.VERSION_CODES.P && !isExternalStorageEnabled) {
            requestExternalStoragePermissions()
            return
        }
        view.onResume()
        super.onResume()
    }
    override fun onPause() { super.onPause(); view.onPause() }

    private val perms = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val isExternalStorageEnabled get() = perms.all { ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }
    private fun requestExternalStoragePermissions() {
        val key = "cardboard_request_external_storage_permission"
        val contract = ActivityResultContracts.RequestMultiplePermissions()
        val reg = context.activityResultRegistryOwner.activityResultRegistry
        val launcher = reg.register(key, contract) { isGranted ->
            if (perms.any { isGranted[it] != true && !ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }) {
                // Permission denied with checking "Do not ask again". In Android R "Do not ask again" is not available anymore.
                launchPermissionsSettings()
            }
            // TODO: activity.finish()
        }
        launcher.launch(perms)
    }
    private fun launchPermissionsSettings() {
        context.startActivity(Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ))
    }

    @Composable
    override fun RenderView(modifier: Modifier) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                val view = view.apply { (parent as? ViewGroup?)?.removeView(this) }
                FrameLayout(context).apply { addView(view) }
            }
        )
    }

    override fun setFullscreen() {
        val window = this.window
        val decorView = window.decorView

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }
    override fun resetFullscreen() {
        val window = this.window
        val decorView = window.decorView
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
        if (VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
    override fun setScreenBrightnessFull() {
        // TODO: have not tested
        val window = this.window
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        window.addFlags(FLAG_KEEP_SCREEN_ON)
    }
    override fun resetBrightness() {
        val window = this.window
        window.attributes = window.attributes.apply { screenBrightness = BRIGHTNESS_OVERRIDE_NONE }
        window.clearFlags(FLAG_KEEP_SCREEN_ON)
    }
    override fun forceLandscape() { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
    override fun resetOrientation() { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
}
