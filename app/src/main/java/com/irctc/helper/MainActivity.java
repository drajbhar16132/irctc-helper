package com.irctc.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.webkit.*;
import android.widget.*;
import org.json.JSONObject;

public class MainActivity extends Activity {
    WebView web;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("irctc", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.loadUrl("https://www.irctc.co.in/nget/train-search");
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        Button p = makeBtn("Passenger", Color.parseColor("#FF6F00"));
        Button f = makeBtn("Fill", Color.parseColor("#2E7D32"));
        p.setOnClickListener(v -> passengerDialog());
        f.setOnClickListener(v -> fill());
        bar.addView(p, new LinearLayout.LayoutParams(0, 150, 1f));
        bar.addView(f, new LinearLayout.LayoutParams(0, 150, 1f));
        root.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    Button makeBtn(String t, int c) {
        Button x = new Button(this);
        x.setText(t); x.setTextColor(Color.WHITE);
        x.setBackgroundColor(c); x.setGravity(Gravity.CENTER);
        return x;
    }

    void passengerDialog() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(40, 20, 40, 0);
        EditText n = new EditText(this); n.setHint("Passenger Name");
        n.setText(sp.getString("name", ""));
        EditText a = new EditText(this); a.setHint("Age");
        a.setInputType(2); a.setText(sp.getString("age", ""));
        Spinner g = new Spinner(this);
        g.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, new String[]{"Male", "Female"}));
        l.addView(n); l.addView(a); l.addView(g);

        new AlertDialog.Builder(this).setTitle("Passenger Details").setView(l)
            .setPositiveButton("Save", (d, w) -> {
                sp.edit().putString("name", n.getText().toString())
                  .putString("age", a.getText().toString())
                  .putString("gender", g.getSelectedItemPosition() == 0 ? "M" : "F")
                  .apply();
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Cancel", null).show();
    }

    void fill() {
        String name = sp.getString("name", "");
        if (name.isEmpty()) {
            Toast.makeText(this, "Pehle Passenger button se details save karein", Toast.LENGTH_LONG).show();
            return;
        }
        String js = "(function(){"
          + "var n=" + JSONObject.quote(name) + ",a=" + JSONObject.quote(sp.getString("age", "")) + ",g=" + JSONObject.quote(sp.getString("gender", "M")) + ";"
          + "function setIn(el,v){if(!el)return;"
          + "Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(el,v);"
          + "el.dispatchEvent(new Event('input',{bubbles:true}));}"
          + "setIn(document.querySelector(\"input[placeholder='Name']\"),n);"
          + "setIn(document.querySelector(\"input[placeholder='Age']\"),a);"
          + "var sel=document.querySelector(\"select[formcontrolname='passengerGender']\");"
          + "if(sel){sel.value=g;sel.dispatchEvent(new Event('change',{bubbles:true}));}"
          + "})();";
        web.evaluateJavascript(js, null);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
