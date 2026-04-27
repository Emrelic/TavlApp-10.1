# Rövanş Zar Takas Hatası — Düzeltme Yaması

**Tarih:** 2026-04-27
**Hata:** Rövanş (Tur 2) sırasında her oyuncu, Tur 1'de oynadığı **aynı zar dizisini** yine kendisi oynuyor. Sebep: iki ayrı yerde yapılan takasın birbirini iptal etmesi (double-swap).
**Hedef Davranış (Yol B):** İlk turda Mehmet'e gelen zar seti, rövanşta Ahmet'e gitmeli; Ahmet'e gelen zar seti de Mehmet'e gitmeli. Oyuncuların ekranda yer değiştirmesine gerek yok — pozisyon sabit, sadece zar mapping'i değişiyor.

---

## Özet — Davranış Tablosu

| | Tur 1 | Tur 2 (mevcut, hatalı) | Tur 2 (yamadan sonra) |
|---|---|---|---|
| Sol pozisyon (Player1 / Ahmet) | A dizisi | A dizisi ❌ | **B dizisi** ✓ |
| Sağ pozisyon (Player2 / Mehmet) | B dizisi | B dizisi ❌ | **A dizisi** ✓ |

A = encounter kurulurken `player1Dice` olarak üretilen orijinal Ahmet'in dizisi
B = encounter kurulurken `player2Dice` olarak üretilen orijinal Mehmet'in dizisi

---

## Neden Sadece DatabaseHelper Yetmez

`DatabaseHelper.getDiceSetForGame()` zaten Tur 2 için doğru takası yapıyor (satır 2308-2329). Ancak `RematchDiceDisplayActivity.kt` ve `GameScoreActivity.kt` içinde Tur 2'de oyuncu pozisyonlarını da takas eden bir if/else var. Bu ikinci takas, ilkini iptal ediyor. Yamanın amacı: pozisyon takasını iki yerden de kaldırıp, DB'deki tek takasın işini yapmasına izin vermek.

---

## DEĞİŞİKLİK 1 — `RematchDiceDisplayActivity.kt`

### 1.A) Satır 213-223: Pozisyon takası kaldır

**ÖNCE:**
```kotlin
if (currentRound == 1) {
    leftPlayerName = encounter.value?.player1Name ?: "Oyuncu 1"
    rightPlayerName = encounter.value?.player2Name ?: "Oyuncu 2"
    leftPlayerId = encounter.value?.player1Id ?: 0L
    rightPlayerId = encounter.value?.player2Id ?: 0L
} else {
    leftPlayerName = encounter.value?.player2Name ?: "Oyuncu 2"
    rightPlayerName = encounter.value?.player1Name ?: "Oyuncu 1"
    leftPlayerId = encounter.value?.player2Id ?: 0L
    rightPlayerId = encounter.value?.player1Id ?: 0L
}
```

**SONRA:**
```kotlin
// ✅ YOL B: Pozisyon her iki turda da sabit. Player1 her zaman SOL, Player2 her zaman SAĞ.
// Tur 2'de zar mapping'i DatabaseHelper.getDiceSetForGame() tarafından otomatik takas edilir,
// bu yüzden burada oyuncu pozisyonlarını swap etmeye gerek yok.
leftPlayerName = encounter.value?.player1Name ?: "Oyuncu 1"
rightPlayerName = encounter.value?.player2Name ?: "Oyuncu 2"
leftPlayerId = encounter.value?.player1Id ?: 0L
rightPlayerId = encounter.value?.player2Id ?: 0L
```

### 1.B) Satır 259-260: Skor pozisyonunu sabit yap

**ÖNCE:**
```kotlin
val leftScore = if (currentRound == 1) partyScores.first else partyScores.second
val rightScore = if (currentRound == 1) partyScores.second else partyScores.first
```

**SONRA:**
```kotlin
// ✅ YOL B: Player1 her zaman solda, Player2 her zaman sağda
val leftScore = partyScores.first    // Player1 skoru
val rightScore = partyScores.second  // Player2 skoru
```

### 1.C) Satır 282: `leftIsP1` her zaman true

**ÖNCE:**
```kotlin
val leftIsP1 = currentRound == 1
```

**SONRA:**
```kotlin
// ✅ YOL B: Sol pozisyon her zaman Player1 — Tur'a bağlı değil
val leftIsP1 = true
```

> **Not:** `leftIsP1`'i tamamen kaldırmak istersen, kullanıldığı yerleri (satır 285, 288, 306, 307, 309, 310) sabitleyebilirsin (`if (leftIsP1) X else Y` → sadece `X`). Ancak bu daha geniş bir refactor olacağından, en güvenli yol değeri `true` yaparak değiştirmektir; derleyici dead-code olarak optimize eder.

---

## DEĞİŞİKLİK 2 — `GameScoreActivity.kt`

### 2.A) Satır 889-897: saveRematchGameResult'taki swap'ı kaldır

**ÖNCE:**
```kotlin
val leftPlayerId: Long
val rightPlayerId: Long
if (rematchCurrentRound == 1) {
    leftPlayerId = player1Id
    rightPlayerId = player2Id
} else {
    leftPlayerId = player2Id
    rightPlayerId = player1Id
}
```

**SONRA:**
```kotlin
// ✅ YOL B: Sol her zaman Player1, sağ her zaman Player2 — pozisyon sabit
val leftPlayerId: Long = player1Id
val rightPlayerId: Long = player2Id
```

> Bu sayede `leftDiceTotal` ve `rightDiceTotal` (SharedPreferences'tan okunan) tutarlı bir şekilde Player1/Player2 pozisyonuna karşılık gelir. Karşılaştırma sayfası (`RematchComparisonActivity`) zaten left+right toplam üzerinden çalıştığı için ek değişiklik gerekmez; ama artık her satırda Round 1 ve Round 2 için sol-slot daima aynı oyuncuya işaret edeceğinden istatistikler de tutarlı kalır.

---

## DEĞİŞİKLİK 3 (İSTEĞE BAĞLI) — `DatabaseHelper.kt` Activity Log mesajı

Mevcut log mesajı oyuncu adlarını içermiyor. Daha açıklayıcı yapmak için:

### Satır 2310-2316 civarı:

**ÖNCE:**
```kotlin
val setId = DiceGenerator.generateSetId(partyIndex, setIndex, 0)
addActivityLog(
    actionType = ActionTypes.REMATCH_DICE_REVERSE,
    description = "Tur 2: Aynı zar setleri ters oynatıldı (Set: $setId)",
    extraData = "original_round=1,reverse_round=2,party_index=$partyIndex,set_index=$setIndex"
)
```

**SONRA:**
```kotlin
val setId = DiceGenerator.generateSetId(partyIndex, setIndex, 0)
val encounter = getRematchEncounter(encounterId)  // oyuncu adlarını çek
val p1Name = encounter?.player1Name ?: "Oyuncu1"
val p2Name = encounter?.player2Name ?: "Oyuncu2"
addActivityLog(
    actionType = ActionTypes.REMATCH_DICE_REVERSE,
    description = "Tur 2 — Set $setId: $p1Name şimdi $p2Name'in orijinal zarını oynuyor; $p2Name şimdi $p1Name'in orijinal zarını oynuyor",
    extraData = "original_round=1,reverse_round=2,party_index=$partyIndex,set_index=$setIndex,p1=$p1Name,p2=$p2Name"
)
```

---

## DOĞRULAMA — Yamadan Sonra Olması Gereken Davranış

Encounter kurulurken üretilen veriler:
```
Set #1:
  player1Dice = A dizisi   → Bu Ahmet'in (P1) orijinal zar setidir
  player2Dice = B dizisi   → Bu Mehmet'in (P2) orijinal zar setidir
```

**Tur 1 oynanırken** (currentRound = 1, getDiceSetForGame swap YAPMAZ):
- `currentDiceSet.player1Dice = A`, `currentDiceSet.player2Dice = B`
- Sol = Ahmet (P1) → `leftDice = player1Dice = A` → **Ahmet, A oynar** ✓
- Sağ = Mehmet (P2) → `rightDice = player2Dice = B` → **Mehmet, B oynar** ✓

**Tur 2 oynanırken** (currentRound = 2, getDiceSetForGame swap YAPAR):
- `currentDiceSet.player1Dice = B` (DB swap), `currentDiceSet.player2Dice = A` (DB swap)
- Sol = Ahmet (P1, pozisyon sabit) → `leftDice = player1Dice = B` → **Ahmet, B oynar** ✓ (Mehmet'in orijinal zarı)
- Sağ = Mehmet (P2, pozisyon sabit) → `rightDice = player2Dice = A` → **Mehmet, A oynar** ✓ (Ahmet'in orijinal zarı)

Talep ettiğin davranış birebir karşılanır:
> *"İlk turda Mehmet'e gelen zar seti rövanşta Ahmet'e gelmeli; Ahmet'e gelen zar seti de Mehmet'e gelmeli"*

---

## TEST CHECKLIST

Yamadan sonra şu testleri yapmanı öneriyorum:

- [ ] Yeni rövanş kur (3 parti, 11 puan, Ahmet vs Mehmet)
- [ ] Tur 1, Parti 1, Set 1'de zar dizisini telefonda yan tarafa not al (mesela ilk 3 hamle: Ahmet 6-3, 4-4, 5-1; Mehmet 1-2, 3-3, 6-5)
- [ ] Tur 1'i bitir, Tur 2'ye geç
- [ ] Tur 2, Parti 1, Set 1'de:
  - **Ahmet** ekranında ilk hamlelerin **1-2, 3-3, 6-5** çıkması beklenir (yani Tur 1'de Mehmet'in zarları)
  - **Mehmet** ekranında ilk hamlelerin **6-3, 4-4, 5-1** çıkması beklenir (yani Tur 1'de Ahmet'in zarları)
- [ ] Activity Log'da `REMATCH_DICE_REVERSE` kayıtları artık oyuncu adlarını içeriyor olmalı (Değişiklik 3 uygulandıysa)
- [ ] Karşılaştırma sayfasında (RematchComparison) sol-slot zar toplamı her iki turda da Player1'e (Ahmet'e) ait olmalı

---

## YAMA UYGULAMA ÖZETİ (Hızlı Bakış)

| Dosya | Satır | Yapılacak |
|---|---|---|
| RematchDiceDisplayActivity.kt | 213-223 | Pozisyon swap'ı kaldır, sadece Round-1 atama |
| RematchDiceDisplayActivity.kt | 259-260 | Skoru sabit ata: left=first, right=second |
| RematchDiceDisplayActivity.kt | 282 | `leftIsP1 = true` |
| GameScoreActivity.kt | 889-897 | leftPlayerId=player1Id, rightPlayerId=player2Id sabit |
| DatabaseHelper.kt | 2310-2316 | (opsiyonel) Activity log'a oyuncu adlarını ekle |

---

## RİSKLER VE ETKİ

- **Saat sistemi (DGT timer):** Sol/sağ saatler hala Player1/Player2'ye doğru bağlı kalır. Round 2'de Player1 hala soldaki saati kullanır.
- **Katlama küpü:** `leftIsP1 = true` olduğu için cube position bilgisi (LEFT_CTRL/RIGHT_CTRL) artık her zaman Player1=sol perspektifinden hesaplanır. Doğru.
- **İstatistikler:** `leftDiceTotal` artık her iki turda da Player1'e karşılık gelir. RematchComparisonActivity zaten left+right topluyor; oyuncu bazlı toplam isteniyorsa ileride ayrı bir geliştirme yapılabilir (bu yamanın kapsamı dışında).
- **Mevcut kayıtlı veriler:** Bu yama Tur 2 davranışını değiştirir, ancak veritabanında saklanan zar dizilerini değiştirmez. Yamadan ÖNCE oynanmış Tur 2 maçlarındaki kayıtlar yine hatalı (aynı zar) kalır; yeni başlayan rövanşlar düzgün çalışır.

---

## SON GÜNCELLEME
- **Tarih:** 2026-04-27
- **Durum:** Patch hazır, uygulanmaya bekliyor
- **Tahmini Build & Test Süresi:** 5-10 dk (Android Studio'da)
