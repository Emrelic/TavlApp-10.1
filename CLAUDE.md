# TavlApp Geliştirme Notları

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

## Son Güncelleme
- **Tarih**: 2025-09-22
- **Durum**: Crawford kuralı eklendi ve test edildi