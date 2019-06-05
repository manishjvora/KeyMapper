package io.github.sds100.keymapper.viewmodel

import android.app.Application
import org.jetbrains.anko.doAsync

/**
 * Created by sds100 on 04/10/2018.
 */

class NewKeyMapViewModel(application: Application) : ConfigKeyMapViewModel(application) {
    //create a blank keymap
    override val id: Long = 0

    override fun saveKeymap() {
        doAsync { db.keyMapDao().insert(createKeymap()) }
    }
}