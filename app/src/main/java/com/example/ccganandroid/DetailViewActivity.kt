package com.example.ccganandroid

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class DetailViewActivity : AppCompatActivity() {

    private val tag: String = this.javaClass.name
    private lateinit var name: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_view)

        // Handle incoming intenet
        intent.also {
            handleIntent(intent)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun handleIntent(intent: Intent?) {
        if (intent != null) {
            val jsonString = intent.getStringExtra("itemData")

            if (jsonString != null) {
                Log.d(tag, jsonString)

                // Create JSONObject from String
                val jsonObject = JSONObject(jsonString)

                window.findViewById<TextView>(R.id.tvName).text = jsonObject.getString("name")
                name = jsonObject.getString("name")

                val bmd = MainActivity.imageCache[jsonObject.get("img")]
                if (bmd != null) {
                    window.findViewById<ImageView>(R.id.imageView).setImageDrawable(bmd)
                }
                val oArray = jsonObject.getJSONArray("occupation")
                val sb: StringBuilder = StringBuilder()
                for (i in 0 until oArray.length()) {
                    sb.append(oArray[i])

                    if (i != oArray.length()-1){
                        sb.append("\n")
                    }

                }
                window.findViewById<TextView>(R.id.tvOccupation).text = sb.toString()
                window.findViewById<TextView>(R.id.tvStatus).text = jsonObject.getString("status")
                window.findViewById<TextView>(R.id.tvNickname).text = jsonObject.getString("nickname")

                val aArray = jsonObject.getJSONArray("appearance")
                val sba: StringBuilder = StringBuilder()
                for (i in 0 until aArray.length()) {
                    sba.append(aArray[i])

                    if (i != aArray.length()-1){
                        sba.append(",")
                    }

                }
                window.findViewById<TextView>(R.id.tvAppearance).text = sba.toString()

            }
        }
    }

    // onClickClose
    fun onClickClose() {
        finish() // close activity
    }

}