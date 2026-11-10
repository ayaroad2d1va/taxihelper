package pl.taxihelper
import android.content.Context
import java.util.Calendar

enum class App(val label: String) { BOLT("Bolt"), FREENOW("FreeNow"), UBER("Uber") }

data class Offer(val app: App, val price: Double, val pickKm: Double, val pickMin: Double, val tripKm: Double, val tripMin: Double)
data class Result(val level: Int, val net: Double, val perKm: Double, val perH: Double, val km: Double, val min: Double, val mult: Double, val note: String)

object Engine {
    // Wykrywanie aplikacji po nazwie pakietu. Jeśli u Ciebie wygląda inaczej – zobacz Logcat (tag TaxiHelper).
    fun detect(pkg: String): App? {
        val p = pkg.lowercase()
        return when {
            p == "ee.mtakso.driver" || p.contains("bolt") -> App.BOLT
            p == "com.ubercab.driver" || p.contains("ubercab") -> App.UBER
            p.contains("freenow") || p.contains("free_now") || p.contains("mytaxi") || p.contains("hailo") -> App.FREENOW
            else -> null
        }
    }

    // mnożnik progów: w szczycie można wybrzydzać (>1), w martwych godzinach łagodniej (<1)
    fun timeMult(): Double {
        val c = Calendar.getInstance()
        val h = c.get(Calendar.HOUR_OF_DAY); val d = c.get(Calendar.DAY_OF_WEEK)
        val weekday = d in Calendar.MONDAY..Calendar.FRIDAY
        val night = (d == Calendar.FRIDAY || d == Calendar.SATURDAY) && h >= 21 || (d == Calendar.SATURDAY || d == Calendar.SUNDAY) && h < 3
        return when {
            night -> 1.10
            weekday && (h in 6..8 || h in 15..18) -> 1.10
            weekday && h in 10..13 -> 0.90
            h in 3..5 -> 0.90
            else -> 1.0
        }
    }

    // im dłużej stoisz bez kursu, tym łagodniejsze progi
    fun waitMult(waitMin: Long) = when { waitMin >= 30 -> 0.80; waitMin >= 15 -> 0.90; else -> 1.0 }

    fun evaluate(c: Context, o: Offer, waitMin: Long): Result {
        val net = when (o.app) {
            App.BOLT -> if (Prefs.boltNet(c)) o.price else o.price * (1 - Prefs.f(c, "bolt") / 100.0)
            App.FREENOW -> o.price * (1 - Prefs.f(c, "fn") / 100.0)
            App.UBER -> o.price * (1 - Prefs.f(c, "uber") / 100.0)
        }
        val km = (o.pickKm + o.tripKm).coerceAtLeast(0.1)
        var min = o.pickMin + o.tripMin
        if (min <= 0) min = km / 30.0 * 60.0   // brak czasu na ekranie: zakładam 30 km/h
        val eff = net - Prefs.f(c, "cost") * km
        val perKm = eff / km
        val perH = eff / (min / 60.0)
        val m = timeMult() * waitMult(waitMin)
        var lvl = when {
            perKm >= Prefs.f(c, "goodKm") * m && perH >= Prefs.f(c, "goodH") * m -> 2
            perKm < Prefs.f(c, "okKm") * m || perH < Prefs.f(c, "okH") * m -> 0
            else -> 1
        }
        var note = ""
        if (o.pickKm > Prefs.f(c, "maxPick") && lvl > 0) { lvl--; note = " · daleki dojazd" }
        return Result(lvl, net, perKm, perH, km, min, m, note)
    }
}
