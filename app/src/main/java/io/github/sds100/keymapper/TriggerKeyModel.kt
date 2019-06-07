package io.github.sds100.keymapper

/**
 * Created by sds100 on 05/06/2019.
 */

data class TriggerKeyModel(val keyCode: Int, val deviceName: String) {

    override fun equals(other: Any?): Boolean {
        return (other as TriggerKeyModel).keyCode == keyCode
    }

    override fun hashCode() = keyCode
}