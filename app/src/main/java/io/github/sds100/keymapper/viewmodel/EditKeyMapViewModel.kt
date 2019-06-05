package io.github.sds100.keymapper.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by sds100 on 04/10/2018.
 */

class EditKeyMapViewModel(override val id: Long, application: Application) : ConfigKeyMapViewModel(application) {

    init {
        doAsync {
            val newKeyMap = db.keyMapDao().getById(id)
            uiThread {
                action.value = newKeyMap.action
                triggerList.value = newKeyMap.triggerList
                flags.value = newKeyMap.flags
                isEnabled.value = newKeyMap.isEnabled
            }
        }
    }

    override fun saveKeymap() {
        doAsync {
            db.keyMapDao().update(createKeymap())
        }
    }

    class Factory(private val mId: Long,
                  private val mApplication: Application
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = EditKeyMapViewModel(mId, mApplication) as T
    }
}