
import com.rimmer.metrics
import com.rimmer.yttrium.router.plugin.IPAddress

api Server where
    post metric where
        packets: {MetricPacket}
        serverName: String
        ip: IPAddress

api Client where
    get stats/(from: Long)/(to: Long) -> {TimeMetric} [password = True]
    get profile/(from: Long)/(to: Long) -> {TimeProfile} [password = True]
    get error/(from: Long) -> {ErrorClass} [password = True]

plugin PasswordPlugin where
    when properties has "password"
    parameters
        add "password": String

plugin AddressPlugin where
    when args has IPAddress
    parameters
        handle IPAddress
