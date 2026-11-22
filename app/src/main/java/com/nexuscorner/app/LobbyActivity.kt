package com.nexuscorner.app

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class LobbyActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private fun <T : View> v(id: Int): T = findViewById(id)

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_lobby)
        prefs = getSharedPreferences("nc", MODE_PRIVATE)
        val legal = { _: View -> startActivity(Intent(this, LegalActivity::class.java)) }
        v<TextView>(R.id.tvFooter).text = "Nexus Corner v" + packageManager.getPackageInfo(packageName, 0).versionName + "  ·  Termos e direitos autorais"
        v<View>(R.id.tvFooter).setOnClickListener(legal)
        v<View>(R.id.btnInfo).setOnClickListener(legal)
        v<View>(R.id.btnSpace).setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        v<View>(R.id.btnTools).setOnClickListener { startActivity(Intent(this, MainActivity::class.java).putExtra("tab", "tools")) }
        v<View>(R.id.btnAdd).setOnClickListener { Lib.addDialog(this, prefs) { build() } }
    }

    override fun onResume() {
        super.onResume()
        val mi = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        v<TextView>(R.id.tvRam).text = "${100 - mi.availMem * 100 / mi.totalMem}%"
        val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val sc = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        v<TextView>(R.id.tvBat).text = "${(b?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0) * 100 / sc}%"
        v<TextView>(R.id.tvTemp).text = "%.0f°C".format((b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0)
        build()
    }

    private fun build() {
        val pm = packageManager
        val list = v<LinearLayout>(R.id.list)
        list.removeAllViews()
        val pkgs = Lib.get(this, prefs).filter { Lib.installed(pm, it) }
        v<TextView>(R.id.tvCount).text = "${pkgs.size}"
        v<View>(R.id.tvEmpty).visibility = if (pkgs.isEmpty()) View.VISIBLE else View.GONE
        for (p in pkgs) {
            val c = layoutInflater.inflate(R.layout.item_lobby, list, false)
            val ic = pm.getApplicationIcon(p)
            c.findViewById<ImageView>(R.id.lBg).setImageDrawable(ic)
            c.findViewById<ImageView>(R.id.lIcon).setImageDrawable(ic)
            c.findViewById<TextView>(R.id.lName).text = Lib.label(pm, p)
            c.findViewById<TextView>(R.id.lVer).text = "v" + (pm.getPackageInfo(p, 0).versionName ?: "")
            c.setOnClickListener { startActivity(Intent(this, MainActivity::class.java).putExtra("select", p)) }
            list.addView(c)
        }
    }
}
