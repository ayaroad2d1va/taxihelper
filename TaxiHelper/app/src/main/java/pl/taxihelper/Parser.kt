package pl.taxihelper
import android.view.accessibility.AccessibilityNodeInfo

object Parser {
    private val PRICE = Regex("""(\d{1,4}(?:[.,]\d{1,2})?)\s*(?:zł|PLN)|(?:zł|PLN)\s*(\d{1,4}(?:[.,]\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val KM = Regex("""(\d{1,3}(?:[.,]\d{1,2})?)\s*km""", RegexOption.IGNORE_CASE)
    private val MIN = Regex("""(\d{1,3})\s*min""", RegexOption.IGNORE_CASE)
    private val ACCEPT = listOf("zaakceptuj", "akceptuj", "przyjmij", "accept")
    val RIDE_END = listOf("zakończ kurs", "kurs zakończony", "podsumowanie kursu", "end trip", "trip complete")

    fun texts(n: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int = 0) {
        if (n == null || depth > 40) return
        n.text?.let { out += it.toString() }
        n.contentDescription?.let { out += it.toString() }
        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            texts(c, out, depth + 1)
            c.recycle()   // Android 8-12: oszczędza pamięć na starszym telefonie
        }
    }

    private fun num(s: String) = s.replace(',', '.').toDouble()

    /** Zwraca ofertę tylko jeśli ekran wygląda jak oferta (jest przycisk akceptacji + cena + km). */
    fun parse(app: App, t: List<String>): Offer? {
        val all = t.joinToString(" | ")
        val low = all.lowercase()
        if (ACCEPT.none { low.contains(it) }) return null
        val pm = PRICE.find(all) ?: return null
        val price = num(pm.groupValues[1].ifEmpty { pm.groupValues[2] })
        val kms = KM.findAll(all).map { num(it.groupValues[1]) }.toList()
        if (kms.isEmpty() || price <= 0) return null
        val mins = MIN.findAll(all).map { num(it.groupValues[1]) }.toList()
        // konwencja: pierwszy odcinek = dojazd do klienta, drugi = trasa kursu
        return if (kms.size >= 2)
            Offer(app, price, kms[0], mins.getOrElse(0) { 0.0 }, kms[1], mins.getOrElse(1) { 0.0 })
        else
            Offer(app, price, 0.0, 0.0, kms[0], mins.getOrElse(0) { 0.0 })
    }
}
