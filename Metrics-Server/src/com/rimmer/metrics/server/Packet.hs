
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

data StatResponse where
    slices: {StatSlice}

-- Profiling data types.
data ProfileEvent where
    group: String
    event: String
    startTime: Long
    endTime: Long

data ProfileEntry where
    start: Long
    end: Long
    events: {ProfileEvent}

data ProfileStat where
    normal: ProfileEntry
    max: ProfileEntry

data ProfileSlice where
    time: Date
    paths: {String -> ProfileStat}

data ProfileResponse where
    slices: {ProfileSlice}

data Error where
    path: String
    latest: Date
    count: Int
    cause: String
    trace: String

data ErrorResponse where
    errors: {Error}
