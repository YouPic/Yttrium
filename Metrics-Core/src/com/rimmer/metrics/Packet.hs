
data MetricPacket
  = StatPacket Stat
  | ProfilePacket Profile
  | ErrorPacket Error

-- Determines how the metric data will be shown.
-- Times are interpreted as nanoseconds.
-- Fractions are interpreted as x/1000000.
data MetricUnit = TimeUnit | ByteUnit | CountUnit | FractionUnit

type Stat = (
    location: ?String,
    category: String,
    time: Date,
    sampleCount: Int,
    unit: MetricUnit,
    total: Long,
    min: Long,
    max: Long,
    median: Long,
    average95: Long,
    average99: Long
 )

type Profile = (
    location: ?String,
    category: String,
    time: Date,
    start: Long,
    end: Long,
    events: {Event}
 )

type Error = (
    location: ?String,
    category: String,
    fatal: Bool,
    time: Date,
    cause: String,
    description: String,
    trace: String,
    count: Int
 )

data Event where
    "type": String
    description: String
    startTime: Long
    endTime: Long
