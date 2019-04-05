package io.github.sds100.keymapper

import androidx.room.*
import io.github.sds100.keymapper.data.KeyMapDao
import io.github.sds100.keymapper.data.ProfileDao
import io.github.sds100.keymapper.profile.Profile
import io.github.sds100.keymapper.util.FlagUtils
import io.github.sds100.keymapper.util.addFlag
import io.github.sds100.keymapper.util.containsFlag
import io.github.sds100.keymapper.util.removeFlag

/**
 * Created by sds100 on 12/07/2018.
 */

@Entity(tableName = KeyMapDao.TABLE_NAME,
        foreignKeys = [(ForeignKey(
                entity = Profile::class,
                parentColumns = [ProfileDao.KEY_ID],
                childColumns = [KeyMapDao.KEY_PROFILE_ID],
                onDelete = ForeignKey.CASCADE))],
        indices = [Index(KeyMapDao.KEY_PROFILE_ID)]
)
class KeyMap(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        @ColumnInfo(name = KeyMapDao.KEY_TRIGGER_LIST)
        val triggerList: MutableList<Trigger> = mutableListOf(),

        @ColumnInfo(name = KeyMapDao.KEY_FLAGS)
        /**
         * Flags are stored as bits.
         */
        var flags: Int = 0,

        @ColumnInfo(name = KeyMapDao.KEY_ENABLED)
        var isEnabled: Boolean = true,

        /**
         * Is null if it isn't in a profile.
         */
        @ColumnInfo(name = KeyMapDao.KEY_PROFILE_ID)
        var profileId: Long? = null
) {
    @Embedded
    var action: Action? = null
        set(value) {
            if (value.isVolumeAction) {
                flags = addFlag(flags, FlagUtils.FLAG_SHOW_VOLUME_UI)
            } else {
                flags = removeFlag(flags, FlagUtils.FLAG_SHOW_VOLUME_UI)
            }

            field = value
        }

    val isLongPress
        get() = containsFlag(flags, FlagUtils.FLAG_LONG_PRESS)

    override fun hashCode() = id.toInt()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyMap

        if (id != other.id) return false

        return true
    }

    fun clone() = KeyMap(id, triggerList, flags, isEnabled, profileId).apply {
        action = this@KeyMap.action
    }

    fun containsTrigger(keyCodes: List<Int>): Boolean {
        return triggerList.any { trigger ->
            trigger.keys.toTypedArray().contentEquals(keyCodes.toTypedArray())
        }
    }
}