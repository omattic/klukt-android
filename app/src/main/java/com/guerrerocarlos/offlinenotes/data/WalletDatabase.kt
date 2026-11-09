package com.guerrerocarlos.offlinenotes.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes", primaryKeys = ["noteId"])
data class NoteEntity(val noteId: String, val json: String, val held: Boolean, val receivedAt: Long)

@Dao interface NoteDao {
    @Query("SELECT * FROM notes WHERE held = 1 ORDER BY receivedAt DESC") fun held(): Flow<List<NoteEntity>>
    @Query("SELECT * FROM notes WHERE noteId = :id") suspend fun byId(id: String): NoteEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(note: NoteEntity)
    @Query("UPDATE notes SET held = 0 WHERE noteId = :id AND held = 1") suspend fun markTransferred(id: String): Int
}

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class WalletDatabase : RoomDatabase() { companion object {
    fun open(context: Context) = Room.databaseBuilder(context, WalletDatabase::class.java, "offline-notes.db").build()
} abstract fun notes(): NoteDao }
