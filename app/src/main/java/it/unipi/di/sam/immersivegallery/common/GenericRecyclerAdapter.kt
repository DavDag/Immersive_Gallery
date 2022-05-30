package it.unipi.di.sam.immersivegallery.common

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class GenericRecyclerAdapter<T, B, K> constructor(
    context: Context,
    private val handler: K,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
        where B : ViewBinding, K : GenericAdapterItemHandler<T, B> {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        GenericRecyclerViewHolder(handler.inflate(inflater, parent, false).rootView)

    abstract fun itemAt(position: Int): T?

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int): Unit =
        handler.updateUI(handler.bind(holder.itemView), itemAt(position))

    class GenericRecyclerViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {}

    /**
     * Current position works *ONLY* if *ALL* updates are done via:
     * - updatePosition / getPosition
     * - prevPosition
     * - nextPosition
     */

    private var _position: Int = 0

    public fun updatePosition(value: Int) {
        _position = value
    }

    public fun getPosition(): Int {
        return _position
    }

    public fun prevPosition(loop: Boolean): Int {
        _position--
        if (_position == -1 && !loop) _position = 0
        _position = (_position + itemCount) % itemCount
        return _position
    }

    public fun nextPosition(loop: Boolean): Int {
        _position++
        if (_position == itemCount && !loop) _position = itemCount - 1
        _position %= itemCount
        return _position
    }
}

class GenericRecyclerAdapterWithList<T, B, K> constructor(
    context: Context,
    handler: K,
    private var items: List<T>,
) : GenericRecyclerAdapter<T, B, K>(context = context, handler = handler)
        where B : ViewBinding, K : GenericAdapterItemHandler<T, B> {

    override fun itemAt(position: Int): T? = items.getOrNull(position)
    override fun getItemCount(): Int = items.size

    fun replaceList(newList: List<T>) {
        this.items = newList
        super.notifyDataSetChanged()
    }
}

class GenericRecyclerAdapterWithCursor<T, B, K> constructor(
    context: Context,
    private val handler: K,
    protected var cursor: Cursor?,
) : GenericRecyclerAdapter<T, B, K>(context = context, handler = handler)
        where B : ViewBinding, K : GenericAdapterItemHandler<T, B>, K : WithCursorSupport<T> {

    init {
        handler.onUpdateCursor(null, cursor)
    }

    override fun itemAt(position: Int): T? = handler.fromCursorPosition(cursor, position)
    override fun getItemCount(): Int = cursor?.count ?: 0

    fun replaceCursor(cursor: Cursor) {
        this.cursor?.close()
        handler.onUpdateCursor(this.cursor, cursor)
        this.cursor = cursor
        super.notifyDataSetChanged()
    }
}

fun <T> RecyclerView.Adapter<RecyclerView.ViewHolder>.replaceList(newList: List<T>) =
    (this as GenericRecyclerAdapterWithList<T, *, *>).replaceList(newList)

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.replaceCursor(cursor: Cursor) =
    (this as GenericRecyclerAdapterWithCursor<*, *, *>).replaceCursor(cursor)

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.position() =
    (this as GenericRecyclerAdapter<*, *, *>).getPosition()

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.position(value: Int) =
    (this as GenericRecyclerAdapter<*, *, *>).updatePosition(value)

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.prevPosition(loop: Boolean) =
    (this as GenericRecyclerAdapter<*, *, *>).prevPosition(loop)

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.nextPosition(loop: Boolean) =
    (this as GenericRecyclerAdapter<*, *, *>).nextPosition(loop)