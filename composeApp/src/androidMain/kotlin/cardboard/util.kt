package cardboard

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner

val Context.activity: Activity
    get() = findOwner(this) ?: throw IllegalStateException("Context has no Activity")

val Context.lifecycleOwner: LifecycleOwner
    get() = findOwner(this) ?: throw IllegalStateException("Context has no LifecycleOwner")

val Context.activityResultRegistryOwner: ActivityResultRegistryOwner
    get() = findOwner(this) ?: throw IllegalStateException("Context has no ActivityResultRegistryOwner")

private inline fun <reified T> findOwner(context: Context): T? {
    var innerContext = context
    while (innerContext is ContextWrapper) {
        if (innerContext is T) {
            return innerContext
        }
        innerContext = innerContext.baseContext
    }
    return null
}
