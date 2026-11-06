package com.voidstream.app.notification

import com.google.common.truth.Truth.assertThat
import com.voidstream.app.data.local.LocalDataManager.AutoBackupType
import org.junit.Test

class AutoBackupTypeTest {
    @Test
    fun `one profile backs up what was chosen`() {
        AutoBackupType.entries.forEach { assertThat(AutoBackupWorker.effectiveType(it, profileCount = 1)).isEqualTo(it) }
    }

    @Test
    fun `two or more profiles always back up every profile in a master backup`() {
        AutoBackupType.entries.forEach { assertThat(AutoBackupWorker.effectiveType(it, profileCount = 2)).isEqualTo(AutoBackupType.MASTER) }
    }
}
