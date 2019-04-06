package io.github.sds100.keymapper.interfaces

import androidx.fragment.app.Fragment

/**
 * Created by sds100 on 06/04/2019.
 */

interface GetTabTitleListener{
    fun getTabTitle(fragment: Fragment): String
}