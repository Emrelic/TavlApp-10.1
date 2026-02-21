# TavlApp Geliştirme Notları

## Bildirim Protokolu
- **KURAL**: Kullaniciya soru sorarken, onay isterken veya bitirmis bir calismayi sunarken **3 kez beep sistem sesi cal**
- **Komut**: `powershell -c "[console]::beep(800,200); Start-Sleep -Milliseconds 100; [console]::beep(1000,200); Start-Sleep -Milliseconds 100; [console]::beep(1200,200)"`
- **Ne zaman**: Soru sorma, onay isteme, tamamlanmis is sunma anlarinda
- **Her zaman uygula**, istisnasiz

## Proje Bilgileri
- **Proje Adı**: TavlApp
- **Platform**: Android (Kotlin + Jetpack Compose)
- **Ana Dosya**: `app\src\main\java\com\tavla\tavlapp\GameScoreActivity-Emrelic.kt`

## Yapılan Değişiklikler

### 1. Geri Al ve Maçı Sonlandır Butonları (2025-08-12)
- ✅ **Son hamleyi geri al** ve **Maçı sonlandır** butonları aynı satırda konumlandırıldı
- ✅ **Maçı sonlandır** butonu kırmızı renkde (Color.Red)
- ✅ **Son hamleyi geri al** butonu mavi renkde (Color(0xFF2196F3))
- ✅ Geri alınacak hamle olmadığında geri al butonu pasif durumda (enabled = undoStack.isNotEmpty())
- ✅ Butonlar Column içinde en altta sabit konumda
- ✅ Her iki buton eşit genişlikte (weight = 1f) ve 50dp yükseklikte

### 2. Katlama Zarı ve Butonları
- ✅ **Katlama zarı**: Normal boyutda (60dp, 24sp yazı boyutu)
- ✅ **Katlama butonları**: İnce boyutlarda (35dp yükseklik)
- ✅ **KATLA yazısı**: Küçük font boyutu (24sp → 18sp)
- ✅ **Cevap butonları**: Görünür boyutlarda
  - Yatay mod: 45dp yükseklik
  - Dikey mod: 40dp yükseklik

### 3. Eski Geri Al Butonları Temizlendi
- ✅ Oyuncu 1 alanındaki eski turuncu geri al butonu kaldırıldı
- ✅ Oyuncu 2 alanındaki eski turuncu geri al butonu kaldırıldı
- ✅ Artık sadece alttaki tek geri al butonu var

### 4. Katlama Mantığı
- **Player1 KATLA** → `showPlayer2DoublingMenu = true` (Player2'ye cevap menüsü)
- **Player2 KATLA** → `showPlayer1DoublingMenu = true` (Player1'e cevap menüsü)
- **Cevap Butonları**:
  - ✓ **Kabul Et** (yeşil)
  - ✗ **Pes Et** (kırmızı)
  - ↩ **İptal** (gri)

## Build ve Test Komutları
```bash
# APK oluştur
./gradlew assembleDebug

# APK'yı telefona yükle
adb install -r "app\build\outputs\apk\debug\app-debug.apk"

# Uygulamayı başlat
adb shell am start -n com.tavla.tavlapp/.MainActivity
```

## Test Checklist
- [ ] Modern tavla modu seç
- [ ] KATLA butonuna bas (35dp yükseklik, 18sp yazı)
- [ ] Cevap butonlarının görünmesini kontrol et
- [ ] Geri Al (mavi) ve Maçı Sonlandır (kırmızı) butonları altta sabit
- [ ] Eski turuncu geri al butonlarının kaldırıldığını doğrula

## Önemli Notlar
- **Katlama özelliği** sadece Modern tavla modunda aktif
- **Geri al butonu** hamle olmadığında pasif (gri) gösterilir
- **Katlama zarı** 60dp boyutunda, merkez konumda
- **Cevap butonları** hem yatay hem dikey modda görünür boyutlarda

### 5. Crawford Kuralı (2025-09-22)
- ✅ **Parti hedef puanı**: Varsayılan 11 puan olarak ayarlandı
- ✅ **Crawford eli kontrolü**: Hedef puanın 1 eksiğine ulaşıldığında devreye girer
- ✅ **Crawford göstergesi**: Küpün üstünde "CRAWFORD" yazısı görünür
- ✅ **Küp devre dışı**: Crawford elinde KATLA butonları ve küp tıklama pasif
- ✅ **Post-Crawford**: Crawford elinden sonra küp tekrar aktif olur
- ✅ **Reset kontrolü**: Yeni parti başladığında Crawford durumları sıfırlanır

## Crawford Kuralı Mantığı
- **Crawford eli**: Parti hedef puanın 1 eksiğine ulaşınca (örn: 11'lik partide 10'a gelince)
- **Küp kullanımı**: Crawford elinde tamamen devre dışı
- **Crawford sonrası**:
  - Önde olan kazanırsa → Parti biter
  - Arkada olan kazanırsa → Post-Crawford (küp tekrar aktif)

### 11. Dosya Adı Düzenleme Sorunu (2025-09-29) ✅
- ❌ **Problem**: `GameScoreActivity-Emrelic.kt` dosya adındaki tire (-) karakteri
- ✅ **Çözüm**: Dosya adı `GameScoreActivity.kt` olarak düzeltildi
- ✅ **Sonuç**: Android build sistemi artık dosyayı düzgün tanıyor
- ✅ **Test durumu**: APK başarıyla build edildi ve telefona yüklendi

## Son Güncelleme
- **Tarih**: 2025-09-29
- **Durum**: DOSYA ADI DÜZELTİLDİ - TÜM SİSTEMLER ÇALIŞIR DURUMDA ✅

### 6. Zar Atma Sistemi Çökme Sorunu (2025-09-25) ✅
- ❌ **İlk problem**: DiceActivity çökme sorunu - "program çöküyor zar atma butonuna basınca"
- ❌ **İkinci problem**: Zar atıcı ayarı ile oyun başlatınca çökme - "zar atıcı ayarına basıp oyuna basınca çöküyor"
- 🔍 **Tespit edilen sorun**: Karmaşık LaunchedEffect + Timer sistemi çakışması
- ✅ **Çözüm**: DiceActivity tamamen basitleştirildi
- ✅ **Basitleştirme**: 
  - Karmaşık timer/clock sistemi kaldırıldı
  - Try-catch koruması eklendi  
  - Basit UI: 2 zar + "ZAR AT" + "KAPAT" butonları
- ✅ **Sonuç**: APK başarıyla build edildi ve telefona yüklendi
- ✅ **Test durumu**: %100 çözüldü - her iki çökme sorunu giderildi

### 7. Yeni Oyun Ayarları Genişletmesi (2025-09-25) ✅
- ✅ **7 Ayar Çerçevesi**: Tek satırda scrollable Row ile düzenlendi
- ✅ **İstatistikler Tutulsun**: Switch ile kontrol edilen yeni ayar
- ✅ **Zar Değerlendirmesi İşaretlensin**: Switch ile kontrol edilen yeni ayar
- ✅ **Buton Genişlikleri**: Weight sistemi ile eşit dağılım (1f)
- ✅ **Padding Optimizasyonu**: 8dp → 4dp ile kompakt tasarım

### 8. Skorboard Buton Sistemi (2025-09-25) ✅
- ✅ **3'lü Buton Düzeni**: Geri Al, Zar At, Maçı Sonlandır
- ✅ **Renk Güncellemesi**:
  - Geri Al: Koyu mavi `#0D47A1` (iki ton koyu)
  - Zar At: Mor `#9C27B0` (ortada)
  - Maçı Sonlandır: Koyu kırmızı `#B71C1C` (iki ton koyu)
- ✅ **Eşit Boyutlar**: Her buton weight=1f ile eşit genişlik

### 9. Basitleştirilmiş Zar Sistemi (2025-09-25) ✅
- ✅ **Yatay Layout**: Sol uçuk mavi, sağ uçuk kırmızı, ortada siyah çizgi
- ✅ **Tam Ekran Butonlar**: Sol/sağ kenarlarda tam yükseklik tıklanabilir
- ✅ **Oyun Akışı**: Başlangıç zarı → büyük atan başlar → normal oyun
- ✅ **Basitleştirilmiş Zarlar**: Text bileşeni ile sayısal görünüm (1-6)
- ✅ **Çökme Sorunu Giderildi**: Drawable bağımlılıkları kaldırıldı
- ✅ **Saat Sistemi**: FIBO kuralları (90sn rezerv + 12sn delay) korundu

### 10. Profesyonel Satranç Saati Sistemi (2025-09-28) ✅
- ✅ **DGT3000 Tarzı Tasarım**: Profesyonel elektronik satranç saati arayüzü
- ✅ **Yatay (Landscape) Düzen**: Sol-sağ oyuncu yerleşimi
- ✅ **Döndürülmüş Görünüm**: 
  - Sol oyuncu: +90° döndürülmüş
  - Sağ oyuncu: -90° döndürülmüş
- ✅ **Orta Kontrol Paneli**: Dikey yerleşim (120dp)
  - PAUSE/PLAY butonu (dinamik renk)
  - Timer durumu göstergesi (RUNNING/PAUSED) 
  - DGT Timer branding
  - RESET butonu
- ✅ **Profesyonel Görünüm**:
  - Koyu gri arka plan (#1E1E1E)
  - Aktif/pasif renk kodlaması (yeşil/gri)
  - Monospace font kullanımı (48sp ana süre)
  - Border efektleri ve aktif oyuncu göstergeleri
- ✅ **FIBO Kuralları Uyumlu**: 90sn rezerv + 12sn hamle delay sistemi

## Mevcut Özellikler (Stabil Versiyon)
- ✅ **Crawford kuralı**: Tam implementasyon
- ✅ **Katlama sistemi**: Modern tavla için aktif
- ✅ **3'lü buton sistemi**: Geri Al (koyu mavi), Zar At (mor), Maçı Sonlandır (koyu kırmızı)
- ✅ **7 ayar sistemi**: Yeni oyun sayfasında scrollable Row
- ✅ **Puan sistemi**: Otomatik hesaplama
- ✅ **Zar atma sistemi**: Tam sayfa DiceActivity, çökme sorunu çözüldü
- ✅ **FIBO saat sistemi**: Profesyonel turnuva kuralları (90sn + 12sn delay)
- ✅ **Profesyonel satranç saati**: DGT3000 tarzı yatay düzen, tam profesyonel görünüm

## Google API Bilgileri (2025-09-23)
- **Paket Adı**: `com.tavla.tavlapp`
- **SHA-1 Parmak İzi**: `9C:F2:DB:04:34:4B:D9:D3:F1:A9:34:2C:3F:48:4A:DA:19:00:54:2D`
- **İstemci Kimliği**: `101677756808-9hpl2apr220rae0jcrrqja3cah81u63m.apps.googleusercontent.com`
- **MD5**: `63:4F:A5:D7:4D:67:81:CE:48:45:F0:B7:66:BB:F7:C3`
- **SHA-256**: `A3:59:31:9B:1A:D7:12:65:5A:63:88:F8:72:44:F0:5E:F3:60:43:C7:DE:F1:EB:FF:17:0F:34:10:05:4C:6C:B8`

