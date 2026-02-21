package com.tavla.tavlapp.online

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Firebase servislerine erisim singleton'i.
 * Anonim kimlik dogrulama ve Realtime Database referanslari.
 */
object FirebaseManager {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance().also {
            it.setPersistenceEnabled(true)
        }
    }

    /** Mevcut kullanici */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** Kullanici UID'si */
    val uid: String?
        get() = auth.currentUser?.uid

    /** Rooms referansi */
    fun roomsRef() = database.reference.child("rooms")

    /** Belirli bir oda referansi */
    fun roomRef(roomCode: String) = roomsRef().child(roomCode)

    /**
     * Anonim olarak giris yapar.
     * Zaten giris yapilmissa mevcut kullaniciyi doner.
     */
    suspend fun signInAnonymously(): FirebaseUser {
        val existingUser = auth.currentUser
        if (existingUser != null) return existingUser

        val result = auth.signInAnonymously().await()
        return result.user ?: throw Exception("Anonim giris basarisiz")
    }

    /**
     * Baglanti durumunu izler.
     * Firebase .info/connected ozel yolu.
     */
    fun connectedRef() = database.getReference(".info/connected")

    /**
     * Cikis yapar.
     */
    fun signOut() {
        auth.signOut()
    }
}
