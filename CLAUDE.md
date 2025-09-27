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
- **Tarih**: 2025-09-27
- **Durum**: ZAR ATMA SİSTEMİ BAŞLANGIÇ SORUNU ÇÖZÜLDİ ✅

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

### 10. Skorboard T M B Buton Sistemi (2025-09-26) ✅
- ✅ **KATLA butonları**: T formatına çevrildi (28sp, yeşil arka plan)
- ✅ **Katlama cevap butonları**: T M B kesintisiz bant formatı
  - T: Kabul Et (yeşil)
  - M: Pes Et (kırmızı)  
  - B: İptal (gri)
- ✅ **Çift mod desteği**:
  - Yatay mod: 60dp yükseklik, 28sp font
  - Dikey mod: 50dp yükseklik, 24sp font
- ✅ **Her iki oyuncu**: Player1 ve Player2 için aynı T M B formatı
- ✅ **Tam ekran genişlik**: weight=1f ile eşit bölüştürme
- ✅ **Fonksiyonellik korundu**: Tüm katlama mantığı değişmeden

### 11. Zar Atma Ekranı Yeniden Tasarım (2025-09-27) ✅
- ✅ **Yatay düzen**: Sol açık mavi (#E3F2FD), sağ açık kırmızı (#FFEBEE)
- ✅ **Tam yükseklik butonları**: 100dp genişlik, süre göstergeleri
- ✅ **FIBO süre sistemi**: 90 saniye rezerv + 12 saniye hamle süresi
- ✅ **BackgammonTimeControl sınıfı**: Profesyonel turnuva kuralları
- ✅ **Merkez zar alanı**: Siyah çizgi (8dp) + her iki yanda zarlar
- ✅ **Çift yönlü tasarım**: Sağ buton 180° döndürülmüş (karşılıklı oyuncular)
- ✅ **Süre göstergeleri**: Rezerv (36sp) ve hamle süreleri (28sp) butonlarda
- ✅ **Kontrol butonları**: DURAKLAT/DEVAM/KAPAT
- ✅ **Başlangıç sistemi**: Zar karşılaştırması → otomatik oyuncu seçimi
- ✅ **UI temizliği**: Oyun modu/kurallar yazıları kaldırıldı
- ✅ **"SÜRE BAŞLAT" yazısı kaldırıldı**: Sadece süre göstergeleri

### 12. Zar Atma Başlangıç Sorunu Düzeltme (2025-09-27) ✅
- ❌ **Tespit edilen sorun**: Sol taraf zar attığında pasifleşiyor, sağ taraf da aynı
- 🔍 **Kök neden**: `leftDiceActive`/`rightDiceActive` state'leri launch sonunda kontrol edilmiyordu
- ✅ **Çözüm**: GamePhase 0'da zar atıldıktan sonra aktif kalma koruması eklendi
- ✅ **`leftHasRolled`/`rightHasRolled`**: Hangi tarafın attığını izleme sistemi
- ✅ **Karşılaştırma mantığı**: Sadece her iki taraf da attığında değerlendirme
- ✅ **Aktif kalma sistemi**: Zar atıldıktan sonra animasyon bitsin ama aktif kalsın
- ✅ **Build ve test**: APK başarıyla telefona yüklendi

## Mevcut Özellikler (Stabil Versiyon)
- ✅ **Crawford kuralı**: Tam implementasyon
- ✅ **Katlama sistemi**: Modern tavla için aktif, T M B formatında
- ✅ **3'lü buton sistemi**: Geri Al (koyu mavi), Zar At (mor), Maçı Sonlandır (koyu kırmızı)
- ✅ **7 ayar sistemi**: Yeni oyun sayfasında scrollable Row
- ✅ **Puan sistemi**: Otomatik hesaplama
- ✅ **Zar atma sistemi**: Tam sayfa DiceActivity, çökme sorunu çözüldü
- ✅ **FIBO saat sistemi**: Profesyonel turnuva kuralları (90sn + 12sn delay)
- ✅ **T M B buton sistemi**: Skorboard'da katlama butonları yeniden tasarlandı

## Google API Bilgileri (2025-09-23)
- **Paket Adı**: `com.tavla.tavlapp`
- **SHA-1 Parmak İzi**: `9C:F2:DB:04:34:4B:D9:D3:F1:A9:34:2C:3F:48:4A:DA:19:00:54:2D`
- **İstemci Kimliği**: `101677756808-9hpl2apr220rae0jcrrqja3cah81u63m.apps.googleusercontent.com`
- **MD5**: `63:4F:A5:D7:4D:67:81:CE:48:45:F0:B7:66:BB:F7:C3`
- **SHA-256**: `A3:59:31:9B:1A:D7:12:65:5A:63:88:F8:72:44:F0:5E:F3:60:43:C7:DE:F1:EB:FF:17:0F:34:10:05:4C:6C:B8`

---

# ÇALIŞMA PROTOKOLLERI

## 📝 NOT DEFTERLERİ PROTOKOLÜ
- **"ntk" komutu**: Tüm .md uzantılı not defterlerini okur
- **Dosyalar**: CLAUDE.md + diğer tüm .md dosyaları projeye dahil
- **"Not defterleri" = .md dosyaları**: Markdown uzantılı tüm dokümanlar

### 📋 YAPILACAKLAR NOT DEFTERİ
- **"ynd" komutu**: Yeni madde ekle (Yapılacaklar Not Defteri)
- **Dosya**: YAPILACAKLAR.md
- **Format**: [Kullanıcı madde] + ynd → otomatik kayıt
- **Otomatik tarih**: Her maddeye tarih damgası eklenir

### 📝 PROMPT GÜNLÜĞÜ SİSTEMİ
- **"*p" komutu**: Bu prompt'u günlüğe ekle (PROMPT_GUNLUGU.md'ye kaydet)
- **Dosya**: PROMPT_GUNLUGU.md
- **Format**: [Tarih-Saat] Prompt İçeriği
- **Manuel kontrol**: Kullanıcı "*p" demediği sürece ekleme yok

## 🔄 BERABER ÇALIŞMA PROTOKOLÜ
1. **🔧 Otomatik Build & Deploy:**
   - Her yenilik → APK build → telefona yükleme
   - Kullanıcı sorgulamaz, otomatik yapılır

0. **🪟 Terminal Başlık Protokolü:**
   - Claude Code terminalinde başlık değişikliği sınırlı
   - Standart terminal: `cmd /c "title TavlApp"`
   - PowerShell: `$host.ui.RawUI.WindowTitle = 'TavlApp'`
   - NOT: Claude Code'da bu özellik çalışmayabilir
   - *tpb komutu başlık değiştirme denemesi yapar

2. **🔊 SİSTEM BEEP PROTOKOLÜ:**
   - **Temel kurallar:**
     - Soru sorulacağı zaman → 3x beep
     - Onay alınacağı zaman → 3x beep
     - Sonuç sunulacağı zaman → 3x beep
     - Etkileşim gerekince → 3x beep
     - **Görev bitirip sunacağı zaman → 3x beep**
     - **1,2,3 tuş seçenekleri sunacağı zaman → 3x beep**

   - **Sessizlik yönetimi:**
     - Çalışma bitip 3 dakika sessizlik → 3x beep
     - 3 beep çalındı, cevap gelmedi → 3 dakika sonra tekrar 3x beep
     - Ara dakikalarda → 1x beep (cevap gelene kadar)

   - **Durdurma sistemi:**
     - "beep çalmayı bırak" VEYA "bçb" → o dönüş için beep durdur
     - Geçici durdurma: Sadece o andaki dönüş için geçerli
     - Otomatik yeniden başlatma: Yeni mesaj/görev geldiğinde beep protokolü yeniden aktif

   - **Kullanıcı Feedback Protokolü:**
     - **"BTŞ"** = Beep Teşekkür (Beep yaptığın için teşekkürler)
     - **"BTK"** = Beep Tenkid (Beep yapmadığın için tenkid)

## 🔥 YILDIZLI KOMUT SİSTEMİ (*)
**Her komut * ile başlar - Hızlı erişim için:**
- **"*p"** = Bu prompt'u günlüğe ekle (PROMPT_GUNLUGU.md'ye kaydet)
- **"*btk"** = Beep protokolünü uygulamadığın için tenkid
- **"*btş"** = Beep protokolü uyguladığın için teşekkür
- **"*tmm"** = Bu özellik tamam, commit + push yap
- **"*yle"** = Yapılacaklar listesine ekle
- **"*ncp"** = Not defterlerini doldur, commit + push
- **"*bty"** = Build et telefona yükle
- **"*ty"** = Telefona yükle (build et + yükle)
- **"*nto"** = Not defterlerini oku (ntk equivalent)
- **"*mo"** = md uzantılı tüm not defterlerini oku
- **"*çpe"** = Çalışma protokolüne ekle (ve terminal başlığını TavlApp yap)
- **"*tpb"** = Terminal pencere başlığını TavlApp olarak ayarla
- **"*ege"** = Ekran görüntülerine ekle
- **"*tsp"** = Sorunun ne olduğunu tespit et (bütün ihtimalleri listele)
- **"*tdv"** = Tespitleri tedavi et, düzelt
- **"*kyg"** = Kısayolları kod listesini göster
- **"*tk"** = Bu kod tekmil ver (emir tekrarı - anlama derecesini açıkla)

3. **💾 Hızlı Commit Protokolü:**
   - "tmm" diyince → anında commit + push
   - "[özellik adı] tamam" diyince → commit + push
   - Yarım kalan iş riski ortadan kalkar

4. **🎨 Görsel Protokol İsteği:**
   - Kullanıcı mesajları turuncu/farklı renkte görünmeli (sınırlı CLI desteği)

## 🔊 SİSTEM SESİ PROTOKOLÜ
**ZORUNLU UYGULANACAK KURALLAR:**

### Ne Zaman Sistem Sesi Çalacak:
1. **TÜM görevler tamamlandıktan sonra yeni talimat beklerken** - İş bitince kullanıcıdan yeni görev beklerken
2. **Kullanıcıdan onay isterken** - Kullanıcı onayı gerektiren işlemler öncesi
3. **Kullanıcıya soru sorarken** - Karar vermem gereken durumlar
4. **Etkileşim gerekince** - Kullanıcı müdahalesi lazım olduğunda
5. **Adımları listeleyip onay beklerken** - "1. Bu yap, 2. Şunu yap, 3. Bunu test et" gibi adım adım talimatlar verirken

### Ses Çıkarma Formatı:
**Önce mesajını yaz, EN SON SES ÇAL:**
```bash
# 1. Önce mesajını yaz
# 2. EN SON ses çal
powershell -c "[Console]::Beep(800,300); [Console]::Beep(800,300); [Console]::Beep(800,300)"
```

**SES MESAJIN EN SONUNDA ÇALACAK - böylece kullanıcı mesajı okur sonra ses duyar**

### ÇALMAYACAK DURUMLAR:
❌ Ara görev tamamlandığında
❌ Build successful olduğunda
❌ Dosya yazıldığında
❌ İş devam ederken

**NOT:** Sadece benden etkileşim/onay/talimat isteyeceğin zaman çal!