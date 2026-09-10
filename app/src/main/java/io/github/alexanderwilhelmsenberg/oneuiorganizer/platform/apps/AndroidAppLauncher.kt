package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId

class AndroidAppLauncher(private val context: Context) : AppLauncher {
    override fun launch(target: LaunchTargetId): Boolean {
        val component = target.toLaunchComponentSpec() ?: return false

        return try {
            context.startActivity(component.toLaunchIntent())
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}

private fun LaunchComponentSpec.toLaunchIntent(): Intent =
    Intent.makeMainActivity(ComponentName(packageName, className)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
