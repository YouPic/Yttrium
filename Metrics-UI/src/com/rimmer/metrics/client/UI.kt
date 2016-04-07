package com.rimmer.metrics.client

import com.rimmer.metrics.server.generated.type.StatsPacket
import com.rimmer.yttrium.server.binary.BinaryClient
import com.rimmer.yttrium.server.binary.connectBinary
import com.rimmer.yttrium.server.binary.routeHash
import com.rimmer.yttrium.server.runClient
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.stage.Stage
import javafx.util.StringConverter
import org.joda.time.DateTime

val host = "127.0.0.1"
val port = 1338
val password = "mysecretpassword"

fun ceilTimeHour(time: DateTime) = time.withTime((time.hourOfDay + 1) % 24, 0, 0, 0)

class StatGraph {
    val chart: LineChart<Number, Number>
    val average = XYChart.Series<Number, Number>()
    val median = XYChart.Series<Number, Number>()
    val max = XYChart.Series<Number, Number>()

    init {
        val xAxis = NumberAxis()
        xAxis.isAutoRanging = false
        xAxis.lowerBound = ceilTimeHour(DateTime.now().minusDays(1)).millis.toDouble()
        xAxis.upperBound = ceilTimeHour(DateTime.now()).millis.toDouble()
        xAxis.tickUnit = 3600000.0
        xAxis.tickLabelFormatter = object: StringConverter<Number>() {
            override fun toString(v: Number) = DateTime(v.toLong()).toString("HH:mm:ss")
            override fun fromString(v: String) = 0
        }

        val yAxis = NumberAxis()
        yAxis.tickLabelFormatter = object: StringConverter<Number>() {
            override fun toString(v: Number) = "${v.toDouble() / 1000000.0} ms"
            override fun fromString(v: String) = 0
        }

        chart = LineChart<Number, Number>(xAxis, yAxis)
        chart.title = "Overall times"

        average.name = "Average"
        median.name = "Median"
        max.name = "Max"

        chart.data.add(average)
        chart.data.add(median)
        chart.data.add(max)
    }
}

class MetricsUI: Application() {
    val context = runClient(1, true)
    var server: BinaryClient? = null
    var lastUpdate = DateTime(0)

    val graph = StatGraph()

    override fun start(stage: Stage) {
        stage.title = "Metrics UI"

        val scene = Scene(graph.chart, 800.0, 600.0)

        stage.scene = scene
        stage.show()
    }

    fun connect() {
        connectBinary(context, host, port, 10000) { c, e ->
            Platform.runLater {
                if(e == null) {
                    server = c!!
                    onConnect()
                } else {
                    onConnectError(e)
                }
            }
        }
    }

    fun update() {
        val server = server
        if(server == null || !server.connected) return connect()

        server.call(
            routeHash("GET /stats/{from}/{to}"),
            arrayOf(lastUpdate.millis, DateTime.now().millis),
            arrayOf(password),
            StatsPacket::class.java
        ) { r, e ->
            Platform.runLater {
                if(e == null) {
                    onUpdate(r!! as StatsPacket)
                } else {
                    onUpdateError(e)
                }
            }
        }
    }

    fun onUpdate(packet: StatsPacket) {
        packet.slices.lastOrNull()?.let {
            lastUpdate = it.time
        }

        packet.slices.forEach {
            graph.average.data.add(XYChart.Data<Number, Number>(it.time.millis, it.global.average))
            graph.median.data.add(XYChart.Data<Number, Number>(it.time.millis, it.global.median))
            graph.max.data.add(XYChart.Data<Number, Number>(it.time.millis, it.global.max))
        }
    }

    fun onUpdateError(e: Throwable) {
        println("update error: $e")
    }

    fun onConnectError(e: Throwable) {
        println("connect error: $e")
    }

    fun onConnect() {
        println("connected")
        update()
    }
}

fun main(args: Array<String>) = Application.launch(MetricsUI::class.java, *args)