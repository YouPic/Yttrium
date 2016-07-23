
data MetricPacket
  = StatPacket Stat
  | ProfilePacket Profile
  | ErrorPacket Error

type Stat = (
    location: ?String,
    category: String,
    time: Date,
    sampleCount: Int,
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
