package com.learnhuayu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.learnhuayu.app.ui.LearnHuayuNavHost
import com.learnhuayu.core.ui.FeatureDestination
import com.learnhuayu.core.ui.LearnHuayuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.jvm.JvmSuppressWildcards

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LearnHuayuTheme {
                LearnHuayuNavHost(featureDestinations = featureDestinations)
            }
        }
    }
}
