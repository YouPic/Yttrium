
-- Statistics data types.
data Metric where
    median: Float
    average: Float
    average95: Float
    average99: Float
    max: Float
    min: Float
    count: Int

data ServerMetric where
    metric: Metric
    paths: {String -> Metric}

data CategoryMetric where
    metric: Metric
    unit: MetricUnit
    servers: {String -> ServerMetric}

data TimeMetric where
    time: Date
    categories: {String -> CategoryMetric}

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

data CategoryProfile where
    profile: ProfileStat
    paths: {String -> ProfileStat}

data TimeProfile where
    time: Date
    servers: {String -> {String -> CategoryProfile}}

-- Error data types.
data ErrorInstance where
    time: Date
    trace: String
    path: ?String

data ErrorClass where
    cause: String
    count: Int
    fatal: Bool
    lastError: Date
    servers: {String -> {ErrorInstance}}
