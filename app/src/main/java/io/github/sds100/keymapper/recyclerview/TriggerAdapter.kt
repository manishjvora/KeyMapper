package io.github.sds100.keymapper.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Trigger
import io.github.sds100.keymapper.util.KeycodeUtils
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.view.SquareImageButton
import kotlinx.android.synthetic.main.trigger_adapter_item.view.*

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * Display a list of [Trigger]s as Chips in a RecyclerView
 */
class TriggerAdapter(
        triggerList: MutableList<Trigger> = mutableListOf(),
        val showRemoveButton: Boolean = true
) : RecyclerView.Adapter<TriggerAdapter.ViewHolder>() {

    var triggerList: MutableList<Trigger> = mutableListOf()
        set(value) {
            notifyDataSetChanged()
            field = value
        }

    init {
        this.triggerList = triggerList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.trigger_adapter_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.apply {
            triggerTitle.text = buildString {
                append(KeycodeUtils.keycodeToString(triggerList[position].keys[0]))
                append(" ${str(R.string.trigger_title_divider_char)} ")
                append("Logitech Bluetooth Keyboard")
            }


            //if it is the last item, don't show the down arrow
            imageViewDownArrow.isVisible = position != triggerList.size - 1

            if (!showRemoveButton) {
                buttonRemove.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = triggerList.size

    private val mItemTouchHelper = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
        ): Boolean {

        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.findViewById<SquareImageButton>(R.id.buttonRemove).setOnClickListener {
                if (adapterPosition in 0..itemCount) {
                    triggerList.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                }
            }
        }
    }
}