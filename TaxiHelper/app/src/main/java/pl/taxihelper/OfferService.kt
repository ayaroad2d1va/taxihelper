package pl.taxihelper
import android.accessibilityservice.AccessibilityService
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView

class OfferService : AccessibilityService() {
    private val h = Handler(Looper.getMainLooper())
    private var box: LinearLayout? = null
    private var t1: TextView? = null
    private var t2: TextView? = null
    private var lastRun = 0L
    private var lastOfferSeen = 0L
    private var lastRideEnd = System.currentTimeMillis()
    private var lastKey = ""
    private val hide = Runnable { box?.visibility = View.GONE; lastKey = "" }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val now = System.currentTimeMillis()
        if (now - lastRun < 400) return
        lastRun = now
        var found: Offer? = null
        for (w in windows) {                          // każde okno osobno => rozróżnia 2 aplikacje naraz
            val root = w.root ?: continue
            val pkg = root.packageName?.toString() ?: continue
            val app = Engine.detect(pkg) ?: continue
            val tx = ArrayList<String>(); Parser.texts(root, tx)
            if (Parser.RIDE_END.any { k -> tx.any { it.lowercase().contains(k) } }) lastRideEnd = now
            Parser.parse(app, tx)?.let { found = it; Log.d("TaxiHelper", "$pkg -> $it") }
            root.recycle()
        }
        val o = found ?: return
        lastOfferSeen = now
        h.removeCallbacks(hide); h.postDelayed(hide, 2500)   // znika, gdy oferta zniknie z ekranu
        val key = o.toString()
        if (key == lastKey) return
        lastKey = key
        show(o, Engine.evaluate(this, o, (now - lastRideEnd) / 60000))
    }

    private fun show(o: Offer, r: Result) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (box == null) {
            t1 = TextView(this).apply { setTextColor(Color.WHITE); textSize = 22f; setTypeface(typeface, Typeface.BOLD) }
            t2 = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f }
            box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 28, 40, 28); addView(t1); addView(t2) }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT)
            lp.gravity = Gravity.TOP
            wm.addView(box, lp)
        }
        val (color, word) = when (r.level) { 2 -> 0xFF1B9E3E.toInt() to "BIERZ"; 1 -> 0xFFF08A00.toInt() to "ŚREDNI"; else -> 0xFFD62828.toInt() to "ODRZUĆ" }
        box!!.background = GradientDrawable().apply { setColor(color); cornerRadius = 0f }
        t1!!.text = "$word  %.2f zł/km · %.0f zł/h".format(r.perKm, r.perH)
        t2!!.text = "${o.app.label} · netto %.1f zł · %.1f km (dojazd %.1f) · %.0f min · próg ×%.2f%s"
            .format(r.net, r.km, o.pickKm, r.min, r.mult, r.note)
        box!!.visibility = View.VISIBLE
    }

    override fun onInterrupt() {}
    override fun onDestroy() { box?.let { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) }; super.onDestroy() }
}
