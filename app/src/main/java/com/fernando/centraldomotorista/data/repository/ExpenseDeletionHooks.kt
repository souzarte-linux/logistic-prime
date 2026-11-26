package com.fernando.centraldomotorista.data.repository

import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

interface ExpenseDeletionListener {
    suspend fun onExpenseDeleted(expenseId: String)
}

object ExpenseDeletionHooks {

    private val listeners = CopyOnWriteArrayList<ExpenseDeletionListener>()

    fun register(listener: ExpenseDeletionListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun unregister(listener: ExpenseDeletionListener) {
        listeners.remove(listener)
    }

    fun clear() {
        listeners.clear()
    }

    suspend fun notifyExpenseDeleted(expenseId: String) {
        for (listener in listeners) {
            try {
                listener.onExpenseDeleted(expenseId)
            } catch (e: Exception) {
                Log.w("ExpenseDeletionHooks", "Falha em listener ao excluir despesa $expenseId: ${e.message}")
            }
        }
    }
}
