package cardboard.util

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSKeyValueObservingOptionOld
import platform.Foundation.NSKeyValueChangeNewKey
import platform.Foundation.NSKeyValueChangeOldKey
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.FoundationCompat.NSKVObserverProtocol
import platform.darwin.NSObjectProtocol
import platform.darwin.NSObject
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner


/**
 * An interface for an observer that can be enabled or disabled. Default state
 * is disabled. See createNotificationObserver() and createKVObserver() for
 * creating observers.
 */
interface Observer {
    fun enable()
    fun disable()
    val isEnabled: Boolean
}

/**
 * An observer for notifications on iOS. It allows you to register an action to
 * be executed when a notification with the specified name is posted, possibly
 * related to a specific object.
 *
 * The observer can be enabled or disabled, and it will automatically clean up
 * when it is no longer needed. It is not enabled by default, you must call
 * `enable()` to start observing.
 */
internal fun createNotificationObserver(
    name: String,
    obj: Any? = null,
    action: (NSNotification?) -> Unit,
): Observer = ObserverWithCleaner(NotificationObserver(name, obj, action))

/**
 * An observer for Key-Value Observing (KVO) on iOS. It allows you to register
 * an action to be executed when a specific key path on an `NSObject` is
 * changed.
 *
 * The observer can be enabled or disabled, and it will automatically clean up
 * when it is no longer needed. It is not enabled by default, you must call
 * `enable()` to start observing.
 */
internal fun <T> createKVObserver(
    obj: NSObject,
    name: String,
    action: (old: T?, new: T?) -> Unit,
): Observer = ObserverWithCleaner(KVObserver(obj, name, action))


/**
 * An observer wrapper that automatically cleans up when it is no longer needed.
 */
private class ObserverWithCleaner(private val observer: Observer): Observer by observer {
    @Suppress("unused")
    @OptIn(ExperimentalNativeApi::class)
    private val cleaner = createCleaner(observer) { it.disable() }
}

// See createNotificationObserver()
private data class NotificationObserver(
    val name: String,
    val obj: Any? = null,
    val action: (NSNotification?) -> Unit,
): Observer {
    var observer: NSObjectProtocol? = null
    override fun enable() {
        if (observer != null) { return }  // already enabled
        observer = NSNotificationCenter.defaultCenter.addObserverForName(name, obj, NSOperationQueue.mainQueue, action)
    }
    override fun disable() {
        observer?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it, name, obj)
        }
        observer = null
    }
    override var isEnabled: Boolean = observer != null
}

// See createKVObserver()
private class KVObserver<T>(
    val obj: NSObject,
    val name: String,
    val action: (old: T?, new: T?) -> Unit
): Observer {
    private var observer = NSKVObserver(action)
    override var isEnabled: Boolean = false; private set

    @OptIn(ExperimentalForeignApi::class)
    override fun enable() {
        if (!isEnabled) {
            obj.addObserver(
                observer, name,
                NSKeyValueObservingOptionOld or NSKeyValueObservingOptionNew, null)
            isEnabled = true
        }
    }

    override fun disable() {
        if (isEnabled) {
            obj.removeObserver(observer, name)
            isEnabled = false
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class NSKVObserver<T>(val action: (old: T?, new: T?) -> Unit): NSObject(), NSKVObserverProtocol {
    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalForeignApi::class)
    override fun observeValueForKeyPath(keyPath: String?,
                                        ofObject: Any?,
                                        change: Map<Any?, *>?, // Map<String, T>
                                        context: COpaquePointer?) {
        action(change?.get(NSKeyValueChangeOldKey) as? T, change?.get(NSKeyValueChangeNewKey) as? T)
    }
}
