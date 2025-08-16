package com.orchestrator.starter

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var etPrompt: EditText
    private lateinit var tOpenAI: CheckBox
    private lateinit var tGrok: CheckBox
    private lateinit var tGemini: CheckBox
    private lateinit var btnAskAll: Button
    private lateinit var btnAskOpenAI: Button
    private lateinit var btnAskGrok: Button
    private lateinit var btnAskGemini: Button
    private lateinit var status: TextView
    private lateinit var rawOpenAI: TextView
    private lateinit var rawGrok: TextView
    private lateinit var rawGemini: TextView
    private lateinit var unified: TextView

    private val http = OkHttpClient()
    // შეცვალე შენი Worker-ის გზით:
    private val endpoint = "https://example.workers.dev/ask"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etPrompt = findViewById(R.id.etPrompt)
        tOpenAI = findViewById(R.id.tOpenAI)
        tGrok = findViewById(R.id.tGrok)
        tGemini = findViewById(R.id.tGemini)
        btnAskAll = findViewById(R.id.btnAskAll)
        btnAskOpenAI = findViewById(R.id.btnAskOpenAI)
        btnAskGrok = findViewById(R.id.btnAskGrok)
        btnAskGemini = findViewById(R.id.btnAskGemini)
        status = findViewById(R.id.status)
        rawOpenAI = findViewById(R.id.rawOpenAI)
        rawGrok = findViewById(R.id.rawGrok)
        rawGemini = findViewById(R.id.rawGemini)
        unified = findViewById(R.id.unified)

        btnAskAll.setOnClickListener { ask(listOf("OpenAI","Grok","Gemini")) }
        btnAskOpenAI.setOnClickListener { ask(listOf("OpenAI")) }
        btnAskGrok.setOnClickListener { ask(listOf("Grok")) }
        btnAskGemini.setOnClickListener { ask(listOf("Gemini")) }
    }

    private fun ask(targets: List<String>) {
        val active = mutableListOf<String>()
        if (tOpenAI.isChecked && "OpenAI" in targets) active += "OpenAI"
        if (tGrok.isChecked && "Grok" in targets) active += "Grok"
        if (tGemini.isChecked && "Gemini" in targets) active += "Gemini"
        if (active.isEmpty()) { toast("აირჩიე მინ. ერთი პროვაიდერი"); return }

        val prompt = etPrompt.text.toString().trim()
        if (prompt.isEmpty()) { toast("ჩაწერე კითხვაც :)"); return }

        status.text = "იგზავნება..."
        rawOpenAI.text = "OpenAI: —"
        rawGrok.text   = "Grok: —"
        rawGemini.text = "Gemini: —"
        unified.text   = "მოლოდინი…"

        val bodyJson = JSONObject().apply {
            put("prompt", prompt)
            put("providers", active)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val req = Request.Builder()
            .url(endpoint)
            .post(RequestBody.create(mediaType, bodyJson.toString()))
            .build()

        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    status.text = "შეცდომა: ${e.message}"
                    toast("ვერ გაიგზავნა")
                }
            }

            override fun onResponse(call: Call, resp: Response) {
                resp.use {
                    if (!it.isSuccessful) {
                        runOnUiThread {
                            status.text = "HTTP ${it.code()}"
                            toast("სერვერის შეცდომა")
                        }
                        return
                    }
                    val txt = it.body()?.string().orEmpty()

                    runOnUiThread {
                        status.text = "მზადაა"

                        val r = try { JSONObject(txt) } catch (_: Exception) { null }
                        if (r == null) {
                            unified.text = txt
                        } else {
                            val openai = r.opt("openai")?.toString() ?: "—"
                            val grok   = r.opt("grok")?.toString()   ?: "—"
                            val gemini = r.opt("gemini")?.toString() ?: "—"
                            val uni    = r.opt("unified")?.toString() ?: txt

                            rawOpenAI.text = "OpenAI: $openai"
                            rawGrok.text   = "Grok: $grok"
                            rawGemini.text = "Gemini: $gemini"
                            unified.text   = uni
                        }
                    }
                }
            }
        })
    }

    private fun toast(s:String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}

// extension for MediaType
private fun String.toMediaType(): MediaType = MediaType.parse(this)!!
