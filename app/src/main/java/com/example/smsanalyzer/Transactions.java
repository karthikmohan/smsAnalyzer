package com.example.smsanalyzer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Transactions extends AppCompatActivity {

    private Context context = this;
    List<Sms> transactions = new ArrayList<>();
    RecyclerView transList;
    DatabaseHandler databaseHandler = new DatabaseHandler(context);
    private MyAdapter myAdapter;
    private EditText editText;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transactions);

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("DATA");
        transactions = (ArrayList<Sms>) bundle.getSerializable("SMS");

        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(R.color.colorAccent);

        transList = findViewById(R.id.transList);
        transList.setHasFixedSize(true);
        transList.setLayoutManager(new LinearLayoutManager(this));

        myAdapter = new MyAdapter(transactions, context);
        transList.setAdapter(myAdapter);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        editText = (EditText) findViewById(R.id.editText);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.bank_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.search:
                editText.setVisibility(View.VISIBLE);
                update_trans();
                break;
            case R.id.forward:
                List<Sms> smsList1 = new ArrayList<>();
                for (Sms s : transactions) {
                    if (s.getDrOrCr().equals("DR")) {
                        smsList1.add(s);
                    }
                }
                // when forward action button is clicked a bar chart is displayed whose values are calculated here
                if (smsList1.size() > 0) {
                    Intent i = new Intent(Transactions.this, report.class);
                    Bundle b = new Bundle();
                    b.putSerializable("SMS", (Serializable) smsList1);
                    // color is sent to the report activity depending on click of bank or cash card
                    b.putString("color", String.valueOf(R.color.colorAccent));
                    i.putExtra("DATA", b);
                    startActivity(i);
                } else {
                    // if no messages are there then a toast is displayed
                    Toast.makeText(Transactions.this, "You have not spent money", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void update_trans() {

    }


    @Override
    public void onBackPressed() {
        if (editText.getVisibility()==View.VISIBLE) {
            editText.setVisibility(View.GONE);
        } else {
            finish();
        }
    }
}
