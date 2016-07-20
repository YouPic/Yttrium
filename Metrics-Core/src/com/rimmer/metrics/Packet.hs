
data StatPacket where
    path: String
    server: String
    time: Date
    totalElapsed: Long
    intervals: {Interval}

data ErrorPacket where
    path: String
    time: Date
    cause: String
    trace: String

data ProfilePacket where
    path: String
    server: String
    time: Date
    start: Long
    end: Long
    events: {Event}

data Event where
    event: EventType
    "type": String
    startTime: Long
    endTime: Long

data EventType = Redis | MySQL | MySQLGen | MySQLProcess | Mongo | Serialize

data Interval where
    start: Long
    end: Long
