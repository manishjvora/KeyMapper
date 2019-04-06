package io.github.sds100.keymapper.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomappbar.BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
import com.google.android.material.tabs.TabLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import io.github.sds100.keymapper.BuildConfig
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.adapter.KeymapAdapter
import io.github.sds100.keymapper.delegate.TabDelegate
import io.github.sds100.keymapper.fragment.KeymapListFragment
import io.github.sds100.keymapper.interfaces.GetTabTitleListener
import io.github.sds100.keymapper.selection.SelectionCallback
import io.github.sds100.keymapper.selection.SelectionEvent
import io.github.sds100.keymapper.selection.SelectionEvent.START
import io.github.sds100.keymapper.selection.SelectionEvent.STOP
import io.github.sds100.keymapper.selection.SelectionProvider
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.service.MyIMEService
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.view.BottomSheetMenu
import io.github.sds100.keymapper.viewmodel.HomeViewModel
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.bottom_sheet_home.view.*
import kotlinx.android.synthetic.main.content_home.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.defaultSharedPreferences

class HomeActivity : AppCompatActivity(), SelectionCallback, TabLayout.OnTabSelectedListener, GetTabTitleListener {

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                MyAccessibilityService.ACTION_ON_START -> {
                    accessibilityServiceStatusLayout.changeToServiceEnabledState()
                }
                MyAccessibilityService.ACTION_ON_STOP -> {
                    accessibilityServiceStatusLayout.changeToServiceDisabledState()
                }
            }
        }
    }

    private val mViewModel by lazy { ViewModelProviders.of(this).get(HomeViewModel::class.java) }
    private val mBottomSheetView by lazy { BottomSheetMenu.create(R.layout.bottom_sheet_home) }

    private val mTabDelegate by lazy {
        TabDelegate(
                supportFragmentManager,
                tabLayout,
                viewPager,
                mGetTabTitleListener = this,
                sortFragments = { sortFragments(it) })
    }

    private var mActionModeActive = false
        set(value) {
            field = value

            if (mActionModeActive) {
                appBar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_END
                appBar.navigationIcon = drawable(R.drawable.ic_arrow_back_appbar_24dp)
                fab.setImageDrawable(drawable(R.drawable.ic_delete_white_24dp))
                onCreateOptionsMenu(appBar.menu)
                viewPager.isPagingEnabled = false
                tabLayout.visibility = View.GONE
            } else {
                appBar.fabAlignmentMode = FAB_ALIGNMENT_MODE_CENTER
                appBar.navigationIcon = drawable(R.drawable.ic_menu_white_24dp)
                fab.setImageDrawable(drawable(R.drawable.ic_add_24dp_white))
                appBar.menu.clear()
                viewPager.isPagingEnabled = true
                tabLayout.visibility = View.VISIBLE
            }
        }

    private val mCurrentKeymapAdapter: KeymapAdapter
        get() = (mTabDelegate.getShownFragment() as KeymapListFragment).adapter

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

        if (savedInstanceState != null) {
            val oldProfileFragments = supportFragmentManager.fragments.filter { it is KeymapListFragment }

            if (oldProfileFragments.all { it != null }) {
                mTabDelegate.addFragment(*oldProfileFragments.toTypedArray())
            }
        }

        mViewModel.keyMapList.observe(this, Observer { keyMapList ->
            updateAccessibilityServiceKeymapCache(keyMapList)
        })

        mViewModel.profiles.observe(this, Observer { profiles ->
            profiles.toMutableList().apply {
                forEach { profile ->

                    val currentFragments = mTabDelegate.getFragments<KeymapListFragment>()

                    //remove any fragments for profiles which don't exist anymore
                    currentFragments.forEach { fragment ->
                        if (!profiles.any { it.id == fragment.profileId }) {
                            mTabDelegate.removeFragment(fragment)
                        }
                    }

                    //add fragments for any new profiles
                    if (!mTabDelegate.containsFragment<KeymapListFragment> { it.profileId == profile.id }) {
                        val profileFragment = KeymapListFragment.newInstance(profile.id)
                        mTabDelegate.addFragment(profileFragment)
                    }
                }

                if (!mTabDelegate.containsFragment<KeymapListFragment> { it.profileId == -1L }) {
                    val profileFragment = KeymapListFragment.newInstance(-1L)
                    mTabDelegate.addFragment(profileFragment)
                }

                //mCurrentKeymapAdapter.iSelectionProvider.subscribeToSelectionEvents(this@HomeActivity)
            }
        })

        appBar.setNavigationOnClickListener {
            if (mActionModeActive) {
                mCurrentKeymapAdapter.iSelectionProvider.stopSelecting()
            } else {
                mBottomSheetView.show(this)
            }
        }

        mBottomSheetView.onViewCreated = { view ->
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
                mViewModel.deleteKeyMapById(*mCurrentKeymapAdapter.iSelectionProvider.selectedItemIds)
                mCurrentKeymapAdapter.iSelectionProvider.stopSelecting()
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
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            startActivity(intent)
        })

        mTabDelegate.configureTabs()
        tabLayout.addOnTabSelectedListener(this)

        val intentFilter = IntentFilter()
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
        menuInflater.inflate(R.menu.menu_multi_select, appBar.menu)

        return mActionModeActive
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val selectedItemIds = mCurrentKeymapAdapter.iSelectionProvider.selectedItemIds

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
                mCurrentKeymapAdapter.iSelectionProvider.selectAll()
                return true
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
            accessibilityServiceStatusLayout.changeToServiceEnabledState()
        } else {
            accessibilityServiceStatusLayout.changeToServiceDisabledState()
        }

        if (MyIMEService.isServiceEnabled(this)) {
            imeServiceStatusLayout.changeToServiceEnabledState()
        } else {
            imeServiceStatusLayout.changeToServiceDisabledState()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putBundle(
                    SelectionProvider.KEY_SELECTION_PROVIDER_STATE,
                    mCurrentKeymapAdapter.iSelectionProvider.saveInstanceState())
        }

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState!!.containsKey(SelectionProvider.KEY_SELECTION_PROVIDER_STATE)) {
            val selectionProviderState =
                    savedInstanceState.getBundle(SelectionProvider.KEY_SELECTION_PROVIDER_STATE)!!

            mCurrentKeymapAdapter.iSelectionProvider.restoreInstanceState(selectionProviderState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onBackPressed() {
        if (mActionModeActive) {
            mCurrentKeymapAdapter.iSelectionProvider.stopSelecting()
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

    override fun getTabTitle(fragment: Fragment): String {
        mViewModel.profiles.value?.apply {
            val fragmentProfileId = (fragment as KeymapListFragment).profileId

            if (fragmentProfileId == -1L) {
                return str(R.string.tab_title_unassigned)
            } else {
                find { it.id == fragmentProfileId }?.apply {
                    return name
                }
            }
        }

        return ""
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        mCurrentKeymapAdapter.iSelectionProvider.unsubscribeFromSelectionEvents(this)
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        mCurrentKeymapAdapter.iSelectionProvider.subscribeToSelectionEvents(this)
    }

    private fun sortFragments(fragments: List<Fragment>): List<Fragment> {
        return fragments.sortedBy { getTabTitle(it) }.toMutableList().apply {
            val unassignedFragment = find { (it as KeymapListFragment).profileId == -1L }

            if (unassignedFragment != null) {
                remove(unassignedFragment)
                add(0, unassignedFragment)
            }
        }
    }

    private fun updateSelectionCount() {
        appBar.menu.findItem(R.id.selection_count)?.apply {
            title = str(R.string.selection_count, mCurrentKeymapAdapter.iSelectionProvider.selectionCount)
        }
    }

    private fun setFirebaseDataCollection() {
        val isDataCollectionEnabled = defaultSharedPreferences.getBoolean(
                str(R.string.key_pref_data_collection),
                bool(R.bool.default_value_data_collection))

        FirebaseAnalytics.getInstance(this@HomeActivity).setAnalyticsCollectionEnabled(isDataCollectionEnabled)
    }

    private fun updateAccessibilityServiceKeymapCache(keyMapList: List<KeyMap>) {
        val intent = Intent(MyAccessibilityService.ACTION_UPDATE_KEYMAP_CACHE)
        val jsonString = Gson().toJson(keyMapList)

        intent.putExtra(MyAccessibilityService.EXTRA_KEYMAP_CACHE_JSON, jsonString)

        sendBroadcast(intent)
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {}
}
