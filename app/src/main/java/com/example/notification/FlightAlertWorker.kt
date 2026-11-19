package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.FlightTrackerApp
import com.example.model.ApiResult
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class FlightAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FlightTrackerApp ?: return Result.success()
        val settings = app.settingsDataStore.userSettings.first()
        val apiConfig = app.settingsDataStore.apiConfiguration.first()
        if (!apiConfig.isConfigured) return Result.success()

        val dao = app.database.alertsAndPlaybackDao()
        val alerts = dao.getEnabledAlerts()
        if (alerts.isEmpty()) return Result.success()

        for (alert in alerts) {
            when (val res = app.aviationRepository.getFlightDetails(alert.flightCode)) {
                is ApiResult.Success -> {
                    val f = res.data
                    val newStatus = f.status?.lowercase()
                    val oldStatus = alert.lastStatus?.lowercase()
                    val depGateChanged = f.depGate != null && alert.lastDepGate != null && f.depGate != alert.lastDepGate
                    val depTermChanged = f.depTerminal != null && alert.lastDepTerminal != null && f.depTerminal != alert.lastDepTerminal
                    val arrGateChanged = f.arrGate != null && alert.lastArrGate != null && f.arrGate != alert.lastArrGate
                    val arrTermChanged = f.arrTerminal != null && alert.lastArrTerminal != null && f.arrTerminal != alert.lastArrTerminal

                    val changes = mutableListOf<String>()
                    var gateRelated = false

                    if (newStatus != null && newStatus != oldStatus) {
                        when {
                            (newStatus == "en-route" || newStatus == "active") && alert.notifyTakeoff && settings.notifyTakeoff ->
                                changes.add("Flight has taken off and is now airborne.")
                            newStatus == "landed" && alert.notifyLanded && settings.notifyLanded ->
                                changes.add("Flight has landed at ${f.arrIata ?: "destination"}.")
                            newStatus == "delayed" && alert.notifyDelayed && settings.notifyDelayed ->
                                changes.add("Flight is delayed (${f.delayed ?: f.depDelayed ?: 0} min).")
                            (newStatus == "cancelled" || newStatus == "canceled") && alert.notifyCancelled && settings.notifyCancelled ->
                                changes.add("Flight has been cancelled.")
                            alert.notifyStatusChange && settings.notifyStatusChange ->
                                changes.add("Status updated to ${f.status}.")
                        }
                    }
                    if (f.depActual != null && oldStatus == "scheduled" && alert.notifyDeparture && settings.notifyDeparture) {
                        changes.add("Departed from ${f.depIata ?: "origin"} at ${f.depActual}.")
                    }
                    if (f.arrActual != null && oldStatus != "landed" && alert.notifyArrival && settings.notifyArrival) {
                        changes.add("Arrived at ${f.arrIata ?: "destination"} at ${f.arrActual}.")
                    }
                    if ((depGateChanged || arrGateChanged) && alert.notifyGateChange && settings.notifyGateChange) {
                        gateRelated = true
                        changes.add("Gate updated: Dep ${f.depGate ?: "-"} / Arr ${f.arrGate ?: "-"}.")
                    }
                    if ((depTermChanged || arrTermChanged) && alert.notifyTerminalChange && settings.notifyTerminalChange) {
                        gateRelated = true
                        changes.add("Terminal updated: Dep ${f.depTerminal ?: "-"} / Arr ${f.arrTerminal ?: "-"}.")
                    }

                    val changeSummary = changes.joinToString(" ")
                    val stateHash = "${f.status}|${f.depGate}|${f.depTerminal}|${f.arrGate}|${f.arrTerminal}|${f.delayed}"

                    if (changeSummary.isNotBlank() && stateHash != alert.lastNotifiedHash) {
                        NotificationHelper.sendFlightAlertNotification(
                            context = applicationContext,
                            flightCode = alert.flightCode,
                            title = "Flight ${alert.flightCode} Update",
                            body = changeSummary,
                            isGateOrTerminal = gateRelated
                        )
                    }

                    dao.upsertAlert(
                        alert.copy(
                            lastStatus = f.status ?: alert.lastStatus,
                            lastDepGate = f.depGate ?: alert.lastDepGate,
                            lastDepTerminal = f.depTerminal ?: alert.lastDepTerminal,
                            lastArrGate = f.arrGate ?: alert.lastArrGate,
                            lastArrTerminal = f.arrTerminal ?: alert.lastArrTerminal,
                            lastDelayedMin = f.delayed ?: alert.lastDelayedMin,
                            lastNotifiedHash = if (changeSummary.isNotBlank()) stateHash else alert.lastNotifiedHash
                        )
                    )
                }
                is ApiResult.Failure -> Unit
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "flight_alert_polling_worker"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<FlightAlertWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
