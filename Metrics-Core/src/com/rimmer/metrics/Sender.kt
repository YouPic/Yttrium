package com.rimmer.metrics

import com.rimmer.metrics.generated.type.ErrorPacket
import com.rimmer.metrics.generated.type.ProfilePacket
import com.rimmer.metrics.generated.type.StatPacket

/**
 * General interface used to send metrics from a client.
 * This can be called from any thread.
 */
interface Sender {
    fun sendStatistic(stat: StatPacket)
    fun sendProfile(profile: ProfilePacket)
    fun sendError(error: ErrorPacket)
}