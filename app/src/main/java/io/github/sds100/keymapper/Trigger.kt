package io.github.sds100.keymapper

import java.io.Serializable

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [keys] The key codes which will trigger the action
 */
data class Trigger(val keys: Set<Key>) : Serializable {

    data class Key(val keyCode: Int, val deviceId: String) {
        override fun equals(other: Any?): Boolean {
            return (other as Key).keyCode == keyCode
        }

        override fun hashCode() = keyCode
    }

    enum class Mode {
        PARALLEL, SEQUENCE
    }
}