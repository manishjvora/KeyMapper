package io.github.sds100.keymapper.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomappbar.BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import io.github.sds100.keymapper.BuildConfig
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.KeymapAdapterModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.recyclerview.KeymapAdapter
import io.github.sds100.keymapper.interfaces.OnItemClickListener
import io.github.sds100.keymapper.selection.SelectionCallback
import io.github.sds100.keymapper.selection.SelectionEvent
import io.github.sds100.keymapper.selection.SelectionEvent.START
import io.github.sds100.keymapper.selection.SelectionEvent.STOP
import io.github.sds100.keymapper.selection.SelectionProvider
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.service.MyIMEService
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.view.BottomSheetMenu
import io.github.sds100.keymapper.view.StatusLayout
import io.github.sds100.keymapper.viewmodel.HomeViewModel
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.bottom_sheet_home.view.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.home_collapsed_status_layouts.*
import kotlinx.android.synthetic.main.home_expanded_status_layouts.*
import org.jetbrains.anko.*

class HomeActivity : AppCompatActivity(), SelectionCallback, OnItemClickListener<KeymapAdapterModel> {

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
                MyAccessibilityService.ACTION_ON_START -> {
                    accessibilityServiceStatusLayout.changeToFixedState()
                }
                MyAccessibilityService.ACTION_ON_STOP -> {
                    accessibilityServiceStatusLayout.changeToErrorState()
                }
            }
        }
    }

    private val mViewModel: HomeViewModel by lazy { ViewModelProviders.of(this).get(HomeViewModel::class.java) }
    private val mKeymapAdapter: KeymapAdapter = KeymapAdapter(this)
    private val mBottomSheetView by lazy { BottomSheetMenu.create(R.layout.bottom_sheet_home) }

    private val mStatusLayouts
        get() = sequence {
            yield(accessibilityServiceStatusLayout)
            yield(imeServiceStatusLayout)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                yield(dndAccessStatusLayout)
            }
        }.toList()

    private var mActionModeActive = false
        set(value) {
            field = value

            if (mActionModeActive) {
                appBar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_END
                appBar.navigationIcon = drawable(R.drawable.ic_arrow_back_appbar_24dp)
                fab.setImageDrawable(drawable(R.drawable.ic_outline_delete_white))
            } else {
                appBar.fabAlignmentMode = FAB_ALIGNMENT_MODE_CENTER
                appBar.navigationIcon = drawable(R.drawable.ic_menu_white_24dp)
                fab.setImageDrawable(drawable(R.drawable.ic_add_24dp_white))
            }

            appBar.menu.clear()
            onCreateOptionsMenu(appBar.menu)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(appBar)

        NotificationUtils.invalidateNotifications(this)

        /*if the app is a debug build then enable the accessibility service in settings
        / automatically so I don't have to! :)*/
        if (BuildConfig.DEBUG) {
            MyAccessibilityService.enableServiceInSettingsRoot()
        }

        mViewModel.keyMapList.observe(this, Observer { keyMapList ->
            populateKeymapsAsync(keyMapList)

            updateAccessibilityServiceKeymapCache(keyMapList)
            updateActionDescriptions(keyMapList)
        })

        appBar.setNavigationOnClickListener {
            if (mActionModeActive) {
                mKeymapAdapter.iSelectionProvider.stopSelecting()
            } else {
                mBottomSheetView.show(this)
            }
        }

        mBottomSheetView.createView = { view ->
            view.menuItemLog.isVisible = Logger.isLoggingEnabled(this)

            view.buttonEnableAll.setOnClickListener {
                mViewModel.enableAllKeymaps()
                mBottomSheetView.dismiss()
            }

            view.buttonDisableAll.setOnClickListener {
                mViewModel.disableAllKeymaps()
                mBottomSheetView.dismiss()
            }

            view.menuItemSendFeedback.setOnClickListener { FeedbackUtils.sendFeedback(this) }
        }

        //start NewKeymapActivity when the fab is pressed
        fab.setOnClickListener {
            if (mActionModeActive) {
                mViewModel.deleteKeyMapById(*mKeymapAdapter.iSelectionProvider.selectedItemIds)
                mKeymapAdapter.iSelectionProvider.stopSelecting()
            } else {
                val intent = Intent(this, NewKeymapActivity::class.java)
                startActivity(intent)
            }
        }

        accessibilityServiceStatusLayout.setOnFixClickListener(View.OnClickListener {
            if (RootUtils.checkAppHasRootPermission(this)) {
                MyAccessibilityService.enableServiceInSettingsRoot()

            } else {
                MyAccessibilityService.openAccessibilitySettings(this)
            }
        })

        imeServiceStatusLayout.setOnFixClickListener(View.OnClickListener {
            try {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

                startActivity(intent)
            } catch (e: Exception) {
                toast(R.string.error_cant_find_ime_settings)
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dndAccessStatusLayout.setOnFixClickListener(View.OnClickListener {
                PermissionUtils.requestPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY)
            })
        }

        mKeymapAdapter.iSelectionProvider.subscribeToSelectionEvents(this)

        //recyclerview stuff
        recyclerViewKeyMaps.layoutManager = LinearLayoutManager(this)
        recyclerViewKeyMaps.adapter = mKeymapAdapter

        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_INPUT_METHOD_CHANGED)
        intentFilter.addAction(MyAccessibilityService.ACTION_ON_START)
        intentFilter.addAction(MyAccessibilityService.ACTION_ON_STOP)
        registerReceiver(mBroadcastReceiver, intentFilter)

        //ask the user whether they want to enable analytics
        val isFirstTime = defaultSharedPreferences.getBoolean(
                str(R.string.key_pref_first_time), true
        )

        defaultSharedPreferences.edit {
            if (isFirstTime) {
                alert {
                    titleResource = R.string.title_pref_data_collection
                    messageResource = R.string.summary_pref_data_collection
                    positiveButton(R.string.pos_opt_in) {
                        putBoolean(str(R.string.key_pref_data_collection), true).commit()
                        setFirebaseDataCollection()
                        putBoolean(str(R.string.key_pref_first_time), false).commit()
                    }

                    negativeButton(R.string.neg_opt_out) {
                        putBoolean(str(R.string.key_pref_data_collection), false).commit()
                        setFirebaseDataCollection()
                        putBoolean(str(R.string.key_pref_first_time), false).commit()
                    }
                }.show()

            } else {
                setFirebaseDataCollection()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (mActionModeActive) {
            menuInflater.inflate(R.menu.menu_multi_select, appBar.menu)
            updateSelectionCount()
        } else {
            menuInflater.inflate(R.menu.menu_home, appBar.menu)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val selectedItemIds = mKeymapAdapter.iSelectionProvider.selectedItemIds

        return when (item?.itemId) {
            R.id.action_enable -> {
                mViewModel.enableKeymapById(*selectedItemIds)
                true
            }

            R.id.action_disable -> {
                mViewModel.disableKeymapById(*selectedItemIds)
                true
            }

            R.id.action_select_all -> {
                mKeymapAdapter.iSelectionProvider.selectAll()
                true
            }

            R.id.action_help -> {
                startActivity(Intent(this, HelpActivity::class.java))
                true
            }

            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> false
        }
    }

    override fun onResume() {
        super.onResume()

        if (MyAccessibilityService.isServiceEnabled(this)) {
            accessibilityServiceStatusLayout.changeToFixedState()
        } else {
            accessibilityServiceStatusLayout.changeToErrorState()
        }

        if (MyIMEService.isServiceEnabled(this)) {
            imeServiceStatusLayout.changeToFixedState()
        } else {
            imeServiceStatusLayout.changeToWarningState()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (accessNotificationPolicyGranted) {
                dndAccessStatusLayout.changeToFixedState()
            } else {
                dndAccessStatusLayout.changeToWarningState()
            }
        }

        when {
            mStatusLayouts.all { it.state == StatusLayout.State.FIXED } -> collapsedStatusLayout.changeToFixedState()
            mStatusLayouts.any { it.state == StatusLayout.State.ERROR } -> {
                collapsedStatusLayout.changeToErrorState()
                cardViewStatus.expanded = true
            }
            mStatusLayouts.any { it.state == StatusLayout.State.WARN } -> collapsedStatusLayout.changeToWarningState()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putBundle(
                    SelectionProvider.KEY_SELECTION_PROVIDER_STATE,
                    mKeymapAdapter.iSelectionProvider.saveInstanceState())
        }

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState!!.containsKey(SelectionProvider.KEY_SELECTION_PROVIDER_STATE)) {
            val selectionProviderState =
                    savedInstanceState.getBundle(SelectionProvider.KEY_SELECTION_PROVIDER_STATE)!!

            mKeymapAdapter.iSelectionProvider.restoreInstanceState(selectionProviderState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onBackPressed() {
        if (mActionModeActive) {
            mKeymapAdapter.iSelectionProvider.stopSelecting()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSelectionEvent(id: Long?, event: SelectionEvent) {
        when (event) {
            START -> mActionModeActive = true
            STOP -> mActionModeActive = false
            else -> updateSelectionCount()
        }
    }

    override fun onItemClick(item: KeymapAdapterModel) {
        val intent = Intent(this, EditKeymapActivity::class.java)
        intent.putExtra(EditKeymapActivity.EXTRA_KEYMAP_ID, item.id)

        startActivity(intent)
    }

    private fun updateSelectionCount() {
        appBar.menu.findItem(R.id.selection_count)?.apply {
            title = str(R.string.selection_count,
                    mKeymapAdapter.iSelectionProvider.selectionCount)
        }
    }

    private fun updateAccessibilityServiceKeymapCache(keyMapList: List<KeyMap>
    ) {
        val intent = Intent(MyAccessibilityService.ACTION_UPDATE_KEYMAP_CACHE)
        val jsonString = Gson().toJson(keyMapList)

        intent.putExtra(MyAccessibilityService.EXTRA_KEYMAP_CACHE_JSON, jsonString)

        sendBroadcast(intent)
    }

    private fun populateKeymapsAsync(keyMapList: List<KeyMap>) {
        doAsync {
            val adapterModels = mutableListOf<KeymapAdapterModel>()

            keyMapList.forEach { keyMap ->

                val actionDescription = ActionUtils.getDescription(this@HomeActivity, keyMap.action)

                adapterModels.add(KeymapAdapterModel(keyMap, actionDescription))
            }

            mKeymapAdapter.itemList = adapterModels

            uiThread {
                mKeymapAdapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                setCaption()
            }
        }
    }

    private fun updateActionDescriptions(keyMapList: List<KeyMap>) {
        //iterate through each keymap model and update the action description
        doAsync {
            mKeymapAdapter.itemList.forEach { model ->
                keyMapList.find { it.id == model.id }?.apply {
                    val actionDescription = ActionUtils.getDescription(this@HomeActivity, action)
                    model.actionDescription = actionDescription
                }
            }

            uiThread {
                mKeymapAdapter.invalidateBoundViewHolders()
            }
        }
    }

    /**
     * Controls what message is displayed to the user on the home-screen
     */
    private fun setCaption() {
        //tell the user if they haven't created any KeyMaps
        if (mKeymapAdapter.itemCount == 0) {
            val spannableBuilder = SpannableStringBuilder()

            spannableBuilder.append(getString(R.string.shrug), RelativeSizeSpan(2f))
            spannableBuilder.append("\n\n")
            spannableBuilder.append(getString(R.string.no_key_maps))

            textViewCaption.visibility = View.VISIBLE
            textViewCaption.text = spannableBuilder
        } else {
            textViewCaption.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setStatusBarColor(@ColorRes colorId: Int) {
        window.statusBarColor = color(colorId)
    }

    private fun setFirebaseDataCollection() {
        val isDataCollectionEnabled = defaultSharedPreferences.getBoolean(
                str(R.string.key_pref_data_collection),
                bool(R.bool.default_value_data_collection))

        FirebaseAnalytics.getInstance(this@HomeActivity).setAnalyticsCollectionEnabled(isDataCollectionEnabled)
    }
}
