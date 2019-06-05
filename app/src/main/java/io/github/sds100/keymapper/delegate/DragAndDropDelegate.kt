package io.github.sds100.keymapper.delegate

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.*

/**
 * Created by sds100 on 29/05/2019.
 */

class DragAndDropDelegate<T>(iDragAndDrop: IDragAndDrop<T>
) : IDragAndDrop<T> by iDragAndDrop {

    private val mCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
    ) {
        override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
        ): Boolean {

            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition

            Collections.swap(items, fromPosition, toPosition)

            onItemMoved(fromPosition, toPosition)

            return true
        }

        override fun isLongPressDragEnabled() = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    }

    private val mItemTouchHelper = ItemTouchHelper(mCallback)

    fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        mItemTouchHelper.attachToRecyclerView(recyclerView)
    }

    fun startDragging(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper.startDrag(viewHolder)
    }
}

interface IDragAndDrop<T> {
    var items: MutableList<T>
    fun onItemMoved(fromPosition: Int, toPosition: Int)
}