package io.github.stivengjekaj.zettastream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val kind = deviceKind()
        setContent {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0B0D12)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "ZettaStream (${kind.name})",
                    color = Color(0xFFE8EAF0),
                    fontSize = 32.sp,
                )
            }
        }
    }
}
