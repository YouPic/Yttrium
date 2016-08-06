package com.rimmer.metrics.client

import com.rimmer.metrics.formatMetric
import com.rimmer.metrics.generated.type.MetricUnit
import com.rimmer.metrics.server.generated.client.clientGetStats
import com.rimmer.metrics.server.generated.type.TimeMetric
import com.rimmer.yttrium.getOrAdd
import com.rimmer.yttrium.server.binary.BinaryClient
import com.rimmer.yttrium.server.binary.connectBinary
import com.rimmer.yttrium.server.runClient
import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.ListView
import javafx.stage.Stage
import javafx.util.StringConverter
import org.joda.time.DateTime
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

val host = "127.0.0.1"
val port = 1440
val password = "mysecretpassword"

fun ceilTimeHour(time: DateTime) = time.withTime((time.hourOfDay + 1) % 24, 0, 0, 0)

class StatGraph(val unit: MetricUnit) {
    val chart: LineChart<Number, Number>
    val metrics = HashMap<String, XYChart.Series<Number, Number>>()

    init {
        val xAxis = NumberAxis()
        xAxis.isAutoRanging = false
        xAxis.lowerBound = ceilTimeHour(DateTime.now().minusHours(2)).millis.toDouble()
        xAxis.upperBound = ceilTimeHour(DateTime.now()).millis.toDouble()
        xAxis.tickUnit = 3600000.0
        xAxis.tickLabelFormatter = object: StringConverter<Number>() {
            override fun toString(v: Number) = DateTime(v.toLong()).toString("HH:mm:ss")
            override fun fromString(v: String) = 0
        }

        val yAxis = NumberAxis()
        yAxis.tickLabelFormatter = object: StringConverter<Number>() {
            override fun toString(v: Number) = formatMetric(v.toLong(), unit)
            override fun fromString(v: String) = 0
        }

        chart = LineChart<Number, Number>(xAxis, yAxis)
        chart.title = "Overall times"
    }

    fun add(name: String, x: Number, y: Number) {
        val series = metrics.getOrAdd(name) {
            val it = XYChart.Series<Number, Number>()
            it.name = name
            chart.data.add(it)
            it
        }
        series.data.add(XYChart.Data<Number, Number>(x, y))
    }
}

class Server(unit: MetricUnit) {
    val paths = StatGraph(unit)
    val stats = StatGraph(unit)
}

class Category(unit: MetricUnit) {
    val overallPaths = StatGraph(unit)
    val overallStats = StatGraph(unit)
    val servers = FXCollections.observableHashMap<String, Server>()
}

class MetricsUI: Application() {
    val context = runClient(1, true)
    var server: BinaryClient? = null
    var lastUpdate = DateTime(0)

    val categories = FXCollections.observableHashMap<String, Category>()

    override fun start(stage: Stage) {
        stage.title = "Metrics UI"

        val graphs = ListView(categories.toList())
        val scene = Scene(graphs, 800.0, 600.0)

        stage.scene = scene
        stage.show()

        Timer().scheduleAtFixedRate(0, 30000) {
            Platform.runLater {update()}
        }
    }

    fun connect() {
        connectBinary(context.acceptorGroup, host, port, 10000) { c, e ->
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

        server.clientGetStats(lastUpdate.millis + 1, DateTime.now().millis, password) { r, e ->
            Platform.runLater {
                if(e == null) {
                    onUpdate(r!!)
                } else {
                    onUpdateError(e)
                }
            }
        }
    }

    fun onUpdate(packet: List<TimeMetric>) {
        println("Received update: $packet")

        packet.lastOrNull()?.let {
            lastUpdate = it.time
        }

        packet.forEach { p ->
            val time = p.time.millis
            p.categories.forEach { c ->
                val baseCategory = c.key.substringBefore('.')
                val category = categories.getOrAdd(baseCategory) { Category(c.value.unit) }

                category.overallPaths.add("Average", time, c.value.metric.average)
                category.overallPaths.add("Median", time, c.value.metric.median)
                category.overallPaths.add("Max", time, c.value.metric.max)
                category.overallStats.add(c.key.substringAfter('.'), time, c.value.metric.average)

                c.value.servers.forEach { s ->
                    val server = category.servers.getOrAdd(s.key) { Server(c.value.unit) }
                    server.paths.add("Average", time, s.value.metric.average)
                    server.paths.add("Median", time, s.value.metric.median)
                    server.paths.add("Max", time, s.value.metric.max)
                }
            }
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

fun <K, V> ObservableMap<K, V>.toList(): ObservableList<V> {
    val list = FXCollections.observableArrayList<V>()
    addListener(MapChangeListener {
        if(it.wasAdded()) {
            list.add(it.valueAdded)
        } else if(it.wasRemoved()) {
            list.remove(it.valueRemoved)
        }
    })

    return list
}