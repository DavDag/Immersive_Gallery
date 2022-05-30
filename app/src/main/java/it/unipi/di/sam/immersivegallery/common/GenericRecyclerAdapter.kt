package it.unipi.di.sam.immersivegallery.common

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class GenericRecyclerAdapter<T, B : ViewBinding> constructor(
    context: Context,
    private val handler: GenericAdapterItemHandler<T, B>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        GenericRecyclerViewHolder(handler.inflate(inflater, parent, false).rootView)

    abstract fun itemAt(position: Int): T

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
        if (_position == itemCount + 1 && !loop) _position = itemCount
        _position %= itemCount
        return _position
    }
}

class GenericRecyclerAdapterWithList<T, B : ViewBinding> constructor(
    context: Context,
    handler: GenericAdapterItemHandler<T, B>,
    private var items: List<T>,
) : GenericRecyclerAdapter<T, B>(context = context, handler = handler) {

    override fun itemAt(position: Int): T = items[position]
    override fun getItemCount(): Int = items.size

    fun replaceList(newList: List<T>) {
        this.items = newList
        super.notifyDataSetChanged()
    }

}

class GenericRecyclerAdapterWithCursor<T, B : ViewBinding> constructor(
    context: Context,
    private val handler: GenericAdapterItemHandlerWithCursorSupport<T, B>,
    private var cursor: Cursor,
) : GenericRecyclerAdapter<T, B>(context = context, handler = handler) {

    override fun itemAt(position: Int): T = handler.fromCursor(cursor)
    override fun getItemCount(): Int = cursor.count

    fun replaceCursor(cursor: Cursor) {
        this.cursor = cursor
        super.notifyDataSetChanged()
    }

}

fun <T> RecyclerView.Adapter<RecyclerView.ViewHolder>.replaceList(newList: List<T>) =
    (this as GenericRecyclerAdapterWithList<T, *>).replaceList(newList)

fun <T> RecyclerView.Adapter<RecyclerView.ViewHolder>.replaceCursor(cursor: Cursor) =
    (this as GenericRecyclerAdapterWithCursor<T, *>).replaceCursor(cursor)

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.position() =
    (this as GenericRecyclerAdapter<*, *>).getPosition()

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.position(value: Int) =
    (this as GenericRecyclerAdapter<*, *>).updatePosition(value)

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.prevPosition(loop: Boolean) =
    (this as GenericRecyclerAdapter<*, *>).prevPosition(loop)

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.nextPosition(loop: Boolean) =
    (this as GenericRecyclerAdapter<*, *>).nextPosition(loop)