package com.example.admin.gpxbymodule;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Bookinglist extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    HomeDatabase helper;
    GenDatabase generaldb;
    RatesDB rate;
    String value;
    ListView lv;
    SQLiteDatabase db;
    ProgressDialog progressBar;
    String link;
    NavigationView navigationView;
    LinearList adapter;
    AutoCompleteTextView search;
    ArrayList<String> bookids;
    private int SETTINGS_ACTION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        preference();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookinglist);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        helper = new HomeDatabase(this);
        generaldb = new GenDatabase(this);
        rate = new RatesDB(this);
        lv = (ListView)findViewById(R.id.lv);
        search = (AutoCompleteTextView)findViewById(R.id.searchableinput);
        search.setSelected(false);
        link = helper.getUrl();
        bookids = new ArrayList<>();

        customtype();
        try {
            search.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    customtype();
                    Log.e("text watch", "before change");
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.getFilter().filter(s.toString());
                    Log.e("text watch", "on change");
                }

                @Override
                public void afterTextChanged(Editable s) {
                    Log.e("text watch", "after change");
                }
            });

            scroll();
            if (helper.logcount() != 0){
                value = helper.getRole(helper.logcount());
                Log.e("role ", value);
            }

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.addbooking);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Intent i = new Intent(getApplicationContext(), Booking.class);
                        Bundle bundle = new Bundle();
                        //Add your data from getFactualResults method to bundle
                        bundle.putString("transno", null);
                        bundle.putString("fullname", null);
                        //Add the bundle to the intent
                        i.putExtras(bundle);
                        startActivity(i);
                        finish();
                    }catch (Exception e){}
                }
            });

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();

            navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            setNameMail();
            sidenavMain();
            subMenu();

        }catch (Exception e){
            Log.e("error",e.getMessage());
        }


    }

    public void setNameMail(){
        RatesDB rate = new RatesDB(getApplicationContext());
        String branchname = null;
        View header = navigationView.getHeaderView(0);
        TextView user = (TextView)header.findViewById(R.id.yourname);
        TextView mail = (TextView)header.findViewById(R.id.yourmail);
        user.setText(helper.getFullname(helper.logcount()+""));
        SQLiteDatabase db = rate.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+rate.tbname_branch
                +" WHERE "+rate.branch_id+" = '"+helper.getBranch(""+helper.logcount())+"'", null);
        if (x.moveToNext()){
            branchname = x.getString(x.getColumnIndex(rate.branch_name));
        }
        x.close();
        mail.setText(helper.getRole(helper.logcount())+" / "+branchname);

    }

    public void scroll(){
        try {
            lv.setOnTouchListener(new View.OnTouchListener() {
                // Setting on Touch Listener for handling the touch inside ScrollView
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Disallow the touch request for parent scroll on touch of child view
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return false;
                }
            });
        }catch (Exception e){}
    }

    public void customtype(){
        try {
            final ArrayList<LinearItem> result = generaldb.getAllBooking(helper.logcount() + "");
            adapter = new LinearList(getApplicationContext(), result);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    extendViewing(view);
                }
            });

            lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view,
                                               final int position, long id) {
                    final String idval = result.get(position).getId();
                    final String booktrans = result.get(position).getTopitem();
                    final AlertDialog.Builder builder = new AlertDialog.Builder(Bookinglist.this);

                    if (!getUpds(booktrans)){
                        builder.setTitle("Edit this booking?");
                        builder.setMessage(Html.fromHtml("<b>note:</b>" +
                            "<i> Update this data how ever you want to. </i>"));
                        builder.setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                    final String booktrans = result.get(position).getTopitem();
                                final String full = result.get(position).getSubitem();
                                //Create the bundle
                                Intent i = new Intent(getApplicationContext(), Booking.class);
                                Bundle bundle = new Bundle();
                                bundle.putString("transno", booktrans);
                                bundle.putString("fullname", full);
                                bundle.putString("reservenum", null);
                                //Add the bundle to the intent
                                i.putExtras(bundle);
                                startActivity(i);
                                finish();
                            }
                        });
                    }else{
                        builder.setTitle("Booking Info");
                        builder.setMessage(Html.fromHtml("<b>note:</b>" +
                                "<i> You cannot edit this booking because it has been uploaded, thank you. </i>"));
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                    }
                    builder.create().show();

                    return true;
                }
            });
        }catch (Exception e){}
    }

    public void sidenavMain(){
        ListView lv=(ListView)findViewById(R.id.optionsList);//initialization of listview

        final ArrayList<HomeList> listitem = helper.getData(value);

        NavAdapter ad = new NavAdapter(getApplicationContext(), listitem);
        lv.setAdapter(ad);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selected = listitem.get(position).getSubitem();

                select(selected);

            }
        });
    }

    public void subMenu(){
        ListView lv=(ListView)findViewById(R.id.submenu);//initialization of listview

        final ArrayList<HomeList> listitem = helper.getSubmenu();

        NavAdapter ad = new NavAdapter(getApplicationContext(), listitem);
        lv.setAdapter(ad);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String sel = listitem.get(position).getSubitem();
                switch (sel){
                    case "Log Out":
                        final AlertDialog.Builder builder = new AlertDialog.Builder(Bookinglist.this);
                        builder.setMessage("Confirm logout?")
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        helper.logout();

                                        startActivity(new Intent(getApplicationContext(), Login.class));
                                        finish();
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                    }
                                });
                        // Create the AlertDialog object and show it
                        builder.create().show();
                        break;
                    case "Home":
                        startActivity(new Intent(getApplicationContext(), Home.class));
                        finish();
                        break;
                    case "Add Customer":
                        Intent mIntent = new Intent(getApplicationContext(), AddCustomer.class);
                        mIntent.putExtra("key", "");
                        startActivity(mIntent);
                        finish();
                        break;
                }
            }
        });
    }

    public void select(String data){
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        switch (data){
            case "Acceptance":
                if (value.equals("OIC")){
                    startActivity(new Intent(this, Acceptance.class));
                    finish();
                }
                else if(value.equals("Warehouse Checker")){
                    startActivity(new Intent(this, Acceptance.class));
                    finish();
                }
                else if(value.equals("Partner Portal")){
                    startActivity(new Intent(this, Partner_acceptance.class));
                    finish();
                }
                break;
            case "Distribution":
                if (value.equals("OIC")){
                    startActivity(new Intent(this, Distribution.class));
                    finish();
                }else if (value.equals("Warehouse Checker")){
                    startActivity(new Intent(this, Distribution.class));
                    finish();
                }else if (value.equals("Partner Portal")){
                    startActivity(new Intent(this, Partner_distribution.class));
                    finish();
                }
                break;
            case "Remittance":
                if (value.equals("OIC")){
                    startActivity(new Intent(this, Remitt.class));
                    finish();
                }else if (value.equals("Sales Driver")){
                    startActivity(new Intent(this, Remitt.class));
                    finish();
                }
                break;
            case "Incident Report":
                    Intent i = new Intent(this, Incident.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("module", "Booking");
                    //Add the bundle to the intent
                    i.putExtras(bundle);
                    startActivity(i);
                    finish();
                break;
            case "Transactions":
                if (value.equals("OIC")){
                    startActivity(new Intent(this, Oic_Transactions.class));
                    finish();
                }
                else if (value.equals("Sales Driver")){
                    startActivity(new Intent(this, Driver_Transactions.class));
                    finish();
                }else if (value.equals("Warehouse Checker")){
                    startActivity(new Intent(this, Checker_transactions.class));
                    finish();
                }
                break;
            case "Inventory":
                if (value.equals("OIC")){
                    startActivity(new Intent(this, Oic_inventory.class));
                    finish();
                }
                else if (value.equals("Sales Driver")){
                    startActivity(new Intent(this, Driver_Inventory.class));
                    finish();
                }
                else if(value.equals("Warehouse Checker")){
                    startActivity(new Intent(this, Checker_Inventory.class));
                    finish();
                }
                else if(value.equals("Partner Portal")){
                    startActivity(new Intent(this, Partner_inventory.class));
                    finish();
                }
                break;
            case "Loading/Unloading":
                if(value.equals("Warehouse Checker")){
                    startActivity(new Intent(this, Load_home.class));
                    finish();
                }
                else if (value.equals("Partner Portal")){
                    startActivity(new Intent(this, Load_home.class));
                    finish();
                }
                break;
            case "Reservation":
                startActivity(new Intent(this, Reservelist.class));
                finish();
                break;
            case "Booking":
                drawer.closeDrawer(Gravity.START);
                break;
            case "Direct":
                startActivity(new Intent(this, Partner_Maindelivery.class));
                finish();
                break;
            case "Barcode Releasing":
                startActivity(new Intent(this, BoxRelease.class));
                finish();
                break;
        }
    }

    public void extendViewing(View v){
        try {
            TextView textView = (TextView) v.findViewById(R.id.c_account);
            final TextView kanino = (TextView) v.findViewById(R.id.c_name);
            final String mtop = textView.getText().toString();
            final String hwo = kanino.getText().toString();
            Log.e("topitem", mtop);
            ArrayList<ListItem> poparray;
            final Dialog d = new Dialog(Bookinglist.this);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setCancelable(false);
            d.setContentView(R.layout.reservationdata);

            final TextView reservationnum = (TextView) d.findViewById(R.id.reservationnumber);
            TextView owner = (TextView) d.findViewById(R.id.owner);
            owner.setText("Sender");
            TextView whom = (TextView) d.findViewById(R.id.ownerinfo);

            TextView ownaddress = (TextView) d.findViewById(R.id.own_addresstitle);
            ownaddress.setText("Payment Amount");
            TextView address = (TextView) d.findViewById(R.id.own_addressinput);
            TextView stat = (TextView) d.findViewById(R.id.stat);
            TextView restat = (TextView) d.findViewById(R.id.reserve_stat);
            restat.setVisibility(View.INVISIBLE);
            stat.setText("Box status");

            ListView poplist = (ListView) d.findViewById(R.id.list);
            poparray = generaldb.getAllBoxInTransaNo(mtop);

            TableAdapter tb = new TableAdapter(getApplicationContext(), poparray);
            poplist.setAdapter(tb);

            reservationnum.setText(mtop);
            whom.setText(Html.fromHtml("" + hwo + ""));
            if (getPayment(mtop) == null){
                address.setText(Html.fromHtml("Data has been sent to Central"));
            }else {
                address.setText(Html.fromHtml("" + getPayment(mtop) + "0"));
            }

            Button ok = (Button) d.findViewById(R.id.addboxnum);
            ok.setText("Edit");
            Button close = (Button) d.findViewById(R.id.close);
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    d.dismiss();
                }
            });
            if (getUpds(mtop)){
                ok.setClickable(false);
                ok.setBackgroundColor(Color.GRAY);
                ok.setTextColor(Color.LTGRAY);
            }else{
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String booktrans = mtop;
                        final String full = hwo;
                        //Create the bundle
                        Intent i = new Intent(getApplicationContext(), Booking.class);
                        Bundle bundle = new Bundle();
                        //Add your data from getFactualResults method to bundle
                        bundle.putString("transno", booktrans);
                        bundle.putString("fullname", full);
                        bundle.putString("reservenum", null);
                        //Add the bundle to the intent
                        i.putExtras(bundle);
                        d.dismiss();
                        startActivity(i);
                        finish();
                    }
                });
            }
            d.show();
        }catch (Exception e){}
    }

    public String getPayment(String id){
        String pay = null;
        SQLiteDatabase db = generaldb.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+generaldb.tbname_payment
        +" WHERE "+generaldb.pay_booking_id+" = '"+id+"'", null);
        if (x.moveToNext()){
            pay = x.getString(x.getColumnIndex(generaldb.pay_total_amount));
        }
        return pay;
    }

    //delete booking consignee
    public boolean deleteBookingConsignee(String trans) {
        SQLiteDatabase db = generaldb.getWritableDatabase();
        db.delete(generaldb.tbname_booking_consignee_box,
                generaldb.book_con_transaction_no + " = '" + trans+"'", null);
        db.close();

        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
                startActivity(new Intent(getApplicationContext(), Home.class));
                finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bookinglist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.syncbooking) {
            network();
        }
        else if (id ==  R.id.passincident){
            Intent i = new Intent(this, Incident.class);
            Bundle bundle = new Bundle();
            bundle.putString("module", "Booking");
            //Add the bundle to the intent
            i.putExtras(bundle);
            startActivity(i);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //syncing data reservations
    //connecting to internet
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void network(){
        if (isNetworkAvailable()== true){
            sendPost();
            loadingPost(getWindow().getDecorView().getRootView());
        }else
        {
            Toast.makeText(getApplicationContext(),"Please check internet connection.", Toast.LENGTH_LONG).show();
        }
    }

    public boolean getUpds(String x){
        boolean ok = false;
        SQLiteDatabase db = generaldb.getReadableDatabase();
        String c = "SELECT * FROM "+generaldb.tbname_booking
                +" WHERE "+generaldb.book_transaction_no+" = '"+x+"'";
        Cursor d = db.rawQuery(c, null);
        if (d.moveToNext()){
            String stat = d.getString(d.getColumnIndex(generaldb.book_upds));
            Log.e("status", stat);
            if (stat.equals("1")){
                ok = false;
            }else{
                ok = true;
            }
        }
        return ok;
    }

    public void loadingPost(final View v){
        // prepare for a progress bar dialog
        int max = 100;
        progressBar = new ProgressDialog(v.getContext());
        progressBar.setCancelable(false);
        progressBar.setMessage("In Progress ...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setMax(max);
        for (int i = 0; i <= max; i++) {
            progressBar.setProgress(i);
            if (i == max ){
                progressBar.dismiss();
            }
            progressBar.show();
        }
    }

    public void threadBooking(){
        SQLiteDatabase db = generaldb.getReadableDatabase();
        String q = " SELECT * FROM " + generaldb.tbname_booking
                +" WHERE "+generaldb.tbname_booking+"."+generaldb.book_upds+" = '1'";
        Cursor cx = db.rawQuery(q, null);
        if (cx.getCount() != 0) {
            try {
                String link = helper.getUrl();
                URL url = new URL("http://" + link + "/api/booking/save.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                JSONObject jsonParam = new JSONObject();
                db = generaldb.getReadableDatabase();
                JSONArray finalarray = new JSONArray();
                JSONArray pay = null, reserve = null, boxtypes = null, disc = null;

                String query = " SELECT * FROM " + generaldb.tbname_booking
                        +" LEFT JOIN "+generaldb.tbname_payment+" ON "
                        +generaldb.tbname_booking+"."+generaldb.book_transaction_no
                        +" = "+generaldb.tbname_payment+"."+generaldb.pay_booking_id
                        +" WHERE "+generaldb.tbname_payment+"."+generaldb.pay_booking_id
                        +" != '' AND "+generaldb.tbname_booking+"."+generaldb.book_upds+" = '1'";
                Cursor c = db.rawQuery(query, null);
                c.moveToFirst();

                while (!c.isAfterLast()) {
                    JSONObject json = new JSONObject();
                    String id = c.getString(c.getColumnIndex(generaldb.book_id));
                    String trans = c.getString(c.getColumnIndex(generaldb.book_transaction_no));
                    String resnum = c.getString(c.getColumnIndex(generaldb.book_reservation_no));
                    String customer = c.getString(c.getColumnIndex(generaldb.book_customer));
                    String bookdate = c.getString(c.getColumnIndex(generaldb.book_book_date));
                    String sched = c.getString(c.getColumnIndex(generaldb.book_schedule_date));
                    String asto = c.getString(c.getColumnIndex(generaldb.book_assigned_to));
                    String bookstat = c.getString(c.getColumnIndex(generaldb.book_booking_status));
                    String booktype = c.getString(c.getColumnIndex(generaldb.book_type));
                    String crby = c.getString(c.getColumnIndex(generaldb.book_createdby));

                    json.put("id", id);
                    json.put("transaction_no", trans);
                    json.put("reservation_no", resnum);
                    json.put("customer", customer);
                    json.put("booking_date", bookdate);
                    json.put("schedule_date", sched);
                    json.put("assigned_to", asto);
                    json.put("booking_stat", bookstat);
                    json.put("booking_type", booktype);
                    json.put("created_by", crby);
                    boxtypes = getAllBookingConsignee(trans);
                    reserve = getBookingPayment(trans);
                    disc = getBookingDiscount(trans);

                    json.put("booking_box", boxtypes);
                    json.put("payment", reserve);
                    json.put("discounts", disc);
                    json.put("customer_information", getBookingCustomer(customer));
                    json.put("booking_image", getBookingImage(trans));
                    bookids.add(trans);
                    finalarray.put(json);

                    c.moveToNext();
                }
                c.close();
                jsonParam.accumulate("data", finalarray);
                Log.e("JSON", jsonParam.toString());
                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsonParam.toString());
                os.flush();
                os.close();

                Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                Log.i("MSG", conn.getResponseMessage());

                if (!conn.getResponseMessage().equals("OK")){
                    conn.disconnect();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.dismiss();
                            final AlertDialog.Builder builder = new AlertDialog.Builder(Bookinglist.this);
                            builder.setTitle("Upload failed")
                                    .setMessage("Data sync has failed, please try again later. thank you.")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                            recreate();
                                        }
                                    });
                            // Create the AlertDialog object and show it
                            builder.create();
                            builder.setCancelable(false);
                            builder.show();
                        }
                    });
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.dismiss();
                            final AlertDialog.Builder builder = new AlertDialog.Builder(Bookinglist.this);
                            builder.setTitle("Information confirmation")
                                    .setMessage("Data upload has been successful, thank you.")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            updateBookingStat(bookids);
                                            dialog.dismiss();
                                            customtype();
                                        }
                                    });
                            // Create the AlertDialog object and show it
                            builder.create();
                            builder.setCancelable(false);
                            builder.show();

                        }
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.dismiss();
                    final AlertDialog.Builder builder = new AlertDialog.Builder(Bookinglist.this);
                    builder.setTitle("Information confirmation")
                            .setMessage("You dont have data to be uploaded yet, please add new transactions. Thank you.")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                    builder.create();
                    builder.setCancelable(false);
                    builder.show();
                }
            });
        }
    }

    public void threadCustomers(){
        db = generaldb.getReadableDatabase();
        String query = " SELECT * FROM "+generaldb.tbname_customers+" WHERE "
                +generaldb.cust_createdby+" = '"+helper.logcount()+"'";
        Cursor cx = db.rawQuery(query, null);
        if (cx.getCount() != 0) {
            try {
                String link = helper.getUrl();
                URL url = new URL("http://" + link + "/api/customer/save.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.accumulate("data", generaldb.getAllCustomers(helper.logcount() + ""));

                Log.e("JSON", jsonParam.toString());
                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsonParam.toString());

                os.flush();
                os.close();

                Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                Log.i("MSG", conn.getResponseMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public JSONArray getAllBookingConsignee(String id) {
        SQLiteDatabase myDataBase = generaldb.getReadableDatabase();
        String raw = "SELECT * FROM " + generaldb.tbname_booking_consignee_box+" WHERE "+generaldb.book_con_transaction_no+" = '"+id+"'";
        Cursor cursor = myDataBase.rawQuery(raw, null);
        JSONArray resultSet = new JSONArray();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();
            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        if (cursor.getString(i) != null) {
                            Log.d("TAG_NAME", cursor.getString(i));
                            rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                        } else {
                            rowObject.put(cursor.getColumnName(i), "");
                        }
                    } catch (Exception e) {
                        Log.d("TAG_NAME", e.getMessage());
                    }
                }
            }
            resultSet.put(rowObject);
            cursor.moveToNext();
        }

        cursor.close();
        //Log.e("result set", resultSet.toString());
        return resultSet;
    }

    public JSONArray getBookingPayment(String id) {
        SQLiteDatabase myDataBase = generaldb.getReadableDatabase();
        String raw = "SELECT * FROM " + generaldb.tbname_payment+" WHERE "
                +generaldb.pay_booking_id+" = '"+id+"' GROUP BY "+generaldb.pay_booking_id;
        Cursor cursor = myDataBase.rawQuery(raw, null);
        JSONArray resultSet = new JSONArray();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();

            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        if (cursor.getString(i) != null) {
                            Log.d("TAG_NAME", cursor.getString(i));
                            rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                        } else {
                            rowObject.put(cursor.getColumnName(i), "");
                        }
                    } catch (Exception e) {
                        Log.d("TAG_NAME", e.getMessage());
                    }
                }
            }
            resultSet.put(rowObject);
            cursor.moveToNext();
        }

        cursor.close();
        //Log.e("result set", resultSet.toString());
        return resultSet;
    }

    public JSONArray getBookingCustomer(String id) {
        SQLiteDatabase myDataBase = generaldb.getReadableDatabase();
        String raw = "SELECT * FROM " + generaldb.tbname_customers
                +" WHERE "+generaldb.cust_accountnumber+" = '"+id+"'";
        Cursor cursor = myDataBase.rawQuery(raw, null);
        JSONArray resultSet = new JSONArray();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {

            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();

            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        if (cursor.getString(i) != null) {
                            Log.d("TAG_NAME", cursor.getString(i));
                            rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                        } else {
                            rowObject.put(cursor.getColumnName(i), "");
                        }
                    } catch (Exception e) {
                        Log.d("TAG_NAME", e.getMessage());
                    }
                }
            }
            resultSet.put(rowObject);
            cursor.moveToNext();
        }

        cursor.close();
        //Log.e("result set", resultSet.toString());
        return resultSet;
    }

    public JSONArray getBookingDiscount(String id) {
        SQLiteDatabase myDataBase = generaldb.getReadableDatabase();
        String raw = "SELECT * FROM " + generaldb.tbname_discount
                + " WHERE "+generaldb.disc_trans_no+" = '"+id+"'";
        Cursor c = myDataBase.rawQuery(raw, null);
        JSONArray resultSet = new JSONArray();
        c.moveToFirst();
        try {
            while (!c.isAfterLast()) {
                JSONObject js = new JSONObject();
                String trans = c.getString(c.getColumnIndex(generaldb.disc_trans_no));
                String disc = c.getString(c.getColumnIndex(generaldb.disc_discount));
                String remarks = c.getString(c.getColumnIndex(generaldb.disc_remarks));

                js.put("transaction_no", trans);
                js.put("discount", disc);
                js.put("remarks", remarks);
                resultSet.put(js);
                c.moveToNext();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        c.close();
        //Log.e("result_set", resultSet.toString());
        return resultSet;
    }

    public JSONArray getBookingImage(String id) {
        SQLiteDatabase myDataBase = rate.getReadableDatabase();
        String raw = "SELECT * FROM " + rate.tbname_generic_imagedb
                + " WHERE "+rate.gen_trans+" = '"+id+"' AND "+rate.gen_module+" = 'booking'";
        Cursor c = myDataBase.rawQuery(raw, null);
        JSONArray resultSet = new JSONArray();
        c.moveToFirst();
        try {
            while (!c.isAfterLast()) {
                JSONObject js = new JSONObject();
                String module = c.getString(c.getColumnIndex(rate.gen_module));
                String trans = c.getString(c.getColumnIndex(rate.gen_trans));
                byte[] image = c.getBlob(c.getColumnIndex(rate.gen_image));
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                byte[] bitmapdata = getBytesFromBitmap(bitmap);

                // get the base 64 string
                String imgString = Base64.encodeToString(bitmapdata, Base64.NO_WRAP);
                js.put("module", module);
                js.put("image", imgString);
                resultSet.put(js);
                c.moveToNext();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        c.close();
        Log.e("result_set", resultSet.toString());
        return resultSet;
    }

    // convert from bitmap to byte array
    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    //send sync data
    public void sendPost() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                threadCustomers();

                threadBooking();

            }
        });

        thread.start();
    }

    //update booking upload status
    public void updateBookingStat(ArrayList<String> trans){
        if (trans.size() != 0) {
            SQLiteDatabase db = generaldb.getWritableDatabase();
            for (String tr : trans) {
                ContentValues cv = new ContentValues();
                cv.put(generaldb.book_upds, "2");
                db.update(generaldb.tbname_booking, cv,
                        generaldb.book_transaction_no + " = '" + tr + "' AND " +
                                generaldb.book_upds + " = '1'", null);
                Log.e("upload", "uploaded booking");
            }
            db.close();
        }
    }

    //get booking status
    public String getStatus(String trans){
        String stat = "For Acceptance";
        SQLiteDatabase db = generaldb.getReadableDatabase();
        Cursor res = db.rawQuery(" SELECT * FROM "
                + generaldb.tbname_booking + " WHERE "
                + generaldb.book_transaction_no + " = '" + trans + "'", null);
        if (res.moveToNext()){
            int a = res.getInt(res.getColumnIndex(generaldb.book_booking_status));
            switch(a){
                case 1:
                    stat = "For Acceptance";
                    break;
                case 2:
                    stat = "Accepted";
                    break;
                case 3:
                    stat = "Loaded";
                    break;
                case 4:
                    stat = "Unloaded";
                    break;
                case 5:
                    stat = "Distributed";
                    break;
                case 6:
                    stat = "Delivered";
                    break;
                case 7:
                    stat = "In-Transit";
                    break;
                    default:break;
            }
        }
        return stat;
    }

    //shared preference
    public void preference(){
        SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(this);
        String themeName = pref.getString("theme", "Theme1");
        if (themeName.equals("Default(Red)")) {
            setTheme(R.style.AppTheme);
        } else if (themeName.equals("Light Blue")) {
            setTheme(R.style.customtheme);
        }else if (themeName.equals("Green")) {
            setTheme(R.style.customgreen);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTINGS_ACTION) {
            if (resultCode == Preferences.RESULT_CODE_THEME_UPDATED) {
                finish();
                startActivity(getIntent());
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
