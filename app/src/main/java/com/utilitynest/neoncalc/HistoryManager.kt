package com.utilitynest.neoncalc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val expression: String,
    val result: String,
    val timestamp: Long
)

/**
 * Persists calculation history and the active input using SharedPreferences.
 * History is stored as a small JSON array, so it survives app restarts.
 */
class HistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getAll(): MutableList<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        val raw = prefs.getString(KEY_ITEMS, "[]") ?: "[]"
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HistoryItem(
                        obj.optString("e", ""),
                        obj.optString("r", ""),
                        obj.optLong("t", 0L)
                    )
                )
            }
        } catch (ignored: Exception) {
            // Corrupt data must never crash the calculator.
        }
        return list
    }

    fun add(item: HistoryItem) {
        val list = getAll()
        list.add(0, item)
        while (list.size > MAX_ITEMS) list.removeAt(list.size - 1)
        persist(list)
    }

    fun clear() {
        prefs.edit().putString(KEY_ITEMS, "[]").apply()
    }

    private fun persist(list: List<HistoryItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("e", item.expression)
            obj.put("r", item.result)
            obj.put("t", item.timestamp)
            array.put(obj)
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    fun saveActiveInput(input: String) {
        prefs.edit().putString(KEY_STATE, input).apply()
    }

    fun loadActiveInput(): String = prefs.getString(KEY_STATE, "") ?: ""

    companion object {
        private const val PREFS = "rgbcalc_history"
        private const val KEY_ITEMS = "items"
        private const val KEY_STATE = "active_input"
        private const val MAX_ITEMS = 200
    }
}
