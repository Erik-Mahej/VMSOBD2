package com.example.vmsobd2;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.graphics.Color;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

public class EngineFaults extends AppCompatActivity {

    private GridLayout gridLayout;
    private int cardCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_engine_faults);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });
        gridLayout = findViewById(R.id.gridLayout);
    }

    public void goBack(View view) {
        if (view.getId() == R.id.btnBack) {
            Intent intentMain = new Intent(EngineFaults.this, MainActivity.class);
            startActivity(intentMain);
        }
        finish();
    }
    public void onClick(View v) {
        addCardView("Card " + (++cardCount));
    }

    private void addCardView(String text) {
        CardView cardView = new CardView(this);
        GridLayout.LayoutParams cardLayoutParams = new GridLayout.LayoutParams();
        cardLayoutParams.setMargins(15, 15, 15, 15);
        cardLayoutParams.width = GridLayout.LayoutParams.MATCH_PARENT;
        cardLayoutParams.height = GridLayout.LayoutParams.MATCH_PARENT;
        cardLayoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardLayoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardView.setLayoutParams(cardLayoutParams);
        cardView.setCardBackgroundColor(Color.parseColor("#f3f3f3"));
        cardView.setCardElevation(4);
        cardView.setRadius(15);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        innerLayout.setGravity(Gravity.CENTER);
        innerLayout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(Color.parseColor("#0D0D0D")); //barva textu
        textView.setTextSize(18); //velikost textu
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setPadding(0, 10, 0, 10); //margin nahore

        innerLayout.addView(textView);

        cardView.addView(innerLayout);

        gridLayout.addView(cardView);
    }

}