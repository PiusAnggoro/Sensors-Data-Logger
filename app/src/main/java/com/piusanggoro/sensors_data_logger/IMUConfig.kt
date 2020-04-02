package com.piusanggoro.sensors_data_logger

import java.io.Serializable

class IMUConfig : Serializable {
    var folderPrefix = ""
    var outputFolder = ""

    val suffix: String
        get() = "_piusanggoro"
}