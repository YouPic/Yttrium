
import com.rimmer.metrics

api Server where
    post statistic where stat: StatPacket
    post error where error: ErrorPacket
    post profile where profile: ProfilePacket

api Client where
    get stats/(from: Long)/(to: Long) -> StatResponse where password: String
    get profile/(from: Long)/(to: Long) -> ProfileResponse where password: String
    get error/(from: Long) -> ErrorResponse where password: String
