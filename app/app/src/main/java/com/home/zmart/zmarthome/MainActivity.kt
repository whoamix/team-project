package com.home.zmart.zmarthome

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import android.util.Base64.NO_WRAP
import com.android.volley.*
import android.graphics.drawable.TransitionDrawable
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.animation.AlphaAnimation

class MainActivity : AppCompatActivity() {
    private var handler: Handler? = null
    val url = "http://192.168.0.15:8082"
    val username = "TheBestTeam"
    val password = "WiesioKiller"
    var queue: RequestQueue? = null
    var statusValue = ""
    var isRunFirstTime = true
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        queue = Volley.newRequestQueue(this)
        handler = Handler()
        handler!!.postDelayed(checkStatus, 0)


        setButtonListener(url)
    }

    private val checkStatus = object : Runnable {
        override fun run() {
            sendRequestAboutStatus(url + "/status")
            handler!!.postDelayed(this, 1500)

        }
    }

    private fun setButtonListener(url: String) {
        button.setOnClickListener {
            toggle(url + "/toggle")
        }
    }

    private fun toggle(url: String) {
        val stringRequest = object : StringRequest(Method.GET, url,
                Response.Listener { response ->
                    logResponse(response)
                    textView_status.text = ""
                    statusValue = response.toString()
                },

                Response.ErrorListener { error ->
                    textView_status.text = "ERROR: "+ error.toString()
                })
        {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                return setHeaders()
            }
        }
        queue!!.add(stringRequest)
    }

    private fun setHeaders(): HashMap<String, String> {
        val headers = HashMap<String, String>()
        val credentials = "$username:$password"
        val auth = "Basic " + Base64.encodeToString(credentials.toByteArray(), NO_WRAP)
        headers.put("Content-Type", "application/json")
        headers.put("Authorization", auth)
        return headers
    }

    private fun sendRequestAboutStatus(url: String) {
        val stringRequest = object : StringRequest(Method.GET, url,
                Response.Listener { response ->
                    logResponse(response)
                    if(isRunFirstTime) {
                        animateVisibilityOfButton()
                        isRunFirstTime = false
                    }
                    makePulseEffect()
                    statusValue = response.toString()
                    setStatus()
                },
                Response.ErrorListener { error ->
                    textView_status.text = "Server is not responding..."
                })
        {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                return setHeaders()
            }
        }
        queue!!.add(stringRequest)
    }

    private fun animateVisibilityOfButton() {
        val animation1 = AlphaAnimation(0.0f, 1f)
        animation1.duration = 2000
        animation1.startOffset = 350
        animation1.fillAfter = true
        button.visibility = View.VISIBLE
        button.startAnimation(animation1)
    }

    private fun makePulseEffect() {
        pulse(this.getColor(R.color.grey), this.getColor(R.color.white))
        pulse(this.getColor(R.color.white), this.getColor(R.color.grey))
    }

    private fun pulse(color1: Int,color2: Int) {
        var color = arrayOf(ColorDrawable(color1), ColorDrawable(color2))
        var trans = TransitionDrawable(color)
        layout.setBackground(trans)
        trans.startTransition(300)
    }

    private fun logResponse(response: String?) {
        Log.d("RESPONSE", "Response is: " + response)
    }

    private fun setStatus() {
        if (statusValue.equals("on")) {
            textView_status.text = ""
            button.setImageResource(R.drawable.button_off)

        } else if(statusValue.equals("off")) {
            textView_status.text = ""
            button.setImageResource(R.drawable.button_on)
        }else{
            textView_status.text = "Sonof is not responding..."

        }
    }
}
