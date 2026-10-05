package com.example.pregon.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QueryDao {
    @Query("SELECT * FROM saved_queries ORDER BY createdAt DESC")
    fun getAllQueries(): Flow<List<QueryEntity>>

    @Query("SELECT * FROM saved_queries WHERE isPinnedToWidget = 1 LIMIT 1")
    fun getPinnedWidgetQuery(): Flow<QueryEntity?>

    @Query("SELECT * FROM saved_queries WHERE id = :id")
    suspend fun getQueryById(id: String): QueryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(query: QueryEntity)

    @Update
    suspend fun updateQuery(query: QueryEntity)

    @Query("DELETE FROM saved_queries WHERE id = :id")
    suspend fun deleteQueryById(id: String)

    @Query("UPDATE saved_queries SET isPinnedToWidget = 0")
    suspend fun unpinAll()

    @Query("UPDATE saved_queries SET isPinnedToWidget = 1 WHERE id = :id")
    suspend fun pinQuery(id: String)

    @Transaction
    suspend fun setPinnedQuery(id: String) {
        unpinAll()
        pinQuery(id)
    }
}
