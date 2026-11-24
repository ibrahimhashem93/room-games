# 🎲 لودو — Vexolabs

لعبة لودو كاملة بواجهة عربية (RTL)، مبنية بـ HTML/CSS/JS خالص بدون أي مكتبات خارجية.
تعمل **دون اتصال فورًا** من ملف واحد، وتصلح للتضمين داخل `WebView` في تطبيق أندرويد.

## الملفات

| الملف | الوصف |
|---|---|
| `ludo.html` | **النسخة الجاهزة — ملف واحد مكتفٍ بذاته** (CSS + JS مدمجان). هذا ما تضعه في التطبيق. |
| `src/index.html` + `src/style.css` + `src/app.js` + `src/ludo-engine.js` | المصدر مقسمًا للتطوير. |
| `build.js` | يعيد بناء `ludo.html` من المصدر: `node build.js` |
| `test/engine.test.js` | 26 اختبارًا لقواعد اللعبة: `node test/engine.test.js` |

## القواعد المنفذة

- 2–4 لاعبين، 4 قطع لكل لاعب (لاعبان يتقابلان: أحمر × أصفر).
- الخروج من القاعدة يتطلب **6**.
- رمية **6** = دور إضافي. **ثلاث ستّات متتالية = إلغاء الدور**.
- الأسر يرجع قطعة الخصم إلى قاعدتها، ويمنح دورًا إضافيًا.
- المربعات الآمنة (★ + مربعات البداية) تمنع الأسر.
- دخول عمود النهاية يتطلب رمية **بالضبط**، وإيصال قطعة للنهاية يمنح دورًا إضافيًا.
- كشف الفائز والترتيب (الأول، الثاني، …) مع شاشة نتائج.
- لعب محلي Pass-and-Play على نفس الجهاز، بلا اتصال وبدون أي تحميل خارجي.

## التضمين في أندرويد (WebView)

انسخ `ludo.html` إلى `app/src/main/assets/ludo.html` ثم:

### Kotlin

```kotlin
class LudoActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this)
        setContentView(web)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        web.loadUrl("file:///android_asset/ludo.html")
    }
}
```

### Java

```java
WebView web = new WebView(this);
setContentView(web);
WebSettings s = web.getSettings();
s.setJavaScriptEnabled(true);
s.setDomStorageEnabled(true);
web.loadUrl("file:///android_asset/ludo.html");
```

لا حاجة لأي أذونات إنترنت — كل شيء محلي.

## جسر المزامنة (Sync Bridge)

اللعبة جاهزة للعب الشبكي لاحقًا عبر `window.LudoBridge` **بدون لمس منطق اللعبة**:

```js
// 1) أنت تستبدل هذه الدالة: تُستدعى مع كل حركة محلية
LudoBridge.sendMove = function (move) {
  // أرسل move إلى خادمك (WebSocket / Firebase / غيرها)
  myBackend.send(JSON.stringify(move));
};

// 2) عند وصول حركة من لاعب آخر، نادِ:
//    myBackend.onMessage(data => LudoBridge.applyRemoteMove(JSON.parse(data)));

// 3) حدد المقاعد التي يتحكم بها هذا الجهاز (مثلاً المقعد 0 فقط):
LudoBridge.localSeats = [0];
```

### أشكال الرسائل (move)

```js
{ kind: 'roll',    player: 0, value: 6 }        // نتيجة النرد لمقعد معيّن
{ kind: 'move',    player: 0, token: 2 }        // القطعة المختارة (0..3)
{ kind: 'sync',    snapshot: {...} }            // إعادة مزامنة كاملة للحالة
{ kind: 'newGame', players: 4, names: [...] }   // بدء لعبة جديدة
```

- `value` في `roll` تأتي من الخادم/المرسل — اللعبة لا تولّدها محليًا في الوضع الشبكي، فيبقى الرمي موثوقًا.
- `LudoBridge.getState()` يعيد نسخة JSON من الحالة الحالية (مفيد للتصحيح أو استعادة جلسة).
- الحركات لمقعد ليس دوره تُتجاهل مع تحذير في console.

### مثال ربط من كوتلن

```kotlin
// استقبال الحركات من JS داخل WebView:
class Bridge(val onMove: (String) -> Unit) {
    @JavascriptInterface fun sendMove(json: String) = onMove(json)
}
web.addJavascriptInterface(Bridge { json -> myBackend.send(json) }, "AndroidBridge")
web.evaluateJavascript(
    "LudoBridge.sendMove = m => AndroidBridge.sendMove(JSON.stringify(m))", null)

// إرسال حركة واردة إلى اللعبة:
fun onRemoteMove(json: String) {
    web.evaluateJavascript("LudoBridge.applyRemoteMove($json)", null)
}
```

## التخصيص

- الألوان والثيم: متغيرات `:root` أعلى `style.css`.
- أسماء اللاعبين الافتراضية: `COLOR_NAMES_AR` في `ludo-engine.js`.
- منطق اللعبة معزول تمامًا في `ludo-engine.js` (يعمل في Node للاختبارات).

---
صُنع لـ **Vexolabs** 🎲
