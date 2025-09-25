package com.tavla.tavlapp

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

class DiceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContent {
                MaterialTheme {
                    SimpleDiceScreen { finish() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}

@Composable
fun SimpleDiceScreen(onExit: () -> Unit) {
    var dice1 by remember { mutableIntStateOf(1) }
    var dice2 by remember { mutableIntStateOf(6) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = "ZAR ATMA EKRANI",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                text = dice1.toString(),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.background(
                    Color.LightGray,
                    androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ).padding(32.dp)
            )

            Text(
                text = dice2.toString(),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.background(
                    Color.LightGray,
                    androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ).padding(32.dp)
            )
        }

        Button(
            onClick = {
                dice1 = Random.nextInt(1, 7)
                dice2 = Random.nextInt(1, 7)
            },
            modifier = Modifier.size(width = 200.dp, height = 60.dp)
        ) {
            Text(text = "ZAR AT", fontSize = 20.sp)
        }

        Button(
            onClick = onExit,
            modifier = Modifier.size(width = 200.dp, height = 60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text(text = "KAPAT", fontSize = 20.sp)
        }
    }
}


