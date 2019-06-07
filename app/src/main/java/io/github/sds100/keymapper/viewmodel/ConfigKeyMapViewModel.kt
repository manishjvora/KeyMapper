package io.github.sds100.keymapper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.Action
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.Trigger
import io.github.sds100.keymapper.data.AppDatabase
import io.github.sds100.keymapper.util.mutableLiveData

/**
 * Created by sds100 on 05/09/2018.
 */

abstract class ConfigKeyMapViewModel(application: Application) : AndroidViewModel(application) {

    val db: AppDatabase = AppDatabase.getInstance(application)

    val actionList: MutableLiveData<List<Action>> = listOf<Action>().mutableLiveData()
    var triggerList: MutableLiveData<List<Trigger>> = listOf<Trigger>().mutableLiveData()
    val flags: MutableLiveData<Int> = 0.mutableLiveData()
    val isEnabled: MutableLiveData<Boolean> = false.mutableLiveData()

    fun createKeymap(): KeyMap {
        val keyMap = KeyMap(id)

        keyMap.actionList = actionList.value!!.toMutableList()
        keyMap.triggerList = triggerList.value!!.toMutableList()
        keyMap.flags = flags.value!!
        keyMap.isEnabled = isEnabled.value!!

        return keyMap
    }

    abstract val id: Long
    abstract fun saveKeymap()
}