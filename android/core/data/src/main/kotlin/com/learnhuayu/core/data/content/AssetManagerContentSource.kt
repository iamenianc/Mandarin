package com.learnhuayu.core.data.content

import android.content.res.AssetManager
import java.io.InputStream
import javax.inject.Inject

internal class AssetManagerContentSource @Inject constructor(
    private val assets: AssetManager,
) : ContentSource {
    override fun open(path: String): InputStream = assets.open(path)
}
