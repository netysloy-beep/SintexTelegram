package com.netysloy.rustgram

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("rustgram", MODE_PRIVATE)
        askPinOrUnlock()
    }

    private fun askPinOrUnlock() {
        val saved = prefs.getString("pin", null)
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = if (saved == null) "Придумайте PIN (4+ цифр)" else "Введите PIN"
        }
        AlertDialog.Builder(this)
            .setTitle(if (saved == null) "Защита приложения" else "Вход")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                val v = input.text.toString()
                if (saved == null) {
                    if (v.length >= 4) {
                        prefs.edit().putString("pin", v).apply()
                        startUi()
                    } else askPinOrUnlock()
                } else if (v == saved) startUi() else askPinOrUnlock()
            }
            .show()
    }

    private fun startUi() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val phone = EditText(this).apply { hint = "Номер: +375291234567" }
        val code = EditText(this).apply { hint = "Код из Telegram" }
        val btn = Button(this).apply { text = "Войти" }
        val target = EditText(this).apply { hint = "@юзернейм другого человека" }
        val btn2 = Button(this).apply { text = "Узнать ID" }
        val autoText = EditText(this).apply { hint = "Текст автоответа (пусто = выкл)" }
        val btn3 = Button(this).apply { text = "Включить автоответчик" }
        val out = TextView(this).apply { text = "RustGram. Данные хранятся приватно." }
        layout.addView(phone); layout.addView(code); layout.addView(btn)
        layout.addView(target); layout.addView(btn2)
        layout.addView(autoText); layout.addView(btn3); layout.addView(out)
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
        btn3.setOnClickListener {
            TgCore.setAutoReply(autoText.text.toString())
            out.text = if (autoText.text.isBlank()) "Автоответчик выключен" else "Автоответчик включен"
        }
    }
}
