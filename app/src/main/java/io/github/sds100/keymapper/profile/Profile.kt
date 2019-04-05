package io.github.sds100.keymapper.profile

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.sds100.keymapper.data.ProfileDao

/**
 * Created by sds100 on 05/04/2019.
 */

@Entity(tableName = ProfileDao.TABLE_NAME)
data class Profile(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        @ColumnInfo(name = ProfileDao.KEY_NAME)
        var name: String,

        @Embedded
        var profileTrigger: ProfileTrigger
)