package com.tavla.tavlapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.tavla.tavlapp.ui.online.OnlineLobbyActivity

// Ana aktivitemizi tanımlıyoruz. ComponentActivity, Compose kullanımı için bir temel sınıftır
class MainActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = DatabaseHelper(this)
        val players = dbHelper.getAllPlayers()

        if (players.isEmpty()) {
            dbHelper.addPlayer("Oyuncu 1")
            dbHelper.addPlayer("Oyuncu 2")
        }

        // Uygulama acilisini logla
        dbHelper.addActivityLog(
            actionType = ActionTypes.APP_OPEN,
            description = "Uygulama acildi"
        )

        setContent {
            TavlaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.BgDark // Koyu tema arka planı
                ) {
                    MainScreen()
                }
            }
        }
    }
}
// Test değişikliği - GitHub Desktop kontrol

// Ana ekranımızı oluşturan Composable fonksiyon
@Composable
fun MainScreen() {
    // LocalContext sayesinde Android context'ine erişebiliriz (aktiviteye erişim için)
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }

    // Column, içindeki öğeleri dikey olarak düzenler
    Column(
        modifier = Modifier
            .fillMaxSize()  // Tüm ekranı kapla
            .background(AppColors.BgDark)
            .padding(24.dp), // Her yönden 24dp boşluk bırak
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically), // İçeriği dikeyde ortala ve aralarında boşluk
        horizontalAlignment = Alignment.CenterHorizontally // İçeriği yatayda ortala
    ) {
        // Başlık
        Text(
            text = "TAVLA OYUNU", 
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.GoldLight,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        // Yeni Oyun kartı
        Card(
            onClick = {
                // Intent, bir aktiviteden diğerine geçmek için kullanılır
                context.startActivity(Intent(context, NewGameActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.BgCard),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = "🎮 Yeni Oyun Başlat",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextWhite,
                modifier = Modifier.padding(20.dp)
            )
        }

        // Online Oyun kartı
        Card(
            onClick = {
                context.startActivity(Intent(context, OnlineLobbyActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = "🌐 Online Oyun",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(20.dp)
            )
        }

        // Oyun Geçmişi kartı
        Card(
            onClick = {
                // Oyun geçmişi ekranını aç
                context.startActivity(Intent(context, GameHistoryActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.BgCard),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = "📊 Oyun Geçmişi",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextWhite,
                modifier = Modifier.padding(20.dp)
            )
        }

        // Hareketler Dökümü kartı
        Card(
            onClick = {
                // Hareketler dokumu ekranini ac
                context.startActivity(Intent(context, ActivityLogActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.BgCard),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = "📋 Hareketler Dökümü",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextWhite,
                modifier = Modifier.padding(20.dp)
            )
        }

        // Çıkış kartı
        Card(
            onClick = {
                // Uygulamadan çıkmak için aktiviteyi sonlandırıyoruz
                (context as ComponentActivity).finish()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF8B0000)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = "🚪 Çıkış",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Veri Sıfırlama kartı (tehlikeli işlem)
        Card(
            onClick = {
                // Onay al
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Tüm Verileri Sıfırla")
                builder.setMessage("Tüm maç geçmişi ve oyuncu istatistikleri sıfırlanacak. Bu işlem geri alınamaz!")
                builder.setPositiveButton("Evet, Sıfırla") { _, _ ->
                    val dbHelper = DatabaseHelper(context)
                    val silinen = dbHelper.resetAllData()
                    // Veri sifirlama islemini logla
                    dbHelper.addActivityLog(
                        actionType = ActionTypes.DATA_RESET,
                        description = "Tum veriler sifirlandi ($silinen mac silindi)"
                    )
                    Toast.makeText(context, "Tüm veriler sıfırlandı ($silinen maç silindi)", Toast.LENGTH_SHORT).show()
                }
                builder.setNegativeButton("İptal", null)
                builder.show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4A1A1A)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = "⚠️ Tüm Verileri Sıfırla",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFF6B6B),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// Preview, Android Studio'da tasarımı görmemizi sağlar
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TavlaAppTheme {
        MainScreen()
    }
}

