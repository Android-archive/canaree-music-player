package dev.olog.core.interactor

import dev.olog.core.gateway.AlarmService
import dev.olog.core.prefs.AppPreferencesGateway
import javax.inject.Inject

class SleepTimerUseCase @Inject constructor(
    private val gateway: AppPreferencesGateway,
    private val alarmService: AlarmService
) {


    fun getLast(): SleepData = SleepData(
        gateway.getSleepFrom(),
        gateway.getSleepTime()
    )

    fun set(sleepFrom: Long, sleepTime: Long, sleepUntil: Long) {
        gateway.setSleepTimer(sleepFrom, sleepTime)
        alarmService.set(sleepUntil)
    }

    fun reset() {
        gateway.resetSleepTimer()
        alarmService.resetTimer()
    }

}

data class SleepData(
    val fromWhen: Long,
    val sleepTime: Long
)