package io.github.sds100.keymapper.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.profile.Profile

/**
 * Created by sds100 on 05/04/2019.
 */

@Dao
abstract class ProfileDao {
    companion object {
        const val TABLE_NAME = "profiles"

        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_TRIGGER_TYPE = "trigger_type"
        const val KEY_TRIGGER_EXTRAS = "trigger_extras"
    }

    @Query("SELECT * FROM $TABLE_NAME")
    abstract fun getAll(): LiveData<List<Profile>>

    @Query("SELECT * FROM $TABLE_NAME WHERE $KEY_ID = (:profileId)")
    abstract fun getProfile(profileId: Long): LiveData<Profile>

    @Query("SELECT $KEY_ID FROM $TABLE_NAME")
    abstract fun getIds(): LiveData<List<Long>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insert(vararg profile: Profile)

    @Query("SELECT * FROM ${KeyMapDao.TABLE_NAME} WHERE ${KeyMapDao.KEY_PROFILE_ID} = (:profileId)")
    abstract fun getKeymapsForProfile(profileId: Long): LiveData<List<KeyMap>>

    @Query("SELECT * FROM ${KeyMapDao.TABLE_NAME} WHERE ${KeyMapDao.KEY_PROFILE_ID} is null")
    abstract fun getKeymapsUnassignedToProfile(): LiveData<List<KeyMap>>
}