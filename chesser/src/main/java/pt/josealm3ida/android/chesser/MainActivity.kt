package pt.josealm3ida.android.chesser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box (
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    Modifier.border(BorderStroke(2.dp, Color.Black)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (c in 0..7) {
                        Column {
                            for (r in 0..7) {
                                Box(
                                    Modifier
                                        .background(if (r % 2 == 0) if (c % 2 == 0) Color.Black else Color.White else if (c % 2 == 0) Color.White else Color.Black)
                                        .align(Alignment.CenterHorizontally)
                                        .size(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}