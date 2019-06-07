package io.github.sds100.keymapper.recyclerview

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Trigger
import io.github.sds100.keymapper.TriggerKeyModel
import io.github.sds100.keymapper.delegate.DragAndDropDelegate
import io.github.sds100.keymapper.delegate.IDragAndDrop
import io.github.sds100.keymapper.util.KeycodeUtils
import io.github.sds100.keymapper.util.observeAdapterData
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.view.SquareImageButton
import kotlinx.android.synthetic.main.trigger_adapter_item.view.*

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * Display a list of [Trigger]s as Chips in a RecyclerView
 */
class TriggerAdapter(override var items: MutableList<TriggerKeyModel> = mutableListOf()
) : RecyclerView.Adapter<TriggerAdapter.ViewHolder>(), IDragAndDrop<TriggerKeyModel> {

    var showRemoveButton: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var triggerMode: Trigger.Mode = Trigger.Mode.PARALLEL
        set(value) {
            field = value

            notifyDataSetChanged()
        }

    private val mDragAndDropDelegate = DragAndDropDelegate(this)

    init {
        /* If the last item is removed or inserted then the item before needs updating since the arrow
        needs to be removed or shown */
        observeAdapterData(
                onInsert = { position ->
                    notifyItemChanged(position - 1)
                },
                onRemove = { position ->
                    notifyItemChanged(position - 1)
                }
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.trigger_adapter_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val model = items[position]

        holder.itemView.apply {
            triggerTitle.text = buildString {
                append(KeycodeUtils.keycodeToString(model.keyCode))
                append(" ${str(R.string.trigger_title_divider_char)} ")
                append(model.deviceName)
            }

            buttonDrag.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    if (itemCount > 1) {
                        mDragAndDropDelegate.startDragging(holder)
                    }
                }

                false
            }

            if (!showRemoveButton) {
                buttonRemove.visibility = View.GONE
            }

            holder.invalidateLinkImage()
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        mDragAndDropDelegate.onAttachedToRecyclerView(recyclerView)

        recyclerView.itemAnimator = object : DefaultItemAnimator() {

            override fun onMoveFinished(item: RecyclerView.ViewHolder?) {
                super.onMoveFinished(item)

                (item as ViewHolder).invalidateLinkImage()
            }
        }
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int) {
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.findViewById<SquareImageButton>(R.id.buttonRemove).setOnClickListener {
                if (adapterPosition in 0..itemCount) {
                    items.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                }
            }

            invalidateLinkImage()
        }

        /**
         * Invalidate the image which is an arrow or plus icon
         */
        fun invalidateLinkImage() {
            //if it is the last item, don't show the down arrow
            itemView.imageViewLink.apply {
                when (triggerMode) {
                    Trigger.Mode.PARALLEL -> {
                        setImageState(intArrayOf(-R.attr.state_arrow), true)
                    }

                    Trigger.Mode.SEQUENCE -> {
                        setImageState(intArrayOf(R.attr.state_arrow), true)
                    }
                }

                //don't show if it is the last time
                isVisible = adapterPosition != itemCount - 1
            }
        }
    }
}