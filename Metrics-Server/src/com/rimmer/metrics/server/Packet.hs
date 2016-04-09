
-- Statistics data types.
data StatEntry where
    median: Float
    average: Float
    average95: Float
    average99: Float
    max: Float
    min: Float
    count: Int

data StatSlice where
    time: Date
    global: StatEntry
    paths: {String -> StatEntry}

data StatsPacket where
    slices: {StatSlice}

-- Profiling data types.
data Event where
    group: String
    event: String
    startTime: Long
    endTime: Long

data ProfileEntry where
    start: Long
    end: Long
    events: {Event}

data ProfileStat where
    normal: ProfileEntry
    max: ProfileEntry

data ProfileSlice where
    time: Date
    paths: {String -> ProfileStat}

data ProfilesPacket where
    slices: {ProfileSlice}
