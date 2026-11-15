package io.github.nowaaak.quietv.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.WallpaperConfig

object Scenes {

    @DrawableRes
    fun drawable(scene: String): Int? = when (scene) {
        "mountains" -> R.drawable.scene_mountains
        "lake" -> R.drawable.scene_lake
        "dunes" -> R.drawable.scene_dunes
        "forest" -> R.drawable.scene_forest
        else -> null
    }

    @StringRes
    fun label(scene: String): Int? = when (scene) {
        "mountains" -> R.string.scene_mountains
        "lake" -> R.string.scene_lake
        "dunes" -> R.string.scene_dunes
        "forest" -> R.string.scene_forest
        WallpaperConfig.PARTICLES -> R.string.scene_particles
        else -> null
    }

    fun render(context: Context, scene: String, width: Int, height: Int): Bitmap? {
        val res = drawable(scene) ?: return null
        val drawable = context.getDrawable(res) ?: return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }
}
