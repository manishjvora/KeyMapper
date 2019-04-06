package io.github.sds100.keymapper.delegate

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import io.github.sds100.keymapper.interfaces.GetTabTitleListener

/**
 * Created by sds100 on 29/11/2018.
 */

class TabDelegate(private val mSupportFragmentManager: FragmentManager,
                  private val mTabLayout: TabLayout,
                  private val mViewPager: ViewPager,
                  private val mGetTabTitleListener: GetTabTitleListener,
                  private val sortFragments: (fragments: List<Fragment>) -> List<Fragment> = { it },
                  private val mOffScreenLimit: Int = 3
) {

    private val mFragmentPagerAdapter =
            object : FragmentStatePagerAdapter(mSupportFragmentManager) {
                override fun getItem(position: Int) = mFragments[position]
                override fun getPageTitle(position: Int) = mGetTabTitleListener.getTabTitle(getItem(position))
                override fun getCount() = mFragments.size

                override fun notifyDataSetChanged() {
                    super.notifyDataSetChanged()

                    if (count == 1) {
                        mTabLayout.tabMode = TabLayout.MODE_FIXED
                        mTabLayout.tabGravity = TabLayout.GRAVITY_FILL
                    } else {
                        mTabLayout.tabMode = TabLayout.MODE_SCROLLABLE
                        mTabLayout.tabGravity = TabLayout.GRAVITY_CENTER
                    }
                }
            }

    private var mFragments: MutableList<Fragment> = mutableListOf()

    @Suppress("UNCHECKED_CAST")
    fun <T : Fragment> containsFragment(predicate: (fragment: T) -> Boolean): Boolean {
        return mFragments.any { predicate(it as T) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Fragment> getFragments() = mFragments as List<T>

    fun addFragment(vararg fragment: Fragment) {
        fragment.forEach {
            mFragments.add(it)
        }

        mFragmentPagerAdapter.notifyDataSetChanged()
    }

    fun removeFragment(fragment: Fragment) {
        val fragmentIndex = mFragments.indexOf(fragment)

        mFragments.removeAt(fragmentIndex)

        mSupportFragmentManager.beginTransaction().remove(fragment).commit()

        mFragmentPagerAdapter.notifyDataSetChanged()
    }

    fun configureTabs() {
        //improves performance when switching tabs since the fragment's onViewCreated isn't called
        mViewPager.offscreenPageLimit = mOffScreenLimit
        mViewPager.adapter = mFragmentPagerAdapter

        mTabLayout.setupWithViewPager(mViewPager)
    }

    fun getShownFragment() = mFragmentPagerAdapter.getItem(mTabLayout.selectedTabPosition)
}