package com.codingblocks.whatsappopener

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OpenChatActivity : AppCompatActivity() {

    companion object {
        fun isNumber(number: String): Boolean {
            val regex =
                Regex("^(?:(?:\\+|0{0,2})[0-9]|[0-9][0-9](\\s*[\\-]\\s*)?|[0]?)?[1-9]\\d{9}$")
            return regex.matches(number)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        var number = ""
        super.onCreate(savedInstanceState)
        if (intent.action == Intent.ACTION_PROCESS_TEXT) number =
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString()
        else if (intent.action == Intent.ACTION_DIAL || intent.action == Intent.ACTION_VIEW)
            number = intent?.data?.schemeSpecificPart.toString()


        if (isNumber(number)) {
            startWhatsApp(number)
        } else {
            Toast.makeText(this, "Please Check The Number", Toast.LENGTH_SHORT).show()
            finish()
        }
    }


    private fun startWhatsApp(number: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setPackage("com.whatsapp")
        val data: String = if (number[0] == '+') {
            number.substring(1)
        } else {
            number
        }
        if (number.length == 10)
            Toast.makeText(this, "Please Add Country Code", Toast.LENGTH_LONG).show()
        else {
            intent.data = Uri.parse("https://wa.me/$data")
            intent.setPackage("com.whatsapp")
            if (packageManager.resolveActivity(intent, 0) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please install whatsApp", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }


}
