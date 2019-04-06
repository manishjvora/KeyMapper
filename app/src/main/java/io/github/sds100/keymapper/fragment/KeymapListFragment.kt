package io.github.sds100.keymapper.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.KeymapAdapterModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.activity.EditKeymapActivity
import io.github.sds100.keymapper.adapter.KeymapAdapter
import io.github.sds100.keymapper.interfaces.OnItemClickListener
import io.github.sds100.keymapper.util.ActionUtils
import io.github.sds100.keymapper.viewmodel.KeymapListViewModel
import kotlinx.android.synthetic.main.fragment_recyclerview.*
import org.jetbrains.anko.append
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by sds100 on 05/04/2019.
 */

class KeymapListFragment : Fragment(), OnItemClickListener<KeymapAdapterModel> {

    companion object {
        private const val KEY_PROFILE_ID = "id"

        fun newInstance(profileId: Long): KeymapListFragment {
            val bundle = Bundle().apply {
                putLong(KEY_PROFILE_ID, profileId)
            }

            return KeymapListFragment().apply {
                arguments = bundle
            }
        }
    }

    private val mViewModel: KeymapListViewModel by lazy {
        KeymapListViewModel.Factory(profileId, activity!!.application).create(KeymapListViewModel::class.java)
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                /*when the input method changes, update the action descriptions in case any need to show an error
                * that they need the input method to be enabled. */
                Intent.ACTION_INPUT_METHOD_CHANGED -> {
                    mViewModel.keyMapList.value?.let {
                        updateActionDescriptions(it)
                    }
                }
            }
        }
    }

    val profileId by lazy { arguments?.getLong(KEY_PROFILE_ID, -1)!! }

    val adapter: KeymapAdapter = KeymapAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_INPUT_METHOD_CHANGED)
        activity?.registerReceiver(mBroadcastReceiver, intentFilter)

        mViewModel.keyMapList.observe(this, Observer { keyMapList ->
            populateKeymapsAsync(keyMapList)

            updateActionDescriptions(keyMapList)
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        setCaption()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()

        activity?.unregisterReceiver(mBroadcastReceiver)
    }

    override fun onItemClick(item: KeymapAdapterModel) {
        val intent = Intent(context!!, EditKeymapActivity::class.java)
        intent.putExtra(EditKeymapActivity.EXTRA_KEYMAP_ID, item.id)

        startActivity(intent)
    }

    private fun updateActionDescriptions(keyMapList: List<KeyMap>) {
        //iterate through each keymap model and update the action description
        doAsync {
            adapter.itemList.forEach { model ->
                keyMapList.find { it.id == model.id }?.apply {
                    val actionDescription = ActionUtils.getDescription(context!!, action)
                    model.actionDescription = actionDescription
                }
            }

            uiThread {
                adapter.invalidateBoundViewHolders()
            }
        }
    }

    private fun populateKeymapsAsync(keyMapList: List<KeyMap>) {
        doAsync {
            val adapterModels = mutableListOf<KeymapAdapterModel>()

            keyMapList.forEach { keyMap ->

                val actionDescription = ActionUtils.getDescription(context!!, keyMap.action)

                adapterModels.add(KeymapAdapterModel(keyMap, actionDescription))
            }

            adapter.itemList = adapterModels

            uiThread {
                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                setCaption()
            }
        }
    }

    /**
     * Controls what message is displayed to the user on the home-screen
     */
    private fun setCaption() {
        //tell the user if they haven't created any KeyMaps
        if (adapter.itemCount == 0) {
            val spannableBuilder = SpannableStringBuilder()

            spannableBuilder.append(getString(R.string.shrug), RelativeSizeSpan(2f))
            spannableBuilder.append("\n\n")
            spannableBuilder.append(getString(R.string.profile_no_keymaps))

            textViewCaption.visibility = View.VISIBLE
            textViewCaption.text = spannableBuilder

            recyclerView.visibility = View.GONE
        } else {
            textViewCaption.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}