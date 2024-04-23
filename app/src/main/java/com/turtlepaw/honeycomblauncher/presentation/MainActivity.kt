/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.turtlepaw.honeycomblauncher.presentation

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColor
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.rotaryinput.rotaryWithScroll
import com.turtlepaw.honeycomblauncher.presentation.components.ItemsListWithModifier
import com.turtlepaw.honeycomblauncher.presentation.theme.HoneycombLauncherTheme
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp(this)
        }
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    } else if (drawable is AdaptiveIconDrawable) {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    } else {
        throw IllegalArgumentException("unsupported drawable type")
    }
}

fun darkenColor(color: Color, factor: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    hsv[2] *= (1 - factor)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun App(context: Context, packageName: String) {
    HoneycombLauncherTheme {
        val pm = context.packageManager
//        val intent = Intent()
//        intent.setComponent(
//            ComponentName(packageName, activityName)
//        )
        val appIcon = pm.getApplicationIcon(packageName)
        val bitmap = drawableToBitmap(appIcon)

        Box(
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier
                .padding(8.dp)
                .size(40.dp)
                .clip(
                    CircleShape
                )
            ){
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(80.dp)
                        .blur(14.dp)
                        .background(
                            darkenColor(Color.Gray, 0.0f)
                        )
                )
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "App Icon",
                modifier = Modifier
                    .padding(8.dp)
                    .size(30.dp)
            )
        }
    }
}

fun getPackages(packageManager: PackageManager): List<ApplicationInfo> {
    return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)/*.filter {*/
//        (it.flags and ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM
//    }
}

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalHorologistApi::class)
@Composable
fun WearApp(context: Context) {
    HoneycombLauncherTheme {
        val focusRequester = rememberActiveFocusRequester()
        val scalingLazyListState = rememberScalingLazyListState()
        var visible by remember { mutableStateOf(false) }
        val lifecycleOwner = LocalLifecycleOwner.current
        val state by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
        val packageManager = context.packageManager
        var packages = getPackages(packageManager)
        LaunchedEffect(state) {
            if (state == Lifecycle.State.CREATED) {
                delay(1000)
                visible = true
            } else if (state == Lifecycle.State.RESUMED) {
                visible = false
                packages = emptyList()
                visible = true
                packages = getPackages(packageManager)
//                visible = false
//                delay(1000)
//                visible = true
            } else if (state == Lifecycle.State.DESTROYED) {
                visible = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText(
                modifier = Modifier.scrollAway(scalingLazyListState)
            )
            PositionIndicator(
                scalingLazyListState = scalingLazyListState
            )
            ItemsListWithModifier(
                modifier = Modifier.rotaryWithScroll(
                    reverseDirection = false,
                    focusRequester = focusRequester,
                    scrollableState = scalingLazyListState,
                ),
                scrollableState = scalingLazyListState,
            ) {
                item {
                    Spacer(modifier = Modifier.padding(0.1.dp))
                }
                val itemsPerRow = 3
                val packagesChunks = packages.chunked(itemsPerRow)

                if(packages.isEmpty()){
                    item {
                        Text(text = "No apps")
                    }
                }

                items(packagesChunks.size) { rowIndex ->
                    Row {
                        packagesChunks[rowIndex].forEach { packageInfo ->
                            val appName = packageInfo.labelRes // .loadLabel(packageManager).toString()
                            val packageName = packageInfo.packageName

                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(),
                                exit = fadeOut(spring(stiffness = 0f, dampingRatio = 0f))
                            ) {
                                Box {
                                    App(context, packageName)
                                }
                            }
                        }
                    }
                }

//                item {
//                    CircularProgressIndicator()
//                }
            }

        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(LocalContext.current)
}