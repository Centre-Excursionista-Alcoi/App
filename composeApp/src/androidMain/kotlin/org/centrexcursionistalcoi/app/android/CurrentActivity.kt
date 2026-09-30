package org.centrexcursionistalcoi.app.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * The activity in the foreground, for APIs that show their own UI over it (like the Credential Manager's sheets),
 * and so need an activity, not just a context. Tracked from [AppBase].
 */
object CurrentActivity : Application.ActivityLifecycleCallbacks {
    private var current: WeakReference<Activity>? = null

    val activity: Activity? get() = current?.get()

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (current?.get() === activity) current = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
