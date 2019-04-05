package io.github.sds100.keymapper.profile

import io.github.sds100.keymapper.KeyMap

/**
 * Created by sds100 on 05/04/2019.
 */

object ProfileManager {
    fun create(name: String) {

    }

    fun delete(vararg profiles: Profile) {

    }

    fun getActiveKeymaps(keymaps: List<KeyMap>): List<KeyMap> {
        return listOf()
    }

    fun isProfileActive(trigger: ProfileTrigger): Boolean {
        return false
    }
}