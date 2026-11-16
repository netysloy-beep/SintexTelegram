package com.netysloy.rustgram

object TgCore {
    // Ключи приложения nordlink с https://my.telegram.org
    const val API_ID = 27367222
    const val API_HASH = "9cf9adcd1c63897798a147fe06c319ff"

    init { System.loadLibrary("core") }

    external fun init(apiId: Int, apiHash: String)
    external fun login(phone: String, code: String): String
    external fun lookup(target: String): String
    external fun startArchive()
}
