package com.rimmer.metrics

import com.rimmer.metrics.generated.type.MetricUnit
import io.netty.channel.EventLoopGroup
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit

/** Monitors the state of this system and creates metrics for important state. */
fun Metrics.monitorServer(loop: EventLoopGroup) {
    val totalMem = registerStat("System.memory.total", Metrics.Accumulator.set, Metrics.Scope.global, MetricUnit.ByteUnit)
    val usedMem = registerStat("System.memory.used", Metrics.Accumulator.max, Metrics.Scope.local, MetricUnit.ByteUnit)
    val heapMem = registerStat("System.memory.process", Metrics.Accumulator.max, Metrics.Scope.local, MetricUnit.ByteUnit)
    val nonHeapMem = registerStat("System.memory.process.nonheap", Metrics.Accumulator.max, Metrics.Scope.local, MetricUnit.ByteUnit)
    val totalDisk = registerStat("System.disk.total", Metrics.Accumulator.set, Metrics.Scope.global, MetricUnit.ByteUnit)
    val usedDisk = registerStat("System.disk.used", Metrics.Accumulator.max, Metrics.Scope.local, MetricUnit.ByteUnit)
    val processCpu = registerStat("System.cpu.process", Metrics.Accumulator.max, Metrics.Scope.local, MetricUnit.FractionUnit)
    val systemCpu = registerStat("System.cpu.total", Metrics.Accumulator.max, Metrics.Scope.local, MetricUnit.FractionUnit)
    val files = registerStat("System.disk.descriptors", Metrics.Accumulator.max, Metrics.Scope.local)
    val maxFiles = registerStat("System.disk.max_descriptors", Metrics.Accumulator.max, Metrics.Scope.local)

    val sysBean = ManagementFactory.getOperatingSystemMXBean()
    val extendedBean = sysBean as? com.sun.management.OperatingSystemMXBean
    val unixBean = sysBean as? com.sun.management.UnixOperatingSystemMXBean
    val memBean = ManagementFactory.getMemoryMXBean()
    val fileSys = File.listRoots().first()

    loop.scheduleAtFixedRate({
        val heap = memBean.heapMemoryUsage
        val nonHeap = memBean.nonHeapMemoryUsage

        setStat(heapMem, heap.used)
        setStat(nonHeapMem, nonHeap.used)
        setStat(totalDisk, fileSys.totalSpace)
        setStat(usedDisk, fileSys.totalSpace - fileSys.freeSpace)

        if(extendedBean !== null) {
            setStat(systemCpu, Math.max((extendedBean.systemCpuLoad * 1000000).toLong(), 0))
            setStat(processCpu, Math.max((extendedBean.processCpuLoad * 1000000).toLong(), 0))

            val memory = extendedBean.totalPhysicalMemorySize
            setStat(totalMem, memory)
            setStat(usedMem, memory - extendedBean.freePhysicalMemorySize)
        }

        if(unixBean !== null) {
            setStat(files, unixBean.openFileDescriptorCount)
            setStat(maxFiles, unixBean.maxFileDescriptorCount)
        }
    }, 1, 1, TimeUnit.MINUTES)
}
