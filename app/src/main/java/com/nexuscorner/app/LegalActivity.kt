package com.nexuscorner.app

import android.app.Activity
import android.os.Bundle
import android.text.Html
import android.widget.TextView

class LegalActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_legal)
        findViewById<TextView>(R.id.tvBody).text =
            Html.fromHtml(getString(R.string.legal_html), Html.FROM_HTML_MODE_COMPACT)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
    }
}
