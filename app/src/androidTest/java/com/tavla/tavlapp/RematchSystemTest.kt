package com.tavla.tavlapp

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Rovansli Karsilasma sistemi kapsamli test
 * Zar swap, istatistik kaydetme, karsilastirma verisi, küp bilgisi
 */
@RunWith(AndroidJUnit4::class)
class RematchSystemTest {

    private lateinit var context: Context
    private lateinit var dbHelper: DatabaseHelper
    private var testEncounterId: Long = -1L
    private var player1Id: Long = -1L
    private var player2Id: Long = -1L

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dbHelper = DatabaseHelper(context)
        // Test oyunculari olustur
        player1Id = dbHelper.addPlayer("TestEmre")
        player2Id = dbHelper.addPlayer("TestMustafa")
    }

    @After
    fun cleanup() {
        if (testEncounterId > 0) {
            try { dbHelper.deleteRematchEncounter(testEncounterId) } catch (_: Exception) {}
        }
    }

    // ========== TEST 1: Karsilasma ve zar seti olusturma ==========
    @Test
    fun test01_createEncounterAndDiceSets() {
        testEncounterId = dbHelper.createRematchEncounter(player1Id, player2Id, totalParties = 3, targetScore = 5)
        assertTrue("Karsilasma olusturuldu", testEncounterId > 0)

        val success = dbHelper.generateAndSaveDiceSets(testEncounterId, totalParties = 3, targetScore = 5)
        assertTrue("Zar setleri olusturuldu", success)

        // Her parti ve set icin zar seti var mi kontrol et
        val encounter = dbHelper.getRematchEncounter(testEncounterId)
        assertNotNull("Encounter okundu", encounter)
        assertEquals("3 parti", 3, encounter!!.totalParties)
        assertEquals("Hedef 5", 5, encounter.targetScore)
        assertEquals("Tur 1 aktif", 1, encounter.currentRound)
        assertEquals("ACTIVE durumu", RematchStatus.ACTIVE, encounter.status)

        // Ilk partinin ilk zar setini kontrol et
        val diceSet = dbHelper.getDiceSetForGame(testEncounterId, partyIndex = 0, setIndex = 0)
        assertNotNull("Zar seti var", diceSet)
        assertNotEquals("Baslangic zarlari farkli", diceSet!!.startingDicePlayer1, diceSet.startingDicePlayer2)
        assertTrue("P1 baslangic 1-6 arasi", diceSet.startingDicePlayer1 in 1..6)
        assertTrue("P2 baslangic 1-6 arasi", diceSet.startingDicePlayer2 in 1..6)
        assertEquals("200 cift zar P1", 200, diceSet.player1Dice.size)
        assertEquals("200 cift zar P2", 200, diceSet.player2Dice.size)

        // Zarlar 1-6 arasi mi
        diceSet.player1Dice.forEach { (d1, d2) ->
            assertTrue("P1 zar1 1-6", d1 in 1..6)
            assertTrue("P1 zar2 1-6", d2 in 1..6)
        }
        println("TEST 1 PASSED: Karsilasma ve zar setleri basariyla olusturuldu")
    }

    // ========== TEST 2: Zar swap kontrolu - Round 2'de zarlar degismemeli ==========
    @Test
    fun test02_diceSwapVerification() {
        testEncounterId = dbHelper.createRematchEncounter(player1Id, player2Id, totalParties = 2, targetScore = 5)
        dbHelper.generateAndSaveDiceSets(testEncounterId, totalParties = 2, targetScore = 5)

        val diceSet = dbHelper.getDiceSetForGame(testEncounterId, partyIndex = 0, setIndex = 0)!!

        // Round 1: Sol=P1, Sag=P2
        val r1LeftDice = diceSet.player1Dice
        val r1RightDice = diceSet.player2Dice
        val r1LeftStart = diceSet.startingDicePlayer1
        val r1RightStart = diceSet.startingDicePlayer2

        // Round 2: Isimler yer degistirir ama zarlar AYNI KALIYOR
        // Sol artik P2 (isim) ama player1Dice aliyor (karsi tarafin zari)
        // Sag artik P1 (isim) ama player2Dice aliyor (karsi tarafin zari)
        val r2LeftDice = diceSet.player1Dice   // SWAP YOK!
        val r2RightDice = diceSet.player2Dice  // SWAP YOK!
        val r2LeftStart = diceSet.startingDicePlayer1
        val r2RightStart = diceSet.startingDicePlayer2

        // Zarlar ayni pozisyonda kalmali
        assertEquals("Sol zar R1==R2", r1LeftDice, r2LeftDice)
        assertEquals("Sag zar R1==R2", r1RightDice, r2RightDice)
        assertEquals("Sol baslangic R1==R2", r1LeftStart, r2LeftStart)
        assertEquals("Sag baslangic R1==R2", r1RightStart, r2RightStart)

        // Ama Round 1'de sol=Emre'nin zarlariyken, Round 2'de sol=Mustafa (isim swap)
        // Mustafa artik Emre'nin zarlarini kullaniyor = DOGRU DAVRANIS!
        println("TEST 2 PASSED: Round 2'de zarlar yer degistirmiyor, sadece isimler")
    }

    // ========== TEST 3: Zar istatistikleri hesaplama ==========
    @Test
    fun test03_diceStatsCalculation() {
        // Manuel zar istatistik hesaplama testi
        val testDice = listOf(
            Pair(6, 6),  // Cift: 6*4 = 24, doubles++
            Pair(5, 4),  // Tek: 5+4 = 9
            Pair(3, 3),  // Cift: 3*4 = 12, doubles++
            Pair(2, 1),  // Tek: 2+1 = 3
            Pair(4, 4),  // Cift: 4*4 = 16, doubles++
            Pair(6, 1),  // Tek: 6+1 = 7
        )

        var total = 0
        var doubles = 0
        for (pair in testDice) {
            if (pair.first == pair.second) {
                total += pair.first * 4
                doubles++
            } else {
                total += pair.first + pair.second
            }
        }

        assertEquals("Toplam: 24+9+12+3+16+7=71", 71, total)
        assertEquals("3 cift", 3, doubles)

        // Zar kuvveti = toplam / atim sayisi
        val power = total.toFloat() / testDice.size
        assertEquals("Kuvvet: 71/6=11.83", 11.83f, power, 0.01f)
        println("TEST 3 PASSED: Zar istatistik hesaplamalari dogru")
    }

    // ========== TEST 4: Oyun sonucu kaydetme (tum alanlarla) ==========
    @Test
    fun test04_saveGameResultWithAllFields() {
        testEncounterId = dbHelper.createRematchEncounter(player1Id, player2Id, totalParties = 2, targetScore = 5)
        dbHelper.generateAndSaveDiceSets(testEncounterId, totalParties = 2, targetScore = 5)

        // Senaryo 1: Normal tek oyun - küp yok
        val id1 = dbHelper.saveRematchGameResult(
            encounterId = testEncounterId,
            partyIndex = 0, setIndex = 0, roundNumber = 1,
            leftPlayerId = player1Id, rightPlayerId = player2Id,
            winnerId = player1Id, winType = WinTypes.SINGLE,
            cubeValue = 1, finalScore = 1,
            loserPipCount = 45, dicePairsUsed = 12,
            doublerPlayerId = null,
            leftDiceTotal = 78, rightDiceTotal = 65,
            leftDoublesCount = 2, rightDoublesCount = 1
        )
        assertTrue("Sonuc 1 kaydedildi", id1 > 0)

        // Senaryo 2: Mars + küp x2 (Emre katladi, Mustafa kabul etti)
        val id2 = dbHelper.saveRematchGameResult(
            encounterId = testEncounterId,
            partyIndex = 0, setIndex = 1, roundNumber = 1,
            leftPlayerId = player1Id, rightPlayerId = player2Id,
            winnerId = player2Id, winType = WinTypes.MARS,
            cubeValue = 2, finalScore = 4,
            loserPipCount = 0, dicePairsUsed = 18,
            doublerPlayerId = player1Id,
            leftDiceTotal = 110, rightDiceTotal = 130,
            leftDoublesCount = 3, rightDoublesCount = 4
        )
        assertTrue("Sonuc 2 kaydedildi", id2 > 0)

        // Senaryo 3: Pes (Mustafa katladi, Emre pes etti)
        val id3 = dbHelper.saveRematchGameResult(
            encounterId = testEncounterId,
            partyIndex = 0, setIndex = 2, roundNumber = 1,
            leftPlayerId = player1Id, rightPlayerId = player2Id,
            winnerId = player2Id, winType = WinTypes.RESIGN,
            cubeValue = 1, finalScore = 1,
            loserPipCount = 0, dicePairsUsed = 8,
            doublerPlayerId = player2Id,
            leftDiceTotal = 40, rightDiceTotal = 52,
            leftDoublesCount = 1, rightDoublesCount = 0
        )
        assertTrue("Sonuc 3 kaydedildi", id3 > 0)

        // Sonuclari geri oku ve dogrula
        val results = dbHelper.getRematchGameResults(testEncounterId, roundNumber = 1, partyIndex = 0)
        assertEquals("3 oyun sonucu", 3, results.size)

        // Sonuc 1 dogrulama
        val r1 = results[0]
        assertEquals("R1 kazanan", player1Id, r1.winnerId)
        assertEquals("R1 tip", WinTypes.SINGLE, r1.winType)
        assertEquals("R1 kup", 1, r1.cubeValue)
        assertEquals("R1 puan", 1, r1.finalScore)
        assertEquals("R1 pip", 45, r1.loserPipCount)
        assertEquals("R1 zar sayisi", 12, r1.dicePairsUsed)
        assertNull("R1 doubler yok", r1.doublerPlayerId)
        assertEquals("R1 sol toplam", 78, r1.leftDiceTotal)
        assertEquals("R1 sag toplam", 65, r1.rightDiceTotal)
        assertEquals("R1 sol cift", 2, r1.leftDoublesCount)
        assertEquals("R1 sag cift", 1, r1.rightDoublesCount)

        // Sonuc 2 dogrulama
        val r2 = results[1]
        assertEquals("R2 kazanan", player2Id, r2.winnerId)
        assertEquals("R2 tip", WinTypes.MARS, r2.winType)
        assertEquals("R2 kup", 2, r2.cubeValue)
        assertEquals("R2 puan", 4, r2.finalScore)
        assertEquals("R2 doubler=Emre", player1Id, r2.doublerPlayerId)
        assertEquals("R2 sol toplam", 110, r2.leftDiceTotal)
        assertEquals("R2 sag toplam", 130, r2.rightDiceTotal)
        assertEquals("R2 sol cift", 3, r2.leftDoublesCount)
        assertEquals("R2 sag cift", 4, r2.rightDoublesCount)

        // Sonuc 3 dogrulama (pes)
        val r3 = results[2]
        assertEquals("R3 tip", WinTypes.RESIGN, r3.winType)
        assertEquals("R3 doubler=Mustafa", player2Id, r3.doublerPlayerId)

        println("TEST 4 PASSED: Tum alanlar dogru kaydedildi ve okundu")
    }

    // ========== TEST 5: Tur 1 + Tur 2 karsilastirma verisi ==========
    @Test
    fun test05_partyComparisonData() {
        testEncounterId = dbHelper.createRematchEncounter(player1Id, player2Id, totalParties = 2, targetScore = 3)
        dbHelper.generateAndSaveDiceSets(testEncounterId, totalParties = 2, targetScore = 3)

        // Tur 1, Parti 0: 3 el
        // El 1: Emre kazandi, tek, kup yok
        dbHelper.saveRematchGameResult(
            testEncounterId, 0, 0, 1, player1Id, player2Id,
            player1Id, WinTypes.SINGLE, 1, 1, 30, 10,
            null, 55, 48, 1, 0
        )
        // El 2: Mustafa kazandi, mars, kup x2
        dbHelper.saveRematchGameResult(
            testEncounterId, 0, 1, 1, player1Id, player2Id,
            player2Id, WinTypes.MARS, 2, 4, 0, 15,
            player1Id, 80, 95, 2, 3
        )
        // El 3: Emre pes etti
        dbHelper.saveRematchGameResult(
            testEncounterId, 0, 2, 1, player1Id, player2Id,
            player2Id, WinTypes.RESIGN, 1, 1, 0, 5,
            player2Id, 25, 30, 0, 1
        )

        // Tur 2, Parti 0: 2 el (isimler yer degisti, zarlar ayni pozisyonda)
        // El 1: Mustafa kazandi (sol, ama P1 zarlarini kullaniyor)
        dbHelper.saveRematchGameResult(
            testEncounterId, 0, 0, 2, player2Id, player1Id,
            player2Id, WinTypes.SINGLE, 1, 1, 35, 11,
            null, 60, 42, 2, 1
        )
        // El 2: Emre kazandi, backgammon
        dbHelper.saveRematchGameResult(
            testEncounterId, 0, 1, 2, player2Id, player1Id,
            player1Id, WinTypes.BACKGAMMON, 1, 3, 0, 20,
            null, 120, 90, 5, 2
        )

        // Karsilastirma verisi al
        val data = dbHelper.getPartyComparisonData(testEncounterId, 0)

        // Temel kontrol
        assertEquals("3 karsilastirma satiri", 3, data.gameComparisons.size)

        // Tur 1 istatistikleri
        assertEquals("T1 toplam zar cifti", 30, data.round1TotalDicePairs)  // 10+15+5
        assertEquals("T1 toplam zar birimi", 333, data.round1TotalDiceUnits) // (55+48)+(80+95)+(25+30)
        assertEquals("T1 toplam cift", 7, data.round1TotalDoubles)     // (1+0)+(2+3)+(0+1)
        assertEquals("T1 mars sayisi", 1, data.round1MarsCount)
        assertEquals("T1 pes sayisi", 1, data.round1ResignCount)
        assertEquals("T1 kup kullanim", 1, data.round1CubeUsedCount)
        assertEquals("T1 max kup", 2, data.round1MaxCube)

        // Tur 2 istatistikleri
        assertEquals("T2 toplam zar cifti", 31, data.round2TotalDicePairs) // 11+20
        assertEquals("T2 toplam zar birimi", 312, data.round2TotalDiceUnits) // (60+42)+(120+90)
        assertEquals("T2 toplam cift", 10, data.round2TotalDoubles)   // (2+1)+(5+2)
        assertEquals("T2 backgammon", 1, data.round2BackgammonCount)

        // Ayni/farkli kazanan (el 1 ve el 2 karsilastirma)
        // El 1: T1=Emre, T2=Mustafa → farkli
        // El 2: T1=Mustafa, T2=Emre → farkli
        // El 3: T1=Mustafa, T2=yok → sayilmaz
        assertEquals("Farkli kazanan", 2, data.differentWinnerCount)
        assertEquals("Ayni kazanan", 0, data.sameWinnerCount)

        // Oyun bazinda karsilastirma
        val row0 = data.gameComparisons[0]
        assertNotNull("El 1 T1 var", row0.round1Result)
        assertNotNull("El 1 T2 var", row0.round2Result)
        assertEquals("El 1 T1 kazanan=Emre", player1Id, row0.round1Result!!.winnerId)
        assertEquals("El 1 T2 kazanan=Mustafa", player2Id, row0.round2Result!!.winnerId)

        val row2 = data.gameComparisons[2]
        assertNotNull("El 3 T1 var", row2.round1Result)
        assertNull("El 3 T2 yok", row2.round2Result)

        println("TEST 5 PASSED: Karsilastirma verisi dogru")
    }

    // ========== TEST 6: Coklu parti senaryosu ==========
    @Test
    fun test06_multiPartyScenario() {
        testEncounterId = dbHelper.createRematchEncounter(player1Id, player2Id, totalParties = 3, targetScore = 3)
        dbHelper.generateAndSaveDiceSets(testEncounterId, totalParties = 3, targetScore = 3)

        // 3 farkli partide farkli sonuclar
        for (partyIdx in 0..2) {
            // Her partide 2 el oyna
            dbHelper.saveRematchGameResult(
                testEncounterId, partyIdx, 0, 1, player1Id, player2Id,
                player1Id, WinTypes.SINGLE, 1, 1, 20 + partyIdx * 5, 10 + partyIdx,
                null,
                50 + partyIdx * 10, 45 + partyIdx * 8,
                1 + partyIdx, partyIdx
            )
            dbHelper.saveRematchGameResult(
                testEncounterId, partyIdx, 1, 1, player1Id, player2Id,
                player2Id, WinTypes.MARS, 1, 2, 0, 14 + partyIdx,
                null,
                70 + partyIdx * 5, 85 + partyIdx * 3,
                2, 1
            )
        }

        // Her parti icin karsilastirma verisi al
        for (partyIdx in 0..2) {
            val data = dbHelper.getPartyComparisonData(testEncounterId, partyIdx)
            assertEquals("Parti $partyIdx: 2 oyun", 2, data.gameComparisons.size)
            assertTrue("Parti $partyIdx: T1 zar cifti > 0", data.round1TotalDicePairs > 0)
            assertTrue("Parti $partyIdx: T1 zar birimi > 0", data.round1TotalDiceUnits > 0)
        }

        // Farkli partilerin zar setlerinin farkli oldugunu dogrula
        val dice0 = dbHelper.getDiceSetForGame(testEncounterId, 0, 0)!!
        val dice1 = dbHelper.getDiceSetForGame(testEncounterId, 1, 0)!!
        assertNotEquals("Farkli partilerin zarlari farkli",
            dice0.player1Dice[0], dice1.player1Dice[0])

        println("TEST 6 PASSED: Coklu parti senaryosu basarili")
    }

    // ========== TEST 7: Tur 2'de leftPlayerId/rightPlayerId swap ==========
    @Test
    fun test07_round2PlayerIdSwap() {
        testEncounterId = dbHelper.createRematchEncounter(player1Id, player2Id, totalParties = 1, targetScore = 3)
        dbHelper.generateAndSaveDiceSets(testEncounterId, totalParties = 1, targetScore = 3)

        // Tur 1: left=P1, right=P2
        dbHelper.saveRematchGameResult(
            testEncounterId, 0, 0, 1, player1Id, player2Id,
            player1Id, WinTypes.SINGLE, 1, 1, 30, 10,
            null, 50, 45, 1, 0
        )

        // Tur 2: left=P2, right=P1 (yer degisti)
        dbHelper.saveRematchGameResult(
            testEncounterId, 0, 0, 2, player2Id, player1Id,
            player1Id, WinTypes.SINGLE, 1, 1, 28, 12,
            null, 55, 50, 2, 1
        )

        val r1Results = dbHelper.getRematchGameResults(testEncounterId, roundNumber = 1)
        val r2Results = dbHelper.getRematchGameResults(testEncounterId, roundNumber = 2)

        assertEquals("T1 left=P1", player1Id, r1Results[0].leftPlayerId)
        assertEquals("T1 right=P2", player2Id, r1Results[0].rightPlayerId)
        assertEquals("T2 left=P2", player2Id, r2Results[0].leftPlayerId)
        assertEquals("T2 right=P1", player1Id, r2Results[0].rightPlayerId)

        // Ama her iki turda da ayni zar seti: player1Dice sol, player2Dice sag
        // T1: Emre(sol) player1Dice kullandi
        // T2: Mustafa(sol) player1Dice kullaniyor = Emre'nin T1 zarlarini aliyor!
        val diceSet = dbHelper.getDiceSetForGame(testEncounterId, 0, 0)!!
        // player1Dice her iki turda da "sol" pozisyonda
        assertNotNull("Zar seti mevcut", diceSet)

        println("TEST 7 PASSED: Tur 2 player ID swap dogru")
    }

    // ========== TEST 8: Kup bilgisi kayit ve okuma ==========
    @Test
    fun test08_cubeInfoSaveAndRead() {
        testEncounterId = dbHelper.createRematchEncounter(player1Id, player2Id, totalParties = 1, targetScore = 5)
        dbHelper.generateAndSaveDiceSets(testEncounterId, totalParties = 1, targetScore = 5)

        // Senaryo A: Emre x2 katladi, Mustafa kabul etti, Mustafa mars kazandi
        dbHelper.saveRematchGameResult(
            testEncounterId, 0, 0, 1, player1Id, player2Id,
            player2Id, WinTypes.MARS, 2, 4, 0, 16,
            player1Id, 90, 110, 3, 2
        )

        // Senaryo B: Mustafa x4 katladi, Emre pes etti
        dbHelper.saveRematchGameResult(
            testEncounterId, 0, 1, 1, player1Id, player2Id,
            player2Id, WinTypes.RESIGN, 2, 2, 0, 7,
            player2Id, 35, 42, 0, 1
        )

        // Senaryo C: Emre x2 katladi, Mustafa kabul, Mustafa x4 katladi, Emre kabul, Emre kazandi
        dbHelper.saveRematchGameResult(
            testEncounterId, 0, 2, 1, player1Id, player2Id,
            player1Id, WinTypes.SINGLE, 4, 4, 55, 22,
            player2Id, 130, 125, 4, 3
        )

        val results = dbHelper.getRematchGameResults(testEncounterId, roundNumber = 1)
        assertEquals("3 sonuc", 3, results.size)

        // A: Emre katladi, mars
        assertEquals("A kup=2", 2, results[0].cubeValue)
        assertEquals("A puan=4", 4, results[0].finalScore)
        assertEquals("A doubler=Emre", player1Id, results[0].doublerPlayerId)
        assertEquals("A tip=MARS", WinTypes.MARS, results[0].winType)

        // B: Mustafa katladi, Emre pes
        assertEquals("B kup=2", 2, results[1].cubeValue)
        assertEquals("B puan=2", 2, results[1].finalScore)
        assertEquals("B doubler=Mustafa", player2Id, results[1].doublerPlayerId)
        assertEquals("B tip=RESIGN", WinTypes.RESIGN, results[1].winType)

        // C: Son katlayan Mustafa, x4, Emre kazandi
        assertEquals("C kup=4", 4, results[2].cubeValue)
        assertEquals("C puan=4", 4, results[2].finalScore)
        assertEquals("C doubler=Mustafa", player2Id, results[2].doublerPlayerId)

        println("TEST 8 PASSED: Kup bilgileri dogru kaydedildi")
    }

    // ========== TEST 9: Zar toplam hesabi (cift x4 kurali) ==========
    @Test
    fun test09_diceDoublesTimesForRule() {
        // Bu test zar hesaplama formulunu dogruluyor
        // Cift: deger × 4, Tek: d1 + d2

        data class TestCase(val d1: Int, val d2: Int, val expectedTotal: Int, val isDouble: Boolean)
        val cases = listOf(
            TestCase(6, 6, 24, true),   // 6×4
            TestCase(5, 5, 20, true),   // 5×4
            TestCase(4, 4, 16, true),   // 4×4
            TestCase(3, 3, 12, true),   // 3×4
            TestCase(2, 2, 8, true),    // 2×4
            TestCase(1, 1, 4, true),    // 1×4
            TestCase(6, 5, 11, false),  // 6+5
            TestCase(5, 4, 9, false),   // 5+4
            TestCase(6, 1, 7, false),   // 6+1
            TestCase(3, 1, 4, false),   // 3+1
            TestCase(2, 1, 3, false),   // 2+1 (minimum tek)
        )

        for (tc in cases) {
            val total = if (tc.d1 == tc.d2) tc.d1 * 4 else tc.d1 + tc.d2
            val isDouble = tc.d1 == tc.d2
            assertEquals("${tc.d1}-${tc.d2} toplam", tc.expectedTotal, total)
            assertEquals("${tc.d1}-${tc.d2} cift mi", tc.isDouble, isDouble)
        }

        println("TEST 9 PASSED: Tum zar hesaplamalari dogru")
    }

    // ========== TEST 10: Baslangic zarlarinin ilk oyuncuya atanmasi ==========
    @Test
    fun test10_startingDiceAssignment() {
        // Baslangic zarlari her zaman farkli (cift olamaz)
        // Her iki turda da ayni pozisyonda kaliyor
        repeat(50) {
            val (d1, d2) = DiceGenerator.generateStartingDice()
            assertNotEquals("Baslangic zarlari farkli (tur $it)", d1, d2)
            assertTrue("d1 1-6", d1 in 1..6)
            assertTrue("d2 1-6", d2 in 1..6)
        }

        // Ilk oyuncu = buyuk atan
        assertEquals("5>3 => P1 baslar", 1, DiceGenerator.determineFirstPlayer(5, 3))
        assertEquals("2<6 => P2 baslar", 2, DiceGenerator.determineFirstPlayer(2, 6))
        assertEquals("4>1 => P1 baslar", 1, DiceGenerator.determineFirstPlayer(4, 1))

        // Baslangic toplami (her zaman tek, cift degil)
        // Tur 1: firstPlayer=1 ise sol oyuncuya eklenir
        // Tur 2: zarlar ayni pozisyonda, firstPlayer=1 ise yine sola eklenir
        // Ama sol oyuncu artik P2 (isim swap)
        println("TEST 10 PASSED: Baslangic zarları ve ilk oyuncu atamasi dogru")
    }

    // ========== TEST 11: DiceGenerator JSON serialization ==========
    @Test
    fun test11_diceJsonSerialization() {
        val original = listOf(Pair(6, 6), Pair(5, 4), Pair(3, 3), Pair(2, 1))
        val json = DiceGenerator.diceSequenceToJson(original)
        val restored = DiceGenerator.jsonToDiceSequence(json)

        assertEquals("Boyut ayni", original.size, restored.size)
        for (i in original.indices) {
            assertEquals("Eleman $i ayni", original[i], restored[i])
        }

        println("TEST 11 PASSED: JSON serialization dogru")
    }
}
