package it.unipi.di.sam.immersivegallery.common

import android.content.Context
import android.database.Cursor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

typealias OnPositionChangedListener<T> = (position: Int, data: T?) -> Unit

abstract class GenericRecyclerAdapter<T, B, K> constructor(
    context: Context,
    private val handler: K,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
        where B : ViewBinding, K : GenericAdapterItemHandler<T, B> {

    private var _listener: OnPositionChangedListener<T>? = null
    private val _inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        GenericRecyclerViewHolder(handler.inflate(_inflater, parent, false).rootView)

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
        // Log.d("PSCH", "$_position => $value")
        _position = value
        _listener?.invoke(_position, itemAt(_position))
    }

    public fun getPosition(): Int {
        return _position
    }

    public fun prevPosition(loop: Boolean): Int {
        if (itemCount == 0) return 0
        var tmp = getPosition() - 1
        if (tmp == -1 && !loop) tmp = 0
        tmp = (tmp + itemCount) % itemCount
        updatePosition(tmp)
        return getPosition()
    }

    public fun nextPosition(loop: Boolean): Int {
        if (itemCount == 0) return 0
        var tmp = getPosition() + 1
        if (tmp == itemCount && !loop) tmp = itemCount - 1
        tmp %= itemCount
        updatePosition(tmp)
        return getPosition()
    }

    fun setOnPositionChangedListener(listener: OnPositionChangedListener<T>) {
        _listener = listener
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
    private var cursor: Cursor?,
    private val cacheMaxSize: Int = 15,
) : GenericRecyclerAdapter<T, B, K>(context = context, handler = handler)
        where B : ViewBinding, K : GenericAdapterItemHandler<T, B>, K : WithCursorSupport<T> {

    init {
        handler.onUpdateCursor(null, cursor)
    }

    private val cache = linkedMapOf<Int, T?>()

    override fun itemAt(position: Int): T? {
        // Search inside cache
        if (cache.contains(position)) return cache[position]

        // Read value from cursor
        val value = handler.fromCursorPosition(cursor, position)

        // Update cache
        cache[position] = value

        // Remove last item if cache is too big
        if (cache.size >= cacheMaxSize) {
            cache.remove(
                cache.entries.first().key
            )
        }
        // Log.d("CACHE", cache.entries.joinToString(",") { it.key.toString() })

        // Returns
        return value
    }

    override fun getItemCount(): Int = cursor?.count ?: 0

    fun replaceCursor(cursor: Cursor, resetPosition: Boolean): Int {
        // Close old cursor
        this.cursor?.close()

        // Call cursor update on handler
        handler.onUpdateCursor(this.cursor, cursor)

        // Update cursor
        this.cursor = cursor

        // !! Invalidate cache
        this.cache.clear()

        // Notify recycler
        super.notifyDataSetChanged()

        // Exit if cursor is empty
        if (cursor.count == 0) return 0

        // Update position
        if (resetPosition) super.updatePosition(0)
        else if (cursor.count <= getPosition()) super.updatePosition(cursor.count - 1)
        else super.updatePosition(getPosition())

        // Returns "new" position
        return getPosition()
    }
}

fun <T> RecyclerView.Adapter<RecyclerView.ViewHolder>.onPositionChangedListener(listener: OnPositionChangedListener<T>) =
    (this as GenericRecyclerAdapter<T, *, *>).setOnPositionChangedListener(listener)

fun <T> RecyclerView.Adapter<RecyclerView.ViewHolder>.replaceList(newList: List<T>) =
    (this as GenericRecyclerAdapterWithList<T, *, *>).replaceList(newList)

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.replaceCursor(cursor: Cursor, reset: Boolean) =
    (this as GenericRecyclerAdapterWithCursor<*, *, *>).replaceCursor(cursor, reset)

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.position() =
    (this as GenericRecyclerAdapter<*, *, *>).getPosition()

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.position(value: Int) =
    (this as GenericRecyclerAdapter<*, *, *>).updatePosition(value)

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.prevPosition(loop: Boolean) =
    (this as GenericRecyclerAdapter<*, *, *>).prevPosition(loop)

fun RecyclerView.Adapter<RecyclerView.ViewHolder>.nextPosition(loop: Boolean) =
    (this as GenericRecyclerAdapter<*, *, *>).nextPosition(loop)