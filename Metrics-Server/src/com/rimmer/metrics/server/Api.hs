
import com.rimmer.metrics
import com.rimmer.yttrium.router.plugin.IPAddress

api Server where
    post statistic where stats: {StatPacket}; ip: IPAddress
    post error where errors: {ErrorPacket}; ip: IPAddress
    post profile where profiles: {ProfilePacket}; ip: IPAddress

api Client where
    get stats/(from: Long)/(to: Long) -> {TimeMetric} [password = True]
    get profile/(from: Long)/(to: Long) -> {TimeProfile} [password = True]
    get error/(from: Long) -> {ErrorClass} [password = True]

plugin PasswordPlugin where
    when properties has "password"
    parameters
        add "password": String

plugin IPPlugin where
    when args has IPAddress
    parameters
        handle IPAddress
