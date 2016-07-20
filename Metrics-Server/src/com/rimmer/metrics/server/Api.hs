
import com.rimmer.metrics

api Server where
    post statistic where stat: StatPacket
    post error where error: ErrorPacket
    post profile where profile: ProfilePacket

api Client where
    get stats/(from: Long)/(to: Long) -> StatResponse [password = True]
    get profile/(from: Long)/(to: Long) -> ProfileResponse [password = True]
    get error/(from: Long) -> ErrorResponse [password = True]

plugin PasswordPlugin where
    when properties has "password"
    parameters
        add "password": String
