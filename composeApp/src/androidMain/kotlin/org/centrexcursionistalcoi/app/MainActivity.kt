package org.centrexcursionistalcoi.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mmk.kmpnotifier.permission.permissionUtil
import io.github.vinceglb.filekit.core.FileKit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FileKit.init(this)

        // this will ask permission in Android 13(API Level 33) or above, otherwise permission will be granted.
        val permissionUtil by permissionUtil()
        permissionUtil.askNotificationPermission()

        setContent {
            AppRoot()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    AppRoot()
}
