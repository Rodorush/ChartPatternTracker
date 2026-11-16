package br.com.rodorush.chartpatterntracker.model

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Cache local de velas. Não guarda nada que o Firestore ou a Brapi não devolvam de novo, então
 * mudança de versão descarta as tabelas em vez de migrar (`di/AppModule.kt`).
 *
 * O schema da versão 2 é o mesmo da 1. O número existe para descartar as linhas em que
 * `lastUpdated` é o instante da gravação no Room, e não o da leitura na Brapi: elas dariam por
 * fechada uma vela copiada do Firestore ainda em formação.
 */
@Database(entities = [CandlestickEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun candlestickDao(): CandlestickDao
}