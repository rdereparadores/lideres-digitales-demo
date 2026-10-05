package com.example.pregon.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pregon.model.*

@Entity(tableName = "saved_queries")
data class QueryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val tool: String,
    val argsJson: String,
    val daysCsv: String,
    val fromMinutes: Int,
    val toMinutes: Int,
    val everyMin: Int,
    val onlyLective: Boolean,
    val headline: String,
    val subtitle: String,
    val status: String,
    val updatedAtDate: String,
    val updatedAtMinutes: Int,
    val createdAt: Long,
    val isPinnedToWidget: Boolean = false
) {
    fun toSavedQuery(): SavedQuery {
        val days = daysCsv.split(",").filter { it.isNotBlank() }
        val schedule = Schedule(
            days = if (days.isEmpty()) listOf("L", "M", "X", "J") else days,
            fromMinutes = fromMinutes,
            toMinutes = toMinutes,
            everyMin = everyMin,
            onlyLective = onlyLective
        )
        val latest = QueryLatest(
            title = title,
            headline = headline,
            subtitle = subtitle,
            status = status,
            updatedAt = SimTime(updatedAtDate, updatedAtMinutes)
        )
        return SavedQuery(
            id = id,
            title = title,
            tool = tool,
            argsJson = argsJson,
            schedule = schedule,
            lastResult = latest,
            createdAt = createdAt,
            isPinnedToWidget = isPinnedToWidget
        )
    }

    companion object {
        fun fromSavedQuery(q: SavedQuery): QueryEntity {
            return QueryEntity(
                id = q.id,
                title = q.title,
                tool = q.tool,
                argsJson = q.argsJson,
                daysCsv = q.schedule.days.joinToString(","),
                fromMinutes = q.schedule.fromMinutes,
                toMinutes = q.schedule.toMinutes,
                everyMin = q.schedule.everyMin,
                onlyLective = q.schedule.onlyLective,
                headline = q.lastResult.headline,
                subtitle = q.lastResult.subtitle,
                status = q.lastResult.status,
                updatedAtDate = q.lastResult.updatedAt.date,
                updatedAtMinutes = q.lastResult.updatedAt.minutes,
                createdAt = q.createdAt,
                isPinnedToWidget = q.isPinnedToWidget
            )
        }
    }
}
