package io.github.sds100.keymapper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.data.AppDatabase

/**
 * Created by sds100 on 29/03/2019.
 */

class KeymapListViewModel(profileId: Long, application: Application) : AndroidViewModel(application) {
    private val mDb: AppDatabase = AppDatabase.getInstance(application)

    val keyMapList = getKeymapsForProfile(profileId)

    /**
     * If [profileId] is -1 then it gets all the keymaps not assigned to a profile
     */
    private fun getKeymapsForProfile(profileId: Long): LiveData<List<KeyMap>> {
        return if (profileId == -1L) {
            mDb.profileDao().getKeymapsUnassignedToProfile()
        } else {
            mDb.profileDao().getKeymapsForProfile(profileId)
        }
    }

    class Factory(private val mProfileId: Long,
                  private val mApplication: Application
    ) : ViewModelProvider.AndroidViewModelFactory(mApplication) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = KeymapListViewModel(mProfileId, mApplication) as T
    }
}