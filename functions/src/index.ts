import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.database();

/**
 * Sunucu tarafinda zar uretimi.
 * Guvenligi saglamak icin zarlar sunucuda uretilir.
 *
 * @param roomCode - Oda kodu
 * @param type - "starting" (baslangic zari) veya "regular" (normal zar)
 */
export const rollDice = functions.https.onCall(async (data, context) => {
  // Kimlik dogrulama kontrolu
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Zar atmak icin giris yapmaniz gerekiyor"
    );
  }

  const { roomCode, type } = data;

  if (!roomCode || !type) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "roomCode ve type parametreleri gerekli"
    );
  }

  const roomRef = db.ref(`rooms/${roomCode}`);
  const roomSnapshot = await roomRef.once("value");

  if (!roomSnapshot.exists()) {
    throw new functions.https.HttpsError("not-found", "Oda bulunamadi");
  }

  const room = roomSnapshot.val();
  const uid = context.auth.uid;

  // Oyuncunun bu odada olup olmadigini kontrol et
  const isWhite = room.players?.white?.uid === uid;
  const isBlack = room.players?.black?.uid === uid;

  if (!isWhite && !isBlack) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Bu odada oyuncu degilsiniz"
    );
  }

  if (type === "starting") {
    return await rollStartingDice(roomRef, room, uid, isWhite);
  } else if (type === "regular") {
    return await rollRegularDice(roomRef, room, uid, isWhite);
  } else {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Gecersiz zar tipi: " + type
    );
  }
});

/**
 * Baslangic zari - her oyuncu 1 zar atar, buyuk atan baslar.
 * Esit gelirse tekrar atilir.
 */
async function rollStartingDice(
  roomRef: admin.database.Reference,
  room: any,
  uid: string,
  isWhite: boolean
) {
  const die = randomDie();
  const playerKey = isWhite ? "white" : "black";

  // Oyuncunun zarini yaz
  await roomRef
    .child(`currentGame/startingDice/${playerKey}`)
    .set(die);

  // Her iki oyuncu da zar attiysa sonucu belirle
  const startingDiceSnap = await roomRef
    .child("currentGame/startingDice")
    .once("value");
  const startingDice = startingDiceSnap.val();

  if (startingDice?.white && startingDice?.black) {
    const whiteDie = startingDice.white;
    const blackDie = startingDice.black;

    if (whiteDie !== blackDie) {
      // Buyuk atan baslar
      const firstPlayer = whiteDie > blackDie ? "white" : "black";
      await roomRef.child("currentGame/startingDice/firstPlayer").set(firstPlayer);
      await roomRef.child("currentGame/turn").set(firstPlayer);

      // Baslangic zarlarini normal zarlar olarak ayarla
      await roomRef.child("currentGame/dice/values").set([whiteDie, blackDie]);
      await roomRef.child("currentGame/status").set("playing");
    } else {
      // Esit - sifirla, tekrar atilacak
      await roomRef.child("currentGame/startingDice").set({
        white: null,
        black: null,
        firstPlayer: null,
      });
    }

    return {
      whiteDie,
      blackDie,
      firstPlayer:
        whiteDie !== blackDie
          ? whiteDie > blackDie
            ? "white"
            : "black"
          : null,
    };
  }

  return { die, waitingForOpponent: true };
}

/**
 * Normal zar atma - sira gelen oyuncu 2 zar atar.
 */
async function rollRegularDice(
  roomRef: admin.database.Reference,
  room: any,
  uid: string,
  isWhite: boolean
) {
  const currentTurn = room.currentGame?.turn;
  const myColor = isWhite ? "white" : "black";

  if (currentTurn !== myColor) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Sira sizde degil"
    );
  }

  const die1 = randomDie();
  const die2 = randomDie();

  await roomRef.child("currentGame/dice/values").set([die1, die2]);

  return { die1, die2 };
}

/**
 * 1-6 arasi rastgele zar degeri uretir.
 */
function randomDie(): number {
  return Math.floor(Math.random() * 6) + 1;
}

/**
 * Eski odalari temizler (30 dk'da bir calisir).
 * 2 saatten eski finished/abandoned odalari siler.
 */
export const cleanupRooms = functions.pubsub
  .schedule("every 30 minutes")
  .onRun(async () => {
    const cutoff = Date.now() - 2 * 60 * 60 * 1000; // 2 saat once
    const roomsRef = db.ref("rooms");
    const snapshot = await roomsRef
      .orderByChild("createdAt")
      .endAt(cutoff)
      .once("value");

    const updates: { [key: string]: null } = {};
    snapshot.forEach((child) => {
      const room = child.val();
      if (
        room.status === "finished" ||
        room.status === "abandoned" ||
        !room.players
      ) {
        updates[`rooms/${child.key}`] = null;
      }
    });

    if (Object.keys(updates).length > 0) {
      await db.ref().update(updates);
      console.log(`${Object.keys(updates).length} eski oda temizlendi`);
    }

    return null;
  });
