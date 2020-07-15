package com.example.smsanalyzer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.anychart.APIlib;
import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.chart.common.listener.Event;
import com.anychart.chart.common.listener.ListenersInterface;
import com.anychart.charts.Pie;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MessageListener {

    private Context context = this;
    private List<Sms> bankList = new ArrayList<>();
    String FILTER = null;

    private TextView total_expenses;
    private TextView total_income;
    private TextView total_transactions;

    private String BALANCE = "0.0";
    // report TextViews
    private TextView bankReport;

    DatabaseHandler databaseHandler = new DatabaseHandler(context);

    private static final int READ_SMS_PERMISSIONS_REQUEST = 1;
    private String income_check = "0.0", expense_check = "0.0";
    private AnyChartView any_chart_view;
    private Parcelable state_check;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // To add a custom action bar
        Toolbar toolbar = findViewById(R.id.tool_bar);
        toolbar.setTitle("Your Expenditure");
        toolbar.setBackgroundColor(R.color.colorAccent);
        setSupportActionBar(toolbar);

        MessageReceiver.bindListener(this);

        try {
            // object read is in the form of list<Sms> so iterate over the list to extract all Sms objects.
            bankList.addAll(databaseHandler.getAllSms("bankTransactions"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        any_chart_view = findViewById(R.id.any_chart_view);
        APIlib.getInstance().setActiveAnyChartView(any_chart_view);

        CardView bankCard = findViewById(R.id.bankCard);
        bankCard.setOnClickListener(this);

        total_expenses = findViewById(R.id.total_expenses);
        total_income = findViewById(R.id.total_income);
        total_transactions = findViewById(R.id.total_transactions);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            getPermissionToReadSMS();
        } else {
            readMessages();
        }

    }

    public void getPermissionToReadSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_SMS)) {
                    Toast.makeText(this, "Please allow permission for the app to work!", Toast.LENGTH_SHORT).show();
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_SMS},
                        READ_SMS_PERMISSIONS_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == READ_SMS_PERMISSIONS_REQUEST) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, "Read SMS permission granted", Toast.LENGTH_SHORT).show();
                readMessages();
            } else {
                Toast.makeText(this, "Read SMS permission denied", Toast.LENGTH_SHORT).show();
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bankCard) {
            if (bankList.size() > 0) {
                Intent bank = new Intent(MainActivity.this, Transactions.class);
                Bundle b = new Bundle();
                b.putSerializable("SMS", (Serializable) bankList);
                bank.putExtra("DATA", b);
                startActivity(bank);
            } else {
                Toast.makeText(MainActivity.this, "No Bank Transactions to display", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void readMessages() {

        // new sms object declared
        Sms sms;

        // read sms are stored in cursor
        Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[]{"date", "body"}, FILTER, null, null);
        int total = 0;
        if (c != null) {
            total = c.getCount();
        }

        // all messages are read from bottom because when new sms gets inserted they are inserted in the position zero
        // thus to keep the latest messages up in the list
        if (c != null) {
            if (c.moveToLast()) {
                for (int i = 0; i < total; i++) {
                    sms = new Sms();

                    // body and date read from cursor
                    String date = c.getString(c.getColumnIndexOrThrow("date"));
                    String body = c.getString(c.getColumnIndexOrThrow("body"));
                    // keeping track of a filter to prevent reading of messages already read
                    FILTER = "date>" + date;

                    String t = "";

                    // date is set to the sms object
                    sms.setMsgDate(date);

                    body = body.toLowerCase();

                    if (!Pattern.compile("(recharge|paytm|ola)").matcher(body).find()) {
                        if (Pattern.compile("(debit|transaction|withdrawn)").matcher(body).find())
                            t = "Debit";
                        else if (Pattern.compile("(credit|deposited)").matcher(body).find())
                            t = "Credit";
                    }

                    // switched according to the type to extract information from the message
                    switch (t) {
                        case "Debit":
                            sms.setMsgType(t);
                            String a = getAmount(body);
                            // getAmount is a method which gives the amount using pattern and matcher
                            if (a != null) {
                                sms.setMsgAmt(a);
                                sms.setMsgBal(Double.toString(Double.parseDouble(BALANCE) - Double.parseDouble(a)));
                                BALANCE = sms.getMsgBal();
                                expense_check = Double.toString(Double.parseDouble(expense_check) + Double.parseDouble(a));
                                bankList.add(0, sms);
                                databaseHandler.addBankSms(sms);
                            } else {
                                c.moveToPrevious();
                                continue;
                            }
                            break;

                        // for type of transaction income first the amount is extracted and then the balance is extracted
                        case "Credit":
                            sms.setMsgType(t);
                            String a1 = getAmount(body);
                            if (a1 != null) {
                                sms.setMsgAmt(a1);
                                sms.setMsgBal(Double.toString(Double.parseDouble(BALANCE) + Double.parseDouble(a1)));
                                BALANCE = sms.getMsgBal();
                                income_check = Double.toString(Double.parseDouble(income_check) + Double.parseDouble(a1));
                                bankList.add(0, sms);
                                databaseHandler.addBankSms(sms);
                            } else {
                                c.moveToPrevious();
                                continue;
                            }
                            break;
                    }
                    c.moveToPrevious();
                }
            } else {
                // if no messages to read than a toast is displayed
                //Toast.makeText(MainActivity.this, "No sms to read!!", Toast.LENGTH_SHORT).show();
            }
        }
        if (c != null) {
            c.close();
        }
        setBankBalance();
        pie_chart_maker();
    }

    public String getAmount(String data) {
        String pattern1 = "(inr)+[\\s]?+[0-9]*+[\\\\,]*+[0-9]*+[\\\\.][0-9]{2}";
        Pattern regex1 = Pattern.compile(pattern1);
        String pattern2 = "(rs)+[\\\\.][\\s]*+[0-9]*+[\\\\,]*+[0-9]*+[\\\\.][0-9]{2}";
        Pattern regex2 = Pattern.compile(pattern2);

        Matcher matcher1 = regex1.matcher(data);
        Matcher matcher2 = regex2.matcher(data);
        if (matcher1.find()) {
            try {
                String a = (matcher1.group(0));
                if (a != null) {
                    a = a.replace("inr", "");
                    a = a.replace(" ", "");
                    a = a.replace(",", "");
                }
                return a;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (matcher2.find()) {
            try {
                String a = (matcher2.group(0));
                if (a != null) {
                    a = a.replace("rs", "");
                    a = a.replaceFirst(".", "");
                    a = a.replace(" ", "");
                    a = a.replace(",", "");
                }
                return a;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @SuppressLint("SetTextI18n")
    private void setBankBalance() {
        if (bankList.size() > 0) {
            total_expenses.setText("Total Expense: ₹ " + expense_check);
            total_income.setText("Total Income: ₹ " + income_check);
            total_transactions.setText("Total No. of Transactions: " + bankList.size());
        } else {
            total_expenses.setText("Total Expense: ₹ " + "0.0");
            total_income.setText("Total Income: ₹ " + "0.0");
            total_transactions.setText("Total No. of Transactions: 0");
        }
    }

    @Override
    public void messageReceived(String message) {
        //Toast.makeText(this, "New Message Received: " + message, Toast.LENGTH_SHORT).show();
        readMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(state_check!=null)
            any_chart_view.onRestoreInstanceState(state_check);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            getPermissionToReadSMS();
        } else {
            readMessages();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        state_check = any_chart_view.onSaveInstanceState();
}

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void pie_chart_maker()
    {
        Pie pie = AnyChart.pie();
        pie.setOnClickListener(new ListenersInterface.OnClickListener(new String[]{"x", "value"}) {
            @Override
            public void onClick(Event event) {
            }
        });

        List<DataEntry> data = new ArrayList<>();
        data.add(new ValueDataEntry("Total Income", Double.parseDouble(income_check)));
        data.add(new ValueDataEntry("Total Expenses", Double.parseDouble(expense_check)));

        pie.data(data);
        APIlib.getInstance().setActiveAnyChartView(any_chart_view);
        any_chart_view.setZoomEnabled(true);
        any_chart_view.setChart(pie);
    }
}
