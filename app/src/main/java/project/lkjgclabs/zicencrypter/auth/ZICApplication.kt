package project.lkjgclabs.zicencrypter.auth

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class ZICApplication : Application(), LifecycleEventObserver {

    override fun onCreate() {
        // Specify the intended supertype in angle brackets
        super<Application>.onCreate()

        // Observe app-level lifecycle changes
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_STOP) {
            AuthManager.isAuthenticated = false
        }
    }
}