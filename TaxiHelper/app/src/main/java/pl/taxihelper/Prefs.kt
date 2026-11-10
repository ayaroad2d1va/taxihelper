package pl.taxihelper
import android.content.Context

object Prefs {
    // klucz -> (opis, wartość domyślna)
    val FIELDS = linkedMapOf(
        "fn" to Pair("Prowizja FreeNow %", 27.06f),
        "uber" to Pair("Prowizja Uber % (0 = kwota w apce jest już dla Ciebie)", 0f),
        "bolt" to Pair("Prowizja Bolt % (tylko gdy Bolt pokazuje brutto)", 30.75f),
        "cost" to Pair("Koszt auta zł/km (paliwo, serwis, amortyzacja)", 0.30f),
        "goodKm" to Pair("ZIELONY: min zł/km (po kosztach)", 2.0f),
        "okKm" to Pair("POMARAŃCZOWY: min zł/km (poniżej = czerwony)", 1.5f),
        "goodH" to Pair("ZIELONY: min zł/h", 50f),
        "okH" to Pair("POMARAŃCZOWY: min zł/h (poniżej = czerwony)", 35f),
        "maxPick" to Pair("Max dojazd do klienta km (więcej = obniża ocenę)", 5f)
    )
    private fun sp(c: Context) = c.getSharedPreferences("p", 0)
    fun f(c: Context, k: String): Float = sp(c).getFloat(k, FIELDS[k]!!.second)
    fun set(c: Context, k: String, v: Float) = sp(c).edit().putFloat(k, v).apply()
    fun boltNet(c: Context) = sp(c).getBoolean("boltNet", true)
    fun setBoltNet(c: Context, v: Boolean) = sp(c).edit().putBoolean("boltNet", v).apply()
}
