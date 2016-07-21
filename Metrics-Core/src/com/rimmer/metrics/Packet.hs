
data StatPacket where
    location: ?String
    category: String
    time: Date
    sampleCount: Int
    total: Long
    min: Long
    max: Long
    median: Long
    average95: Long
    average99: Long

data ErrorPacket where
    location: ?String
    category: String
    fatal: Bool
    time: Date
    cause: String
    description: String
    trace: String

data ProfilePacket where
    location: ?String
    category: String
    time: Date
    start: Long
    end: Long
    events: {Event}

data Event where
    "type": String
    description: String
    startTime: Long
    endTime: Long
