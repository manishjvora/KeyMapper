package io.github.sds100.keymapper.profile

import androidx.annotation.StringDef
import androidx.room.ColumnInfo
import io.github.sds100.keymapper.Extra
import io.github.sds100.keymapper.data.ProfileDao

/**
 * Created by sds100 on 05/04/2019.
 */

class ProfileTrigger(
        @ColumnInfo(name = ProfileDao.KEY_TRIGGER_TYPE)
        val type: String,

        @ColumnInfo(name = ProfileDao.KEY_TRIGGER_EXTRAS)
        val extras: MutableList<Extra>) {

    companion object {
        const val TYPE_BLUETOOTH_DEVICE = "type_bluetooth_device"
        const val TYPE_APP = "type_app"
    }
}

@StringDef(
        value = [
            ProfileTrigger.TYPE_BLUETOOTH_DEVICE,
            ProfileTrigger.TYPE_APP
        ]
)
annotation class ProfileTriggerType