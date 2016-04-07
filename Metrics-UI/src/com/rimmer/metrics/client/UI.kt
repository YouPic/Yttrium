package com.rimmer.metrics.client

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.stage.Stage
import javafx.util.StringConverter
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import java.net.InetAddress
import java.util.*

/*
/** The ElasticSearch configuration. */
val elasticCluster = System.getenv("ELASTIC_CLUSTER") ?: "bb4fc4f2225aaadaa61abd68ec38b820"
val elasticRegion = System.getenv("ELASTIC_REGION") ?: "eu-west-1"
val elasticPort = Integer.parseInt(System.getenv("ELASTIC_PORT") ?: "9343")
val elasticUser = System.getenv("ELASTIC_USER") ?: "admin"
val elasticPassword = System.getenv("ELASTIC_PASSWORD") ?: "fgkf4h5ees"

fun ceilTimeHour(time: DateTime) = time.withTime(time.hourOfDay + 1, 0, 0, 0)

fun parseStatPoint(stat: Map<*, *>, point: String) = XYChart.Data<Number, Number>(
    DateTime.parse(stat["time"] as String).millis, (stat["stat"] as Map<String, Number>)[point]
)

class MetricModel {
    inner class Point {
        var totalTime = 0L
        var totalCalls = 0L
    }

    val search = TransportClient.builder()
        .addPlugin(ShieldPlugin::class.java)
        .settings(Settings.settingsBuilder()
            .put("cluster.name", elasticCluster)
            .put("shield.transport.ssl", true)
            .put("request.headers.X-Found-Cluster", elasticCluster)
            .put("shield.user", "${elasticUser}:${elasticPassword}")
            .build()
        ).build().addTransportAddress(
            InetSocketTransportAddress(InetAddress.getByName("$elasticCluster.$elasticRegion.aws.found.io"), elasticPort)
    )

    val timeMap = HashMap<Long, Point>()

    fun update() {
        val result = search.prepareSearch("metrics").setTypes("stat").execute().addListener(object: ActionListener<SearchResponse> {
            override fun onFailure(e: Throwable) {}
            override fun onResponse(response: SearchResponse) {
                response.hits.hits.forEach {
                    val entry = it.source
                    val date = DateTime.parse(entry["time"] as String)
                    //val point = timeMap.putIfAbsent(date.millis / 60000, Point())
                    val stat = entry["stat"] as Map<String, Number>
                    val average = stat["average"]!!
                    val count = stat["count"]!!

                    point.totalCalls += count.toLong()
                    point.totalTime += average.toLong() * count.toLong()
                }
            }
        })


        result.hits.hits.forEach {
            val data = it.source
            average.data.add(parseStatPoint(data, "average"))
            median.data.add(parseStatPoint(data, "median"))
            max.data.add(parseStatPoint(data, "max"))
        }
    }
}

class MetricsUI: Application() {


    override fun start(stage: Stage) {
        stage.title = "Metrics UI"

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

        val chart = LineChart<Number, Number>(xAxis, yAxis)
        chart.title = "Overall times"

        val average = XYChart.Series<Number, Number>()
        average.name = "Average"

        val median = XYChart.Series<Number, Number>()
        median.name = "Median"

        val max = XYChart.Series<Number, Number>()
        max.name = "Max"

        val result = search.prepareSearch("metrics").setTypes("stat").execute().actionGet()
        result.hits.hits.forEach {
            val data = it.source
            average.data.add(parseStatPoint(data, "average"))
            median.data.add(parseStatPoint(data, "median"))
            max.data.add(parseStatPoint(data, "max"))
        }

        val scene = Scene(chart, 800.0, 600.0)
        chart.data.add(average)
        chart.data.add(median)
        chart.data.add(max)

        stage.scene = scene
        stage.show()
    }
}

fun main(args: Array<String>) = Application.launch(MetricsUI::class.java, *args)*/