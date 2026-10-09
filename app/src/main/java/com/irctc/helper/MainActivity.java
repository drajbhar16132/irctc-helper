package com.irctc.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.webkit.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    WebView web;
    SharedPreferences sp;
    TextView clock;
    Handler handler = new Handler(Looper.getMainLooper());
    SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy  HH:mm:ss", Locale.getDefault());
    Runnable tick;

    static final String HOME_URL = "https://www.irctc.co.in/nget/train-search";
    static final String[] GENDER_LABELS = {"Male", "Female", "Transgender"};
    static final String[] GENDER_CODES = {"M", "F", "T"};
    static final String[] BERTH_LABELS = {"No Preference", "Lower", "Middle", "Upper", "Side Lower", "Side Upper"};
    static final String[] BERTH_CODES = {"", "LB", "MB", "UB", "SL", "SU"};
    static final String[] FOOD_LABELS = {"Default (change mat karo)", "Veg", "Non Veg", "Jain Meal", "Veg (Diabetic)", "Non Veg (Diabetic)", "No Food"};
    static final String[] FOOD_CODES = {"", "V", "N", "J", "F", "G", "D"};
    static final String[] OPT_LABELS = {"Change mat karo", "Haan (ON)", "Nahi (OFF)"};
    static final int MAX_SAVED = 30;
    static final int MAX_FILL = 6;

    static final String FILL_JS = """
(function(){
  var P = __DATA__;
  var O = __OPTS__;
  var tries = 0;
  var done = 0;
  function all(sel){ return document.querySelectorAll(sel); }
  function setVal(el, v){
    if(!el) return false;
    var proto = (el.tagName === 'SELECT') ? HTMLSelectElement.prototype : HTMLInputElement.prototype;
    var d = Object.getOwnPropertyDescriptor(proto, 'value');
    try { el.focus(); } catch(e) {}
    d.set.call(el, v);
    el.dispatchEvent(new Event('input', {bubbles:true}));
    el.dispatchEvent(new Event('change', {bubbles:true}));
    el.dispatchEvent(new Event('blur', {bubbles:true}));
    return true;
  }
  function setSel(el, code, label){
    if(!el) return false;
    var i, o;
    for(i = 0; i < el.options.length; i++){
      o = el.options[i];
      if(o.value === code) return setVal(el, o.value);
    }
    var lb = label.toLowerCase();
    for(i = 0; i < el.options.length; i++){
      o = el.options[i];
      if((o.text || '').trim().toLowerCase().indexOf(lb) === 0) return setVal(el, o.value);
    }
    return false;
  }
  function clickEl(el){
    if(!el) return;
    var host = el.closest ? el.closest('p-checkbox,p-radiobutton') : null;
    var box = host ? host.querySelector('.p-checkbox-box,.p-radiobutton-box') : null;
    (box || el).click();
  }
  function setCheck(sel, want){
    var el = document.querySelector(sel);
    if(!el) return false;
    if(!!el.checked !== want) clickEl(el);
    return true;
  }
  function setInsurance(yes){
    var rs = all("p-radiobutton[formcontrolname='travelInsuranceOpted'] input[type='radio'], input[type='radio'][formcontrolname='travelInsuranceOpted']");
    if(rs.length < 2) return false;
    clickEl(yes ? rs[0] : rs[1]);
    return true;
  }
  function nameEls(){
    return all("[formcontrolname='passengerName'] input, input[formcontrolname='passengerName'], input[placeholder='Name']");
  }
  function hasChild(e){
    for(var j = 0; j < e.children.length; j++){
      if((e.children[j].textContent || '').toLowerCase().indexOf('add passenger') >= 0) return true;
    }
    return false;
  }
  function addBtn(){
    var els = all('a,button,span,div');
    for(var i = 0; i < els.length; i++){
      var t = (els[i].textContent || '').trim().toLowerCase();
      if(t.length < 25 && t.indexOf('add passenger') >= 0 && !hasChild(els[i])) return els[i];
    }
    return null;
  }
  function fillRow(i, p){
    var ns = nameEls();
    var ags = all("input[formcontrolname='passengerAge'], input[placeholder='Age']");
    var gs = all("select[formcontrolname='passengerGender']");
    var bs = all("select[formcontrolname='passengerBerthChoice']");
    var fs = all("select[formcontrolname='passengerFoodChoice']");
    var ok = 0;
    if(setVal(ns[i], p.name)) ok++;
    if(setVal(ags[i], p.age)) ok++;
    setSel(gs[i], p.g, p.gl);
    setSel(bs[i], p.b, p.bl);
    if(p.f) setSel(fs[i], p.f, p.fl);
    return ok;
  }
  function fillOptions(){
    var n = [];
    if(O.mobile && setVal(document.querySelector("input[formcontrolname='mobileNumber'], input[placeholder*='obile']"), O.mobile)) n.push('Mobile');
    if(O.auto > 0 && setCheck("p-checkbox[formcontrolname='autoUpgradationSelected'] input, input[formcontrolname='autoUpgradationSelected'], #autoUpgradation", O.auto === 1)) n.push('Auto upgrade');
    if(O.conf > 0 && setCheck("p-checkbox[formcontrolname='bookOnlyIfCnf'] input, input[formcontrolname='bookOnlyIfCnf'], #confirmberths", O.conf === 1)) n.push('Confirm berth');
    if(O.ins > 0 && setInsurance(O.ins === 1)) n.push('Insurance');
    return n;
  }
  function finish(){
    var msg;
    if(nameEls().length === 0){
      msg = 'Passenger form nahi mila. Pehle train select karke Passenger Details page par aao.';
    } else {
      msg = done + ' / ' + P.length + ' passenger fill hue';
      var notes = fillOptions();
      if(notes.length) msg += ' | ' + notes.join(', ');
    }
    try { Android.report(msg); } catch(e) {}
  }
  function step(i){
    if(i >= P.length){ finish(); return; }
    if(nameEls().length <= i){
      var b = addBtn();
      if(b && tries < 20){
        tries++;
        b.click();
        setTimeout(function(){ step(i); }, 600);
        return;
      }
      finish();
      return;
    }
    if(fillRow(i, P[i]) > 0) done++;
    step(i + 1);
  }
  step(0);
})();
""";

    class Bridge {
        @JavascriptInterface
        public void report(final String m) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, m, Toast.LENGTH_LONG).show());
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("irctc", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        clock = new TextView(this);
        clock.setTextSize(20);
        clock.setTypeface(Typeface.DEFAULT_BOLD);
        clock.setTextColor(Color.WHITE);
        clock.setBackgroundColor(Color.parseColor("#213F99"));
        clock.setGravity(Gravity.CENTER);
        clock.setPadding(10, 16, 10, 16);
        root.addView(clock, new LinearLayout.LayoutParams(-1, -2));

        tick = new Runnable() {
            @Override
            public void run() {
                clock.setText(fmt.format(new Date()));
                handler.postDelayed(this, 500);
            }
        };

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setUseWideViewPort(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith("http://")) {
                    v.loadUrl("https://" + u.substring(7));
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedHttpError(WebView v, WebResourceRequest r, WebResourceResponse e) {
                if (r.isForMainFrame() && e.getStatusCode() == 403) {
                    Toast.makeText(MainActivity.this,
                        "IRCTC ne access block kiya (403). 'Reset' dabake dobara try karo.",
                        Toast.LENGTH_LONG).show();
                }
            }
        });
        web.loadUrl(HOME_URL);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        Button p = makeBtn("Passenger", Color.parseColor("#FF6F00"));
        Button f = makeBtn("Fill", Color.parseColor("#2E7D32"));
        Button r = makeBtn("Reset", Color.parseColor("#1565C0"));
        p.setOnClickListener(v -> passengerDialog());
        f.setOnClickListener(v -> fill());
        r.setOnClickListener(v -> resetWeb());
        bar.addView(p, new LinearLayout.LayoutParams(0, 150, 1f));
        bar.addView(f, new LinearLayout.LayoutParams(0, 150, 1f));
        bar.addView(r, new LinearLayout.LayoutParams(0, 150, 1f));
        root.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    void resetWeb() {
        CookieManager cm = CookieManager.getInstance();
        cm.removeAllCookies(null);
        cm.flush();
        WebStorage.getInstance().deleteAllData();
        web.clearCache(true);
        web.clearHistory();
        web.loadUrl(HOME_URL);
        Toast.makeText(this, "Reset ho gaya. IRCTC me dobara login karo.", Toast.LENGTH_LONG).show();
    }

    Button makeBtn(String t, int c) {
        Button x = new Button(this);
        x.setText(t);
        x.setTextColor(Color.WHITE);
        x.setBackgroundColor(c);
        x.setGravity(Gravity.CENTER);
        return x;
    }

    TextView label(String t) {
        TextView x = new TextView(this);
        x.setText(t);
        x.setPadding(0, 20, 0, 0);
        return x;
    }

    Spinner optSpinner(String key) {
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item, OPT_LABELS));
        s.setSelection(sp.getInt(key, 0));
        return s;
    }

    int indexOf(String[] a, String v) {
        for (int i = 0; i < a.length; i++) {
            if (a[i].equals(v)) return i;
        }
        return -1;
    }

    JSONArray load() {
        try {
            return new JSONArray(sp.getString("list", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    void save(JSONArray a) {
        sp.edit().putString("list", a.toString()).apply();
    }

    void passengerDialog() {
        final JSONArray arr = load();
        final AlertDialog[] dlg = new AlertDialog[1];

        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(30, 20, 30, 0);
        sv.addView(box);

        if (arr.length() == 0) {
            TextView t = new TextView(this);
            t.setText("Koi passenger save nahi hai. '+ Add New' dabao.");
            box.addView(t);
        } else {
            Button un = new Button(this);
            un.setText("Sab untick karo");
            un.setTextSize(12);
            un.setOnClickListener(v -> {
                try {
                    for (int k = 0; k < arr.length(); k++) {
                        arr.getJSONObject(k).put("sel", false);
                    }
                } catch (Exception e) {
                }
                save(arr);
                dlg[0].dismiss();
                passengerDialog();
            });
            box.addView(un);
        }

        for (int i = 0; i < arr.length(); i++) {
            final int idx = i;
            final JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            CheckBox cb = new CheckBox(this);
            int gi = Math.max(0, indexOf(GENDER_CODES, o.optString("g")));
            cb.setText(o.optString("name") + " (" + o.optString("age") + ", " + GENDER_LABELS[gi] + ")");
            cb.setChecked(o.optBoolean("sel", true));
            cb.setOnCheckedChangeListener((v, c) -> {
                try {
                    o.put("sel", c);
                } catch (Exception e) {
                }
                save(arr);
            });

            Button ed = new Button(this);
            ed.setText("Edit");
            ed.setTextSize(12);
            ed.setOnClickListener(v -> {
                dlg[0].dismiss();
                editDialog(idx);
            });

            Button del = new Button(this);
            del.setText("X");
            del.setTextSize(12);
            del.setOnClickListener(v -> {
                arr.remove(idx);
                save(arr);
                dlg[0].dismiss();
                passengerDialog();
            });

            row.addView(cb, new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(ed, new LinearLayout.LayoutParams(-2, -2));
            row.addView(del, new LinearLayout.LayoutParams(-2, -2));
            box.addView(row);
        }

        dlg[0] = new AlertDialog.Builder(this)
            .setTitle("Saved Passengers (" + arr.length() + ")")
            .setView(sv)
            .setPositiveButton("+ Add New", (d, w) -> editDialog(-1))
            .setNeutralButton("Settings", (d, w) -> settingsDialog())
            .setNegativeButton("Close", null)
            .show();
    }

    void editDialog(final int idx) {
        final JSONArray arr = load();
        if (idx < 0 && arr.length() >= MAX_SAVED) {
            Toast.makeText(this, "Maximum " + MAX_SAVED + " passenger save ho sakte hain", Toast.LENGTH_SHORT).show();
            passengerDialog();
            return;
        }
        final JSONObject cur = idx >= 0 ? arr.optJSONObject(idx) : null;

        ScrollView sv = new ScrollView(this);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(40, 20, 40, 0);
        sv.addView(l);

        final EditText n = new EditText(this);
        n.setHint("Passenger Name (max 16)");
        n.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        final EditText a = new EditText(this);
        a.setHint("Age");
        a.setInputType(InputType.TYPE_CLASS_NUMBER);

        final Spinner g = new Spinner(this);
        g.setAdapter(new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item, GENDER_LABELS));
        final Spinner br = new Spinner(this);
        br.setAdapter(new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item, BERTH_LABELS));
        final Spinner fd = new Spinner(this);
        fd.setAdapter(new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_dropdown_item, FOOD_LABELS));

        if (cur != null) {
            n.setText(cur.optString("name"));
            a.setText(cur.optString("age"));
            int gi = indexOf(GENDER_CODES, cur.optString("g"));
            int bi = indexOf(BERTH_CODES, cur.optString("b"));
            int fi = indexOf(FOOD_CODES, cur.optString("f"));
            g.setSelection(gi < 0 ? 0 : gi);
            br.setSelection(bi < 0 ? 0 : bi);
            fd.setSelection(fi < 0 ? 0 : fi);
        }

        l.addView(n);
        l.addView(a);
        l.addView(label("Gender"));
        l.addView(g);
        l.addView(label("Berth Preference"));
        l.addView(br);
        l.addView(label("Food Choice (jahan available ho)"));
        l.addView(fd);

        new AlertDialog.Builder(this)
            .setTitle(idx >= 0 ? "Edit Passenger" : "New Passenger")
            .setView(sv)
            .setPositiveButton("Save", (d, w) -> {
                String name = n.getText().toString().trim();
                String age = a.getText().toString().trim();
                if (name.isEmpty() || age.isEmpty()) {
                    Toast.makeText(this, "Name aur Age zaroori hai", Toast.LENGTH_SHORT).show();
                    passengerDialog();
                    return;
                }
                try {
                    JSONObject o = new JSONObject();
                    o.put("name", name);
                    o.put("age", age);
                    o.put("g", GENDER_CODES[g.getSelectedItemPosition()]);
                    o.put("b", BERTH_CODES[br.getSelectedItemPosition()]);
                    o.put("f", FOOD_CODES[fd.getSelectedItemPosition()]);
                    o.put("sel", cur == null ? true : cur.optBoolean("sel", true));
                    if (idx >= 0) arr.put(idx, o); else arr.put(o);
                    save(arr);
                } catch (Exception e) {
                }
                passengerDialog();
            })
            .setNegativeButton("Cancel", (d, w) -> passengerDialog())
            .show();
    }

    void settingsDialog() {
        ScrollView sv = new ScrollView(this);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(40, 20, 40, 0);
        sv.addView(l);

        final EditText mob = new EditText(this);
        mob.setHint("Mobile Number (10 digit)");
        mob.setInputType(InputType.TYPE_CLASS_PHONE);
        mob.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        mob.setText(sp.getString("mobile", ""));

        final Spinner ins = optSpinner("ins");
        final Spinner auto = optSpinner("auto");
        final Spinner conf = optSpinner("conf");

        l.addView(label("Mobile Number"));
        l.addView(mob);
        l.addView(label("Travel Insurance"));
        l.addView(ins);
        l.addView(label("Auto Upgrade (Consider for auto upgradation)"));
        l.addView(auto);
        l.addView(label("Book only if confirm berths allotted"));
        l.addView(conf);

        new AlertDialog.Builder(this)
            .setTitle("Booking Settings")
            .setView(sv)
            .setPositiveButton("Save", (d, w) -> {
                sp.edit()
                    .putString("mobile", mob.getText().toString().trim())
                    .putInt("ins", ins.getSelectedItemPosition())
                    .putInt("auto", auto.getSelectedItemPosition())
                    .putInt("conf", conf.getSelectedItemPosition())
                    .apply();
                Toast.makeText(this, "Settings save ho gayi", Toast.LENGTH_SHORT).show();
                passengerDialog();
            })
            .setNegativeButton("Cancel", (d, w) -> passengerDialog())
            .show();
    }

    void fill() {
        JSONArray arr = load();
        JSONArray sel = new JSONArray();
        try {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (!o.optBoolean("sel", true)) continue;
                if (sel.length() >= MAX_FILL) {
                    Toast.makeText(this, "6 se zyada tick hain, sirf pehle 6 fill honge", Toast.LENGTH_LONG).show();
                    break;
                }
                int gi = Math.max(0, indexOf(GENDER_CODES, o.optString("g")));
                int bi = Math.max(0, indexOf(BERTH_CODES, o.optString("b")));
                int fi = Math.max(0, indexOf(FOOD_CODES, o.optString("f")));
                JSONObject p = new JSONObject();
                p.put("name", o.optString("name"));
                p.put("age", o.optString("age"));
                p.put("g", GENDER_CODES[gi]);
                p.put("gl", GENDER_LABELS[gi]);
                p.put("b", BERTH_CODES[bi]);
                p.put("bl", BERTH_LABELS[bi]);
                p.put("f", FOOD_CODES[fi]);
                p.put("fl", FOOD_LABELS[fi]);
                sel.put(p);
            }
        } catch (Exception e) {
        }
        if (sel.length() == 0) {
            Toast.makeText(this, "Pehle Passenger button se passenger save aur tick karein", Toast.LENGTH_LONG).show();
            return;
        }
        JSONObject opts = new JSONObject();
        try {
            opts.put("mobile", sp.getString("mobile", ""));
            opts.put("ins", sp.getInt("ins", 0));
            opts.put("auto", sp.getInt("auto", 0));
            opts.put("conf", sp.getInt("conf", 0));
        } catch (Exception e) {
        }
        String js = FILL_JS.replace("__DATA__", sel.toString()).replace("__OPTS__", opts.toString());
        web.evaluateJavascript(js, null);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
