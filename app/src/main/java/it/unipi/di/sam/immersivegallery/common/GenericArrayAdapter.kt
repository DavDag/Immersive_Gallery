package it.unipi.di.sam.immersivegallery.common;

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.BaseAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

class GenericArrayAdapter<T, B : ViewBinding>(
    context: Context,
    private var handler: GenericAdapterItemHandler<T, B>,
    private var items: List<T>,
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    private fun inflateNewView(parent: ViewGroup): View =
        handler.inflate(inflater, parent, false)

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): T = items[position]
    override fun getItemId(position: Int): Long = handler.id(items[position])

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflateNewView(parent)
        val binding = handler.bind(view)
        handler.updateUI(binding, getItem(position))
        return view
    }

    fun replaceList(newList: List<T>) {
        this.items = newList
        super.notifyDataSetChanged()
    }
}

fun <T> Adapter.replaceList(newList: List<T>) =
    (this as GenericArrayAdapter<T, *>).replaceList(newList)

