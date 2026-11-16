package com.netysloy.rustgram

object TgCore {
    // ВСТАВЬ СВОИ КЛЮЧИ из https://my.telegram.org (API development tools)
    const val API_ID = 1234567             // замени
    const val API_HASH = "вставь_свой_api_hash"  // замени

    init { System.loadLibrary("core") }

    external fun init(apiId: Int, apiHash: String)
    external fun login(phone: String, code: String): String
    external fun lookup(target: String): String
    external fun startArchive()
}
