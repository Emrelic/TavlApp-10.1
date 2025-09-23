# PROMPT GÜNLÜĞÜ

## [2025-09-22] Zar Atma Sistemi ve İstatistik Implementasyonu

### Talep Özeti:
- Skorboard ekranında katlama zarının üzerinde "ZAR AT" butonu eklenmesi
- Geleneksel vs Modern tavla için farklı zar atma kuralları
- Detaylı zar istatistik sistemi implementasyonu

### Detaylar:

#### 1. Ekran Tasarımı:
- Oyun başında zarlar görünmez
- İki taraf birer zar atar, büyük atan tarafa küçük zar sürüklenir
- Çift atılırsa 4 zar küçülüp sıralanır

#### 2. Geleneksel Tavla Kuralları:
- İlk oyuncu tek zar atar
- 6 atarsa oyuna başlar, 1 atarsa karşı taraf başlar
- 2,3,4,5 atarsa diğer taraf da atar, büyük atan iki zar birden atar
- Eşitlik olursa yeniden atılır
- Sonraki oyunlarda kazanan başlar

#### 3. Modern Tavla Kuralları:
- Her oyun başında iki taraf birer zar atar
- Büyük atan ortadaki kombinasyonu kullanır
- Eşitlik olursa 1 saniye sonra otomatik yeniden atılır

#### 4. İstatistik Sistemi:
- Atılan zar çeşitleri (4-4, 6-5 vb.)
- Çift zarlar ayrı istatistik
- Toplam sayı ve parça istatistikleri
- Oynanan/oynamayan zarlar
- Boşa giden zarlar (ev toplaması sırasında)
- Oyun sonu boşa giden zarlar
- Checkbox sistemi ile zar işaretleme

#### 5. Örnek İstatistik:
**4-4 atıldığında:**
- Zar çeşidi: 4-4 → +1
- Çift zarlar → +1  
- Toplam sayı → +16
- Toplam parça → +4
- Oynanan sayı/parça (tümü oynanırsa) → 16/4
- Gele kalan zarlar checkbox ile işaretlenir

#### 6. Butonlar:
- "Bütün zarlar oynandı" butonu
- "Hepsi gele" butonu  
- Checkbox'lar ile kısmi oynama

---

## [2025-09-23] Sistem Çökmesi ve Kurtarma İşlemi

### Durum Tespiti:
- Dün gece limit bitti, çalışma yarım kaldı
- Sabah laptop şarjı bitti, sistem kapandı
- Yarım kalan zar atma sistemi implementasyonu tespit edildi
- Build hatası var - Gradle test problemi

### Yapılacaklar:
1. ✅ Git durumu kontrolü - GameScoreActivity-Emrelic.kt değişmiş
2. ✅ Yarım kalan kod tespiti - Zar sistemi %60 tamamlanmış
3. ✅ Build hatasını çözme - Gradle test sorunu çözüldü
4. ✅ Eksik implementasyonları tamamlama - Denendi
5. ✅ Build + telefona yükleme - Yapıldı

### Karşılaşılan Sorunlar:
1. **Syntax Hataları**: DiceComponent'te matematik hatası (size * 0.2f)
2. **Sonsuz Döngü**: startInitialRoll() fonksiyonunda rekursif çağrı
3. **ANR (Application Not Responding)**: Oyun başlatma sorunu
4. **Kritik Çökme**: Yeni zar sistemi uygulamayı çökertiyordu

### Çözüm Süreci:
1. **Build hatalarını düzelttik** - Import ve syntax sorunları
2. **Zar sistemini geçici devre dışı bıraktık** - ANR önlemi
3. **Database çağrılarını devre dışı bıraktık** - Blocking önlemi
4. **Git'ten eski çalışan versiyonu geri yükledik** (a163498)

### Nihai Durum:
- ❌ **Zar sistemi implementasyonu başarısız** - Çok karmaşık
- ✅ **Eski stabil versiyon geri yüklendi** - Crawford kuralı var
- ✅ **Oyun çalışıyor** - Temel fonksiyonlar sağlam
- 🔮 **Gelecek plan**: Zar sistemi daha basit şekilde tekrar eklenecek

---

## [2025-09-23] TAM SAYFA ZAR ATMA EKRANİ - DETAYLI TASARIM

### 🎯 Kullanıcı Talebi:
AlertDialog yerine **tam sayfa yatay zar atma ekranı** implementasyonu isteniyor.

### 📱 Ekran Tasarımı:
1. **Yatay mod**: Skorboard gibi landscape orientation
2. **Tam sayfa**: AlertDialog değil, full screen Activity/Composable
3. **İkiye bölünmüş layout**: Yatay çizgi ile üst/alt bölgeler
4. **İki taraflı kullanım**: Oyuncular telefonun iki ucunda oturacak
5. **Ters yazılar**: Bir taraf 180° döndürülmüş (upside down)

### 🎲 Zar Atma Sistemi:
#### Random Algoritma:
- 6 elemanlı dizi: [1,2,3,4,5,6]
- Sırayla random eleman çıkarma
- Her çıkarılan sayı zartta gösterilir
- Son kalan sayı = final sonuç
- **Örnek**: 3→5→6→1→4→**2** (2 kalır, sonuç=2)

#### Animasyon:
- Hızlı zar değişimi efekti
- Her iki zar bağımsız çalışır
- Gerçek zar desenlerinin gösterimi

### ⏰ Süre Tutma Sistemi:
- **Üst kısımda**: Dakika:Saniye sayacı
- **Sıralı çalışma**: Hangi oyuncunun sırası varsa onun sayacı çalışır
- **Geri sayım**: Belirlenen süre bitene kadar

### 📊 İstatistik Sistemi:
#### Zar İstatistikleri:
- **Atılan zar çeşitleri**: 1-1, 2-3, 4-4 vb.
- **Toplam sayı**: Çift atılırsa 4x sayı (4-4 = 16 sayı)
- **Çift zarlar**: Ayrı kategori
- **Oynanan/Gele**: Checkbox sistemi

#### Checkbox Durumları:
- **■ (Kare)**: Zar tamamen oynandı
- **✓ (Tik)**: Zar gele (oynanamadı)
- **☐ (Boş)**: Oyun sonu boşa giden

### 🔧 Ayar Kombinasyonları:
1. **Sadece zar atma**: Tek/çift zar seçimi
2. **Sadece süre tutma**: Sayaç aktif
3. **Zar + süre**: İkisi birlikte
4. **Zar + istatistik**: Zar tracking
5. **Tam kombinasyon**: Zar + süre + istatistik + değerlendirme

### 🎮 Oyun Akışı:
#### Geleneksel Tavla:
- İlk atış: 6 atarsa başlar, 1 atarsa karşı başlar
- Sonraki oyunlar: Kazanan başlar
- Büyük atan iki zarı alır

#### Modern Tavla:
- Her oyun: İki taraf birer zar atar
- Büyük atan kombinasyonu kullanır (5-2 gibi)
- Berabere: Yeniden atış

### 📱 UI Düzeni:
```
|[SAYAÇ]     ZAR AT     [SAYAÇ]|
|     ⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯    |
| [🎲] [🎲]  |  [🎲] [🎲]     |
|                              |
| [İSTATİSTİK PANEL]           |
| [CHECKBOX'LAR]               |
| [KAYDET] [TÜMÜ GELE] vb.     |
```

### 🔄 Sıra Sistemi:
1. Zar at → Random animasyon → Sonuç
2. İstatistik kaydet → Checkbox işaretle
3. Süre geçir → Karşı tarafa sıra
4. Döngü devam eder

Bu detaylı tasarım dokümanı implementasyon için hazır!