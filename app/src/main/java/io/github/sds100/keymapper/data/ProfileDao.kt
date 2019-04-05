package io.github.sds100.keymapper.data

/**
 * Created by sds100 on 05/04/2019.
 */

abstract class ProfileDao {
    companion object {
        const val TABLE_NAME = "profiles"

        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_TRIGGER_TYPE = "trigger_type"
        const val KEY_TRIGGER_EXTRAS = "trigger_extras"
    }
}