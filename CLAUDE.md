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
- **Tarih**: 2025-09-23
- **Durum**: TAM SAYFA ZAR ATMA EKRANİ TAMAMLANDI ✅

### 6. Zar Atma Sistemi Denemesi (2025-09-23) - İLK DENEME BAŞARISIZ
- ❌ **İlk implementasyon**: AlertDialog tabanlı, karmaşık ve problemli
- ❌ **ANR sorunları**: Uygulama donması ve çökmeler
- ❌ **Sonsuz döngü**: startInitialRoll() fonksiyonunda
- ✅ **Kurtarma işlemi**: Git'ten çalışan versiyon geri yüklendi (a163498)

### 7. Tam Sayfa Zar Sistemi (2025-09-23) - BAŞARILI ✅
- ✅ **DiceActivity**: Yeni tam sayfa Activity oluşturuldu
- ✅ **İki taraflı tasarım**: Sol taraf 180° döndürülmüş, sağ normal
- ✅ **Full screen**: Immersive mode, status bar gizli
- ✅ **Süre tutma**: İkili sayaç sistemi (5:00 format)
- ✅ **İstatistik**: Zar kombinasyonları ve checkbox sistemi
- ✅ **Animasyon**: Döner zar efekti ve visual feedback
- ✅ **Intent tabanlı**: Skorboard'dan DiceActivity'ye geçiş

## Mevcut Özellikler (Stabil Versiyon)
- ✅ **Crawford kuralı**: Tam implementasyon
- ✅ **Katlama sistemi**: Modern tavla için aktif
- ✅ **Geri al/Maçı sonlandır**: Mavi/kırmızı butonlar
- ✅ **Puan sistemi**: Otomatik hesaplama
- ✅ **TAM SAYFA ZAR SİSTEMİ**: Profesyonel tasarım ile tamamlandı

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