package com.home.zmart.zmarthome

import android.content.Context
import android.content.SharedPreferences
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
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.EditText
import android.widget.LinearLayout




class MainActivity : AppCompatActivity() {
    private var handler: Handler? = null
    var selectedSwitch = "FIRST"
    private var toggleUrl = "http://192.168.0.100:8082/toggle?sw=$selectedSwitch"
    var statusUrl = "http://192.168.0.100:8082/status?sw=$selectedSwitch"
    var username = ""
    var password = ""
    var queue: RequestQueue? = null
    var statusValue = ""
    var isRunFirstTime = true
    var isCreditsFilledFirstTime = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)

        setToggleButtonListener()
        setButtonListener()
        setButtonSettingsListener(sharedPref)

        username = sharedPref.getString("username","")
        password = sharedPref.getString("password","")

        queue = Volley.newRequestQueue(this)
        handler = Handler()

        if(!(username.isNotEmpty() && password.isNotEmpty())) {
            displayAlert(sharedPref)
        }else{
            isCreditsFilledFirstTime = true
        }


        handler!!.postDelayed(checkStatus, 0)


    }

    private fun displayAlert(sharedPref: SharedPreferences) {
        val alert = AlertDialog.Builder(this)
        var loginText: EditText?
        var passwordText: EditText?

        // Builder
        with(alert) {
            setTitle("Fill cridentials to gain access")

            loginText = EditText(context)
            loginText!!.hint = "login"
            loginText!!.inputType = InputType.TYPE_CLASS_TEXT

            passwordText = EditText(context)
            passwordText!!.hint = "password"
            passwordText!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            setPositiveButton("Save") { dialog, whichButton ->
                username = loginText!!.text.toString()
                password = passwordText!!.text.toString()
                with (sharedPref.edit()) {
                    putString("username", username)
                    putString("password",password)
                    commit()
                }
                isCreditsFilledFirstTime = true
                dialog.dismiss()
            }
            if (!isRunFirstTime) {
                setNegativeButton("Cancel") { dialog, whichButton ->
                    dialog.dismiss()
                }
            }
        }

        // Dialog
        val dialog = alert.create()
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.addView(loginText)
        layout.addView(passwordText)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setView(layout)
        dialog.show()
    }

    private val checkStatus = object : Runnable {
        override fun run() {
            if (isCreditsFilledFirstTime) {
                sendRequestAboutStatus(statusUrl)
            }
                handler!!.postDelayed(this, 1500)

        }
    }

    private fun setButtonListener() {
        button.setOnClickListener {
            toggle()
        }
    }

    private fun setToggleButtonListener() {
        toggleButton.setOnClickListener {
            selectedSwitch = if (Integer.parseInt(toggleButton.text.toString()) == 1) {
                "FIRST"
            } else {
                "SECOND"
            }
            updateUrls()
        }
    }

    private fun setButtonSettingsListener(sharedPref: SharedPreferences) {
        buttonSettings.setOnClickListener {
            displayAlert(sharedPref)
        }
    }

    private fun toggle() {
        val stringRequest = object : StringRequest(Method.GET, toggleUrl,
                Response.Listener { response ->
                    logResponse(response)
                    textView_status.text = ""
                    statusValue = response.toString()
                },

                Response.ErrorListener { error ->
                    textView_status.text = "ERROR: " + error.toString()
                }) {
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
        headers["Content-Type"] = "application/json"
        headers["Authorization"] = auth
        return headers
    }

    private fun sendRequestAboutStatus(url: String) {
        val stringRequest = object : StringRequest(Method.GET, url,
                Response.Listener { response ->
                    logResponse(response)
                    animateShowingButton()
                    makePulseEffect()
                    statusValue = response.toString()
                    setStatus()
                },
                Response.ErrorListener { error ->
                    Log.d("ERROR", error.toString())
                    textView_status.text = "Server is not responding..."
                    animateShowingButton()
                }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                return setHeaders()
            }
        }
        queue!!.add(stringRequest)
    }

    private fun animateShowingButton() {
        if (isRunFirstTime) {
            animateVisibilityOfButton()
            isRunFirstTime = false
        }
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

    private fun pulse(color1: Int, color2: Int) {
        val color = arrayOf(ColorDrawable(color1), ColorDrawable(color2))
        val trans = TransitionDrawable(color)
        layout.background = trans
        trans.startTransition(300)
    }

    private fun logResponse(response: String?) {
        Log.d("RESPONSE", "Response is: $response")
    }

    private fun setStatus() {
        when {
            statusValue.equals("on") -> {
                textView_status.text = ""
                button.setImageResource(R.drawable.button_off)

            }
            statusValue.equals("off") -> {
                textView_status.text = ""
                button.setImageResource(R.drawable.button_on)
            }
            statusValue.equals("error 401") -> textView_status.text = "Authorization failed, check cridentials"
            statusValue.equals("error 501") -> textView_status.text = "Sonoff is not responding..."
        }
    }

    private fun updateUrls() {
        toggleUrl = "http://192.168.0.100:8082/toggle?sw=$selectedSwitch"
        statusUrl = "http://192.168.0.100:8082/status?sw=$selectedSwitch"
    }
}
