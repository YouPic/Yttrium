
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
