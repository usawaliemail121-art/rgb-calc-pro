package com.utilitynest.neoncalc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for the calculation history. Only the rows that are
 * actually on screen get inflated, so a long history stays smooth no matter
 * how many entries have been saved.
 */
class HistoryAdapter(
    private val items: MutableList<HistoryItem>,
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val expression: TextView = view.findViewById(R.id.itemExpression)
        val result: TextView = view.findViewById(R.id.itemResult)
        val time: TextView = view.findViewById(R.id.itemTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = items[position]
        holder.expression.text = item.expression
        holder.result.text = "= " + item.result
        holder.time.text = formatter.format(Date(item.timestamp))
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
