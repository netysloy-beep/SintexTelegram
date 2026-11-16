package com.netysloy.rustgram

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val phone = EditText(this).apply { hint = "Номер: +375291234567" }
        val code = EditText(this).apply { hint = "Код из Telegram" }
        val btn = Button(this).apply { text = "Войти" }
        val target = EditText(this).apply { hint = "@юзернейм другого человека" }
        val btn2 = Button(this).apply { text = "Узнать ID" }
        val out = TextView(this).apply { text = "RustGram. Данные хранятся приватно." }
        layout.addView(phone); layout.addView(code); layout.addView(btn)
        layout.addView(target); layout.addView(btn2); layout.addView(out)
        setContentView(layout)

        btn.setOnClickListener {
            btn.isEnabled = false
            Thread {
                TgCore.init(TgCore.API_ID, TgCore.API_HASH)
                val res = TgCore.login(phone.text.toString(), code.text.toString())
                runOnUiThread {
                    out.text = res
                    if (res.startsWith("ID:")) TgCore.startArchive()
                    btn.isEnabled = true
                }
            }.start()
        }
        btn2.setOnClickListener {
            Thread {
                val res = TgCore.lookup(target.text.toString())
                runOnUiThread { out.text = res }
            }.start()
        }
    }
}
