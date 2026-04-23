package com.tavla.tavlapp

/**
 * Hareketler Dokumu (Activity Log) veri sinifi
 * Programda yapilan tum islemleri saat ve dakika olarak kaydeder
 */
data class ActivityLog(
    val id: Long = 0,
    val timestamp: String,        // Saat:Dakika:Saniye formatinda
    val dateTime: String,         // Tam tarih ve saat (yyyy-MM-dd HH:mm:ss)
    val actionType: String,       // Islem tipi (APP_OPEN, GAME_START, SCORE_ADD, DOUBLE_OFFER, etc.)
    val description: String,      // Islemin detayli aciklamasi
    val player1Name: String? = null,  // Oyuncu 1 adi (varsa)
    val player2Name: String? = null,  // Oyuncu 2 adi (varsa)
    val matchId: Long? = null,    // Mac ID (varsa)
    val extraData: String? = null // Ek bilgiler (JSON formatinda)
)

/**
 * Islem tipleri - Hareketler Dokumunde kullanilacak sabit degerler
 */
object ActionTypes {
    // Uygulama islemleri
    const val APP_OPEN = "APP_OPEN"                    // Program acildi
    const val APP_CLOSE = "APP_CLOSE"                  // Program kapatildi

    // Oyun ayarlari islemleri
    const val SETTINGS_PLAYER1_SELECT = "SETTINGS_PLAYER1_SELECT"    // Oyuncu 1 secildi
    const val SETTINGS_PLAYER2_SELECT = "SETTINGS_PLAYER2_SELECT"    // Oyuncu 2 secildi
    const val SETTINGS_GAME_TYPE = "SETTINGS_GAME_TYPE"              // Oyun tipi secildi
    const val SETTINGS_ROUNDS = "SETTINGS_ROUNDS"                    // El sayisi secildi
    const val SETTINGS_SCORE_MODE = "SETTINGS_SCORE_MODE"            // Skor modu degistirildi
    const val SETTINGS_DICE_ROLLER = "SETTINGS_DICE_ROLLER"          // Zar atici ayari degistirildi
    const val SETTINGS_TIMER = "SETTINGS_TIMER"                      // Sure tutucu ayari degistirildi
    const val SETTINGS_STATISTICS = "SETTINGS_STATISTICS"            // Istatistik ayari degistirildi
    const val SETTINGS_DICE_EVAL = "SETTINGS_DICE_EVAL"              // Zar degerlendirme ayari degistirildi
    const val NEW_PLAYER_ADDED = "NEW_PLAYER_ADDED"                  // Yeni oyuncu eklendi

    // Oyun islemleri
    const val GAME_START = "GAME_START"               // Oyun basladi
    const val GAME_END = "GAME_END"                   // Mac bitti
    const val ROUND_END = "ROUND_END"                 // El bitti

    // Skor islemleri
    const val SCORE_SINGLE = "SCORE_SINGLE"           // Tek sayi (1 puan)
    const val SCORE_MARS = "SCORE_MARS"               // Mars (2 puan)
    const val SCORE_BACKGAMMON = "SCORE_BACKGAMMON"   // Backgammon (3 puan)
    const val SCORE_UNDO = "SCORE_UNDO"               // Skor geri alindi

    // Katlama islemleri
    const val DOUBLE_OFFER = "DOUBLE_OFFER"           // Katlama teklifi yapildi
    const val DOUBLE_ACCEPT = "DOUBLE_ACCEPT"         // Katlama kabul edildi
    const val DOUBLE_REJECT = "DOUBLE_REJECT"         // Katlama reddedildi (pes)
    const val DOUBLE_CANCEL = "DOUBLE_CANCEL"         // Katlama iptal edildi

    // Zar islemleri
    const val DICE_ROLL = "DICE_ROLL"                 // Zar atildi
    const val DICE_SCREEN_OPEN = "DICE_SCREEN_OPEN"   // Zar ekrani acildi
    const val DICE_SCREEN_CLOSE = "DICE_SCREEN_CLOSE" // Zar ekrani kapatildi

    // Timer islemleri
    const val TIMER_START = "TIMER_START"             // Zamanlayici basladi
    const val TIMER_PAUSE = "TIMER_PAUSE"             // Zamanlayici durakladi
    const val TIMER_RESET = "TIMER_RESET"             // Zamanlayici sifirlandi
    const val TIMER_PLAYER_SWITCH = "TIMER_PLAYER_SWITCH" // Oyuncu degisti

    // Diger islemler
    const val MATCH_END_CONFIRM = "MATCH_END_CONFIRM" // Mac bitirme onaylandi
    const val MATCH_END_CANCEL = "MATCH_END_CANCEL"   // Mac bitirme iptal edildi
    const val DATA_RESET = "DATA_RESET"               // Tum veriler sifirlandi

    // Rovansli Karsilasma islemleri
    const val REMATCH_ENCOUNTER_CREATE = "REMATCH_ENCOUNTER_CREATE"   // Rovansli karsilasma olusturuldu
    const val REMATCH_DICE_GENERATE = "REMATCH_DICE_GENERATE"         // Zar setleri uretildi
    const val REMATCH_DICE_REVERSE = "REMATCH_DICE_REVERSE"           // Tur 2'de aynı setler ters oynatıldı
    const val REMATCH_MATCH_START = "REMATCH_MATCH_START"             // Rovansli mac basladi
    const val REMATCH_MATCH_END = "REMATCH_MATCH_END"                 // Rovansli mac bitti
    const val REMATCH_ROUND_COMPLETE = "REMATCH_ROUND_COMPLETE"       // Rovansli tur tamamlandi
    const val REMATCH_ENCOUNTER_COMPLETE = "REMATCH_ENCOUNTER_COMPLETE" // Rovansli karsilasma tamamlandi
    const val REMATCH_ENCOUNTER_CANCEL = "REMATCH_ENCOUNTER_CANCEL"   // Rovansli karsilasma iptal edildi
    const val REMATCH_DICE_CONFIRM = "REMATCH_DICE_CONFIRM"           // Zar onayi
    const val REMATCH_PIP_ENTRY = "REMATCH_PIP_ENTRY"                 // Pip sayisi girildi
}
