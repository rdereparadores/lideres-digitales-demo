package com.example.pregon.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pregon.data.PregonDatabase
import com.example.pregon.data.QueryEntity
import com.example.pregon.mock.CalendarUtils
import com.example.pregon.mock.PregonEngine
import com.example.pregon.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PregonDatabase.getDatabase(application)
    private val queryDao = db.queryDao()
    val engine = PregonEngine()

    // Active Navigation Tab: 0 = Preguntar, 1 = Consultas, 2 = Ajustes
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Chat messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Home preset
    private val _currentHome = MutableStateFlow<NeighborhoodPreset>(HomePresets.DEFAULT_HOME)
    val currentHome: StateFlow<NeighborhoodPreset> = _currentHome.asStateFlow()

    // Demo state (clock, mode, scenarios)
    private val _demoState = MutableStateFlow(engine.demoState)
    val demoState: StateFlow<DemoState> = _demoState.asStateFlow()

    // Data mode
    private val _dataSourceMode = MutableStateFlow("simulated") // "simulated" | "real"
    val dataSourceMode: StateFlow<String> = _dataSourceMode.asStateFlow()

    private val _realBackendUrl = MutableStateFlow("http://10.0.2.2:8000")
    val realBackendUrl: StateFlow<String> = _realBackendUrl.asStateFlow()

    private val _connectionStatus = MutableStateFlow<String?>(null)
    val connectionStatus: StateFlow<String?> = _connectionStatus.asStateFlow()

    // Persistent Queries from Room
    val savedQueries: StateFlow<List<SavedQuery>> = queryDao.getAllQueries()
        .map { entities -> entities.map { it.toSavedQuery() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Pinned Widget Query
    val pinnedWidgetQuery: StateFlow<SavedQuery?> = queryDao.getPinnedWidgetQuery()
        .map { it?.toSavedQuery() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dialogs state
    private val _activeTrace = MutableStateFlow<List<TraceStep>?>(null)
    val activeTrace: StateFlow<List<TraceStep>?> = _activeTrace.asStateFlow()

    private val _activeSources = MutableStateFlow<List<Provenance>?>(null)
    val activeSources: StateFlow<List<Provenance>?> = _activeSources.asStateFlow()

    private val _activePersistOffer = MutableStateFlow<PersistOffer?>(null)
    val activePersistOffer: StateFlow<PersistOffer?> = _activePersistOffer.asStateFlow()

    init {
        // Initialize default query if database is empty
        viewModelScope.launch {
            val existing = queryDao.getAllQueries().first()
            if (existing.isEmpty()) {
                seedInitialQuery()
            }
        }
    }

    private suspend fun seedInitialQuery() {
        val simNow = engine.demoState.now
        val initialSchedule = Schedule(
            days = listOf("L", "M", "X", "J"),
            fromMinutes = 7 * 60,
            toMinutes = 8 * 60 + 45,
            everyMin = 5,
            onlyLective = true
        )
        val initialSaved = SavedQuery(
            title = "Salir hacia la Politécnica",
            tool = "estimar_hora_salida",
            argsJson = "{\"destino\":\"S19\",\"hora\":\"09:00\"}",
            schedule = initialSchedule,
            lastResult = QueryLatest(
                title = "Salir hacia la Politécnica",
                headline = "Sal a las 8:12",
                subtitle = "L1 desde Nuevo Cáceres · día normal · 92%",
                status = "ok",
                updatedAt = simNow,
                sources = ReferenceData.createSources(simNow).filter { it.datasetId in listOf("buses_flota", "buses_red", "trafico_caceres") }
            ),
            isPinnedToWidget = true
        )
        queryDao.insertQuery(QueryEntity.fromSavedQuery(initialSaved))
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        if (tabIndex == 1) {
            refreshAllQueries()
        }
    }

    fun setHomePreset(preset: NeighborhoodPreset) {
        _currentHome.value = preset
        refreshAllQueries()
    }

    fun openTraceDialog(trace: List<TraceStep>) {
        _activeTrace.value = trace
    }

    fun closeTraceDialog() {
        _activeTrace.value = null
    }

    fun openSourcesDialog(sources: List<Provenance>) {
        _activeSources.value = sources
    }

    fun closeSourcesDialog() {
        _activeSources.value = null
    }

    fun openNewQueryDialog(offer: PersistOffer) {
        _activePersistOffer.value = offer
    }

    fun closeNewQueryDialog() {
        _activePersistOffer.value = null
    }

    fun sendUserMessage(queryText: String) {
        if (queryText.isBlank()) return

        val userMsg = ChatMessage(isUser = true, text = queryText)
        val currentList = _messages.value.toMutableList()
        currentList.add(userMsg)

        // Add placeholder thinking message
        val thinkingMsg = ChatMessage(
            isUser = false,
            text = "",
            isThinking = true,
            thinkingStepIndex = 0
        )
        currentList.add(thinkingMsg)
        _messages.value = currentList

        viewModelScope.launch {
            // Process query through deterministic engine
            val engineResp = engine.processQuery(queryText, _currentHome.value)

            // Animate through thinking steps
            for (stepIdx in engineResp.thinkingSteps.indices) {
                delay(220)
                _messages.update { list ->
                    list.mapIndexed { idx, msg ->
                        if (idx == list.lastIndex) {
                            msg.copy(thinkingStepIndex = stepIdx)
                        } else msg
                    }
                }
            }

            delay(180)

            // Complete with final message
            val finalMsg = ChatMessage(
                isUser = false,
                text = engineResp.answer,
                cards = engineResp.cards,
                sources = engineResp.sources,
                trace = engineResp.trace,
                persistOffer = engineResp.persistOffer,
                isThinking = false
            )

            _messages.update { list ->
                list.dropLast(1) + finalMsg
            }
        }
    }

    fun saveQuery(title: String, schedule: Schedule, offer: PersistOffer) {
        viewModelScope.launch {
            val simNow = engine.demoState.now
            val initialResult = engine.runPersistentQuery(
                SavedQuery(
                    title = title,
                    tool = offer.tool,
                    argsJson = "{}",
                    schedule = schedule,
                    lastResult = QueryLatest(title, "Calculando…", "", "idle", simNow)
                ),
                _currentHome.value
            )

            val query = SavedQuery(
                title = title,
                tool = offer.tool,
                argsJson = "{}",
                schedule = schedule,
                lastResult = initialResult,
                isPinnedToWidget = true
            )

            queryDao.setPinnedQuery(query.id)
            queryDao.insertQuery(QueryEntity.fromSavedQuery(query))
            closeNewQueryDialog()
        }
    }

    fun deleteQuery(id: String) {
        viewModelScope.launch {
            queryDao.deleteQueryById(id)
        }
    }

    fun pinQueryToWidget(id: String) {
        viewModelScope.launch {
            queryDao.setPinnedQuery(id)
        }
    }

    fun recalculateQuery(query: SavedQuery) {
        viewModelScope.launch {
            val updatedResult = engine.runPersistentQuery(query, _currentHome.value)
            val updatedQuery = query.copy(lastResult = updatedResult)
            queryDao.updateQuery(QueryEntity.fromSavedQuery(updatedQuery))
        }
    }

    fun refreshAllQueries() {
        viewModelScope.launch {
            val list = queryDao.getAllQueries().first()
            list.forEach { entity ->
                val saved = entity.toSavedQuery()
                val updated = engine.runPersistentQuery(saved, _currentHome.value)
                queryDao.updateQuery(QueryEntity.fromSavedQuery(saved.copy(lastResult = updated)))
            }
        }
    }

    // Demo State Clock and Scenario Controls
    fun adjustClockMinutes(mins: Int) {
        val newNow = engine.demoState.now.addMinutes(mins)
        engine.setClock(newNow, engine.demoState.mode)
        _demoState.value = engine.demoState
        refreshAllQueries()
    }

    fun adjustClockDays(days: Int) {
        val newNow = SimTime(
            date = engine.demoState.now.offsetDate(days),
            minutes = engine.demoState.now.minutes
        )
        engine.setClock(newNow, engine.demoState.mode)
        _demoState.value = engine.demoState
        refreshAllQueries()
    }

    fun setClockPreset(preset: String) {
        when (preset) {
            "default" -> {
                engine.setClock(SimTime("2026-10-28", 7 * 60 + 45), "frozen")
            }
            "now" -> {
                val cal = java.util.Calendar.getInstance()
                val mins = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                val y = cal.get(java.util.Calendar.YEAR)
                val m = cal.get(java.util.Calendar.MONTH) + 1
                val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val dateStr = "%04d-%02d-%02d".format(y, m, d)
                engine.setClock(SimTime(dateStr, mins), "running")
            }
        }
        _demoState.value = engine.demoState
        refreshAllQueries()
    }

    fun toggleClockMode() {
        val newMode = if (engine.demoState.mode == "frozen") "running" else "frozen"
        engine.setClock(engine.demoState.now, newMode)
        _demoState.value = engine.demoState
    }

    fun toggleScenario(scenario: String) {
        engine.toggleScenario(scenario)
        _demoState.value = engine.demoState
        refreshAllQueries()
    }

    fun clearScenarios() {
        engine.clearScenarios()
        _demoState.value = engine.demoState
        refreshAllQueries()
    }

    fun simulateTomorrow() {
        val newDemo = engine.simulateTomorrow()
        _demoState.value = newDemo
        refreshAllQueries()
    }

    fun resetDemo() {
        val newDemo = engine.resetDemo()
        _demoState.value = newDemo
        _currentHome.value = HomePresets.DEFAULT_HOME
        refreshAllQueries()
    }

    fun setDataSourceMode(mode: String) {
        _dataSourceMode.value = mode
    }

    fun setRealBackendUrl(url: String) {
        _realBackendUrl.value = url
    }

    fun testBackendConnection() {
        viewModelScope.launch {
            _connectionStatus.value = "Conectando con ${_realBackendUrl.value}..."
            delay(600)
            _connectionStatus.value = "⚠️ Backend real no disponible en modo offline. Usando simulación local determinista."
        }
    }
}
