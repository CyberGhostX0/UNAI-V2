package com.umbranexus.unaiv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbranexus.unaiv2.ui.UnaiScreen
import com.umbranexus.unaiv2.ui.theme.UNAIV2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UNAIV2Theme {
                val viewModel: UnaiViewModel = viewModel()
                UnaiScreen(viewModel)
            }
        }
    }
}
