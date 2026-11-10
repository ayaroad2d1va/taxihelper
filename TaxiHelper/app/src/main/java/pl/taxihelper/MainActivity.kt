package pl.taxihelper
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    private val edits = LinkedHashMap<String, EditText>()
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }
        col.addView(TextView(this).apply { text = "TaxiHelper"; textSize = 26f })
        col.addView(TextView(this).apply {
            text = "1) Włącz usługę dostępności 'TaxiHelper'.\n2) HUAWEI: Ustawienia > Bateria > Uruchamianie aplikacji > TaxiHelper > Zarządzaj ręcznie > włącz wszystkie 3 przełączniki (autostart, uruchamianie pośrednie, działanie w tle).\n3) Zablokuj apkę w ekranie ostatnich aplikacji (kłódka), żeby EMUI jej nie zabijał.\nKafelek pojawia się u góry nad aplikacją z ofertą."
        })
        col.addView(Button(this).apply { text = "Otwórz ustawienia dostępności"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
        col.addView(CheckBox(this).apply { text = "Bolt pokazuje kwotę NETTO (nie odejmuj prowizji)"
            isChecked = Prefs.boltNet(context); setOnCheckedChangeListener { _, v -> Prefs.setBoltNet(context, v) } })
        for ((k, v) in Prefs.FIELDS) {
            col.addView(TextView(this).apply { text = v.first })
            val e = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(Prefs.f(context, k).toString()) }
            edits[k] = e; col.addView(e)
        }
        col.addView(Button(this).apply { text = "Zapisz"; setOnClickListener {
            edits.forEach { (k, e) -> e.text.toString().replace(',', '.').toFloatOrNull()?.let { Prefs.set(context, k, it) } }
            Toast.makeText(context, "Zapisano", Toast.LENGTH_SHORT).show() } })
        setContentView(ScrollView(this).apply { addView(col) })
    }
}
