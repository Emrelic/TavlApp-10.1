package com.tavla.tavlapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.util.Log

// Ana aktivitemizi tanımlıyoruz. ComponentActivity, Compose kullanımı için bir temel sınıftır
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d("TavlApp", "MainActivity onCreate BAŞLADI")
            super.onCreate(savedInstanceState)
            Log.d("TavlApp", "super.onCreate TAMAM")

            // Veritabanını başlat ve varsayılan oyuncuları kontrol et
            Log.d("TavlApp", "DatabaseHelper oluşturuluyor...")
            val dbHelper = DatabaseHelper(this)
            Log.d("TavlApp", "DatabaseHelper TAMAM")

            Log.d("TavlApp", "getAllPlayers çağrılıyor...")
            val players = dbHelper.getAllPlayers()
            Log.d("TavlApp", "Players alındı: ${players.size}")

            // Eğer hiç oyuncu yoksa, iki varsayılan oyuncu ekle
            if (players.isEmpty()) {
                Log.d("TavlApp", "Varsayılan oyuncular ekleniyor...")
                dbHelper.addPlayer("Oyuncu 1")
                dbHelper.addPlayer("Oyuncu 2")
                Log.d("TavlApp", "Varsayılan oyuncular EKLENDİ")
            }

            Log.d("TavlApp", "setContent başlıyor...")
            // setContent ile Compose UI'ı başlatıyoruz
            setContent {
                Log.d("TavlApp", "TavlaAppTheme başlıyor...")
                // Theme.kt'de tanımlanan temayı uyguluyoruz
                TavlaAppTheme {
                    Log.d("TavlApp", "Surface oluşturuluyor...")
                    // Surface, arka plan rengini ve diğer özellikleri ayarlar
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Log.d("TavlApp", "MainScreen çağrılıyor...")
                        // Ana ekran içeriğimizi çağırıyoruz.
                        MainScreen()
                    }
                }
            }
            Log.d("TavlApp", "MainActivity onCreate TAMAM!")

        } catch (e: Exception) {
            Log.e("TavlApp", "MainActivity onCreate HATA: ${e.message}", e)
            e.printStackTrace()
            throw e // Hatayı tekrar fırlat ki sistem görsün
        }
    }
}
// Test değişikliği - GitHub Desktop kontrol

// Ana ekranımızı oluşturan Composable fonksiyon
@Composable
fun MainScreen() {
    // LocalContext sayesinde Android context'ine erişebiliriz (aktiviteye erişim için)
    val context = LocalContext.current

    // Column, içindeki öğeleri dikey olarak düzenler
    Column(
        modifier = Modifier
            .fillMaxSize()  // Tüm ekranı kapla
            .padding(16.dp), // Her yönden 16dp boşluk bırak
        verticalArrangement = Arrangement.Center, // İçeriği dikeyde ortala
        horizontalAlignment = Alignment.CenterHorizontally // İçeriği yatayda ortala
    ) {
        // Yeni Oyun butonu
        Button(
            onClick = {
                // Intent, bir aktiviteden diğerine geçmek için kullanılır
                context.startActivity(Intent(context, NewGameActivity::class.java))
            },
            modifier = Modifier
                .fillMaxWidth() // Butonun genişliğini ekran genişliğine eşitle
                .padding(vertical = 8.dp) // Üst ve alttan 8dp boşluk bırak
        ) {
            Text(text = "Yeni Oyun Aç")
        }

        // Oyun Geçmişi butonu
        Button(
            onClick = {
                // Oyun geçmişi ekranını aç
                context.startActivity(Intent(context, GameHistoryActivity::class.java))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Oyun Geçmişi")
        }

        // Çıkış butonu
        Button(
            onClick = {
                // Uygulamadan çıkmak için aktiviteyi sonlandırıyoruz
                (context as ComponentActivity).finish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Çık")
        }
        Button(
            onClick = {
                // Onay al
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Tüm Verileri Sıfırla")
                builder.setMessage("Tüm maç geçmişi ve oyuncu istatistikleri sıfırlanacak. Bu işlem geri alınamaz!")
                builder.setPositiveButton("Evet, Sıfırla") { _, _ ->
                    val dbHelper = DatabaseHelper(context)
                    val silinen = dbHelper.resetAllData()  // Yeni fonksiyonumuzu çağırın
                    Toast.makeText(context, "Tüm veriler sıfırlandı ($silinen maç silindi)", Toast.LENGTH_SHORT).show()
                }
                builder.setNegativeButton("İptal", null)
                builder.show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(text = "Tüm Verileri Sıfırla", color = MaterialTheme.colorScheme.error)
        }    }
}

// Preview, Android Studio'da tasarımı görmemizi sağlar
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TavlaAppTheme {
        MainScreen()
    }
}

