package it.unipi.di.sam.immersivegallery.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

class GenericRecyclerAdapter<T, B : ViewBinding> constructor(
    val context: Context,
    private val handler: GenericAdapterItemHandler<T, B>,
    private var items: List<T>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        GenericRecyclerViewHolder(handler.inflate(inflater, parent, false).rootView)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int): Unit =
        handler.updateUI(handler.bind(holder.itemView), items[position])

    fun replaceList(newList: List<T>) {
        this.items = newList
        super.notifyDataSetChanged()
    }

    class GenericRecyclerViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {}

}

fun <T> RecyclerView.Adapter<RecyclerView.ViewHolder>.replaceList(newList: List<T>) =
    (this as GenericRecyclerAdapter<T, *>).replaceList(newList)