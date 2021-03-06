package com.example.admin.gpxbymodule;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class Receiver extends Fragment {

    ListView lv;
    GenDatabase gen;
    HomeDatabase home;
    RatesDB rate;
    AutoCompleteTextView receivername;
    ImageButton addcust;
    IntentIntegrator scanIntegrator;
    int requestcode = 1;
    EditText boxnum;
    Booking book;
    Button addconsignee;
    String accnt, boxid;
    ArrayList<ListItem> result ;
    ArrayList<String> clickids, boxnumbers;
    FloatingActionButton addnewitem;
    Spinner sour, dest, nsbspin, bcontent;
    int sourceid, destid, selectedbt;
    ArrayList<String> boxesids;
    TextView nsbid;
    ArrayList<LinearItem> itemsboxnsb;
    LinearList ad;
    String nsbname = null, len, wid, hei;
    EditText l, w, h;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.receiver, null);

        gen = new GenDatabase(getContext());
        home = new HomeDatabase(getContext());
        rate = new RatesDB(getContext());
        book = (Booking) getActivity();
        clickids = new ArrayList<>();
        boxnumbers = new ArrayList<>();
        boxesids = new ArrayList<>();

        lv = (ListView)view.findViewById(R.id.lv);
        addnewitem = (FloatingActionButton)view.findViewById(R.id.addnew);
        accnt = book.getAccntno();

        if (book.getTransNo() != null ){
            book.setTransNo(book.getTransNo());
            Log.e("booktrans", book.getTransNo());
        }
        if (book.getClicksids() != null ){
            clickids = book.getClicksids();
        }

        withReservation();

        if (book.getReserveno() != null ) {
            addtoListWithReservation(book.getReserveno());
        }
        if (book.getAccntno() != null ){
            Log.e("account", book.getAccntno());
        }

        addnewitem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNSB();
                Log.e("nsb", "nsb form");
            }
        });
        return  view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle("Select Receiver");
        setHasOptionsMenu(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.savebooking).setVisible(false);
        menu.findItem(R.id.loadprev).setVisible(true);
        menu.findItem(R.id.btnnext).setVisible(false);
        menu.findItem(R.id.loadprevpay).setVisible(false);
        menu.findItem(R.id.btnnextpay).setVisible(true);
        super.onPrepareOptionsMenu(menu);
    }

    public void withReservation(){
        try{
            if (book.getTransNo() != null ) {
                result = getAllBoxInTransNo(book.getTransNo());
                ListAdapter adapter = new ListAdapter(getContext(), result);
                lv.setAdapter(adapter);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        TextView top = (TextView) view.findViewById(R.id.topitem);
                        TextView sub = (TextView) view.findViewById(R.id.subitem);
                        TextView ids = (TextView) view.findViewById(R.id.idget);
                        final String idg = ids.getText().toString();
                        final String nub = sub.getText().toString();
                        final String head = top.getText().toString();
                        selectedbt = getBoxId(result.get(position).getAmount());
                        Log.e("boxtypeID", ""+selectedbt);

                        itemclick(idg, head, nub);
                    }
                });
                lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        TextView ids = (TextView) view.findViewById(R.id.idget);
                        final String idg = ids.getText().toString();
                        //Log.e("idbook", ""+idg);
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Delete this box.?");
                        builder.setMessage(Html.fromHtml("<b>note:</b>"+
                                "Once you delete this box, you can not retrieve it back."))
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface d, int id) {
                                        gen.delBookingBox(idg);
                                        withReservation();
                                        d.dismiss();
                                    }
                                });
                        builder.create().show();
                        return true;
                    }
                });
            }
        }catch (Exception e){}
    }

    public void addnewcustomer(){
        try {
            addcust.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent mIntent = new Intent(getContext(), AddCustomer.class);
                    mIntent.putExtra("key", "booking");
                    startActivity(mIntent);
                }
            });
        }catch (Exception e){}
    }

    public void autoFullname(){
        try{
            String[] names = gen.getReceiverName();
            ArrayAdapter<String> adapter = new ArrayAdapter<String>
                    (getContext(), android.R.layout.select_dialog_item, names);

            receivername.setThreshold(1);
            receivername.setAdapter(adapter);
            receivername.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String val = (String)parent.getItemAtPosition(position);
                    receivername.setText(val);
                    if (gen.getCustomerInfo(val)) {
                        SQLiteDatabase db = gen.getReadableDatabase();
                        Cursor v = db.rawQuery(" SELECT * FROM " + gen.tbname_customers +
                                " WHERE "+gen.cust_fullname+ " = '"+val+"'", null);
                        if(v.moveToNext()) {
                            String fname = v.getString(v.getColumnIndex(gen.cust_fullname));
                            accnt = v.getString(v.getColumnIndex(gen.cust_accountnumber));
                            receivername.setText(fname);
                        }
                    }
                }
            });
        }catch (Exception e){}
    }

    public void focus(){
        try{
            boxnum.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus){
                        scanpermit();
                    }
                }
            });

            boxnum.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scanpermit();
                }
            });

        }catch (Exception e){}
    }

    public void scanpermit(){
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.CAMERA}, 100);
        }else{
            scanIntegrator = IntentIntegrator.forSupportFragment(Receiver.this);
            scanIntegrator.setPrompt("Scan barcode");
            scanIntegrator.setBeepEnabled(true);
            scanIntegrator.setOrientationLocked(true);
            scanIntegrator.setCaptureActivity(CaptureActivityPortrait.class);
            scanIntegrator.initiateScan();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result.getContents() != null) {
                String bn = result.getContents();
                    if (getBoxNumber(bn)) {
                        if (!checkBoxnum(bn)) {
                            if (!checkBoxnumOne(bn)) {
                                boxnum.setText(bn);
                            } else {
                                delExistStatOne(bn);
                                boxnum.setText(bn);
                            }
                        } else {
                            String box = "Boxnumber has been used, please try another.";
                            customToast(box);
                        }
                    }else{
                        if (getBoxNumberBarcode(bn)) {
                       // if (getBoxNumber(bn)) {
                            if (!checkBoxnumNSB(bn)){
                                if (!checkBoxnumNSBOne(bn)) {
                                    //boxid = getBoxName(bn);
                                    Log.e("addnsbboxid", boxid);
                                    boxnum.setText(bn);
                                }else{
                                    delExistStatOne(bn);
                                    Log.e("addnsbboxid", boxid);
                                    boxnum.setText(bn);
                                }
                            }else{
                                String box = "Boxnumber has been used, please try another.";
                                customToast(box);
                            }
                       // }
                      }else{
                            if (!checkBoxnumNSB(bn)){
                                if (!checkBoxnumNSBOne(bn)) {
                                    //boxid = getBoxName(bn);
                                    Log.e("addnsbboxid", boxid);
                                    boxnum.setText(bn);
                                }else{
                                    delExistStatOne(bn);
                                    Log.e("addnsbboxid", boxid);
                                    boxnum.setText(bn);
                                }
                            }else{
                                String box = "Boxnumber has been used, please try another.";
                                customToast(box);
                            }
                        }
                    }
            } else
                super.onActivityResult(requestCode, resultCode, data);
        }catch (Exception e){}
    }

    public String getAccntNo(String fulln){
        String fullname = null;
            SQLiteDatabase db = gen.getReadableDatabase();
            Cursor v = db.rawQuery(" SELECT * FROM " + gen.tbname_customers +
                    " WHERE "+gen.cust_fullname+ " = '"+fulln+"'", null);
            if(v.moveToNext()) {
                fullname = v.getString(v.getColumnIndex(gen.cust_accountnumber));
            }
        return fullname;
    }

    //alert dialog for item clicked
    public void itemclick(final String id, String rc, String boxnumber){
        try {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            LayoutInflater inflater = Receiver.this.getLayoutInflater();
            View d = inflater.inflate(R.layout.newreceiver, null);
            dialogBuilder.setTitle("Set receiver");

            receivername = (AutoCompleteTextView) d.findViewById(R.id.receiverinput);
            boxnum = (EditText) d.findViewById(R.id.re_boxinput);
            addcust = (ImageButton) d.findViewById(R.id.add_cust);
            sour = (Spinner) d.findViewById(R.id.source);
            dest = (Spinner) d.findViewById(R.id.destination);
            bcontent = (Spinner) d.findViewById(R.id.boxcont);

            //populate listview for receiver
            String[] names = gen.getYourReceivers(book.getAccntno());
            ListView list = (ListView) d.findViewById(R.id.list);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                    android.R.layout.simple_list_item_single_choice, names);
            list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String selname = (String) parent.getItemAtPosition(position);
                    receivername.setText(selname);
                    receivername.requestFocus();
                }
            });

            //populate box contents
            String[] bbcont = rate.getAllBoxContents();
            ArrayAdapter<String> bcontadapter =
                    new ArrayAdapter<>(getContext(), R.layout.spinneritem,
                            bbcont);
            bcontent.setAdapter(bcontadapter);

            //populate sources
            String[] sr = rate.getSources();
            ArrayAdapter<String> sourceadapter =
                    new ArrayAdapter<>(getContext(), R.layout.spinneritem,
                            sr);
            sour.setEnabled(false);
            sour.setAdapter(sourceadapter);
            sour.setSelection(sourceadapter.getPosition(getYourBranch()));
            sourceadapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
            sour.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    sourceid = getSourceid(sour.getSelectedItem().toString());
                    Log.e("sourceid", sourceid + "");
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    sourceid = getSourceid(sour.getSelectedItem().toString());
                }
            });

            //populate destinations
            String[] des = rate.getDest();
            ArrayAdapter<String> destadapter =
                    new ArrayAdapter<>(getContext(), R.layout.spinneritem,
                            des);
            dest.setAdapter(destadapter);
            destadapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
            dest.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    destid = getDestid(dest.getSelectedItem().toString());
                    Log.e("destid", destid + "");
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    destid = getDestid(dest.getSelectedItem().toString());
                }
            });

            list.setOnTouchListener(new View.OnTouchListener() {
                // Setting on Touch Listener for handling the touch inside ScrollView
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Disallow the touch request for parent scroll on touch of child view
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return false;
                }
            });

            receivername.setText(rc);
            boxnum.setText(boxnumber);

            addnewcustomer();
            focus();
            autoFullname();

            Button close = (Button) d.findViewById(R.id.cancel);
            Button confirm = (Button) d.findViewById(R.id.confirm);

            dialogBuilder.setView(d);
            final AlertDialog alertDialog = dialogBuilder.show();
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                }
            });
            confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (receivername.getText().toString().equals("")) {
                        String h = "Receiver name is empty.";
                        customToast(h);
                    } else if (boxnum.getText().toString().equals("")) {
                        String h = "Box number is empty.";
                        customToast(h);
                    } else {
                        String trans = book.getTransNo();
                        String re = receivername.getText().toString();
                        String bn = boxnum.getText().toString();
                        String bcont = bcontent.getSelectedItem().toString();
                        if (!boxnumbers.contains(bn)){
                            boxnumbers.add(bn);
                        }
                        boolean feed = checkHardPort(getProvince(getAccntNo(receivername.getText().toString())));
                        Log.e("hardport", feed+"");
                        if (feed == true) {
                            gen.updConsigneeBooking(id, getAccntNo(re), sourceid + "",
                                    destid + "", trans, bn, "1","1",bcont);
                        }else{
                            gen.updConsigneeBooking(id, getAccntNo(re), sourceid + "",
                                    destid + "", trans, bn, "1","0",bcont);
                        }
                        Log.e("boxtype", selectedbt + "");
                        if (book.getPayamount() == 0) {
                            String calc = calculate(selectedbt + "",
                                    sourceid + "", destid + "");
                            if (calc != null){
                                book.setPayamount(Double.valueOf(calc));
                                Log.e("amount", book.getPayamount() + "");
                            }

                            if (!clickids.contains(id)) {
                                clickids.add(id);
                            }
                        } else {
                            if (!clickids.contains(id)) {
                                String calc = calculate(selectedbt + "",
                                        sourceid + "", destid + "");
                                if (calc != null){
                                    Double famount = ((book.getPayamount()) + (Double.valueOf(calc)));
                                    book.setPayamount(famount);
                                }

                                clickids.add(id);
                                Log.e("ids", clickids.toString() + "");
                                Log.e("amount", book.getPayamount() + "");
                            } else {
                                book.setPayamount(book.getPayamount());
                                Log.e("amount", book.getPayamount() + "");
                            }
                        }
                        withReservation();
                    }
                    alertDialog.dismiss();
                }
            });
        }catch (Exception e){}
    }

    public void addtoListWithReservation(String reservation){
        try{
            SQLiteDatabase db = gen.getWritableDatabase();
            Cursor res = db.rawQuery(" SELECT * FROM "+gen.tbname_reservation_boxtype_boxnumber
                    + " WHERE "+gen.res_btype_bnum_reservation_id+ " = '"+reservation+"' AND "
                    +gen.res_btype_bnum_box_number+" != 'NULL' AND "+gen.res_btype_bnum_stat+" = '2'",null);
            res.moveToFirst();
            while(!res.isAfterLast()) {
                String transactionum = book.getTransNo();
                String bnumber = res.getString(res.getColumnIndex(gen.res_btype_bnum_box_number));
                String bt = res.getString(res.getColumnIndex(gen.res_btype_bnum_boxtype));
                Log.e("btype", bt);
                SQLiteDatabase gx = gen.getWritableDatabase();
                Cursor getb = gx.rawQuery(" SELECT * FROM "+gen.tbname_booking_consignee_box
                        +" WHERE "+gen.book_con_box_number+" = '"+bnumber+"'", null);
                if (getb.getCount() == 0){
                    gen.addConsigneeBooking(null, bt,"", "", transactionum, bnumber,"0", "0", "");
                }
                getb.close();

                Log.e("trans", transactionum);
                res.moveToNext();
            }
            withReservation();
            res.close();
        }catch (Exception e){}
    }

    public void addNSB(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = Receiver.this.getLayoutInflater();
        View d = inflater.inflate(R.layout.nsbform, null);
        dialogBuilder.setTitle("New Box");

        receivername = (AutoCompleteTextView)d.findViewById(R.id.receiverinput);
        boxnum = (EditText)d.findViewById(R.id.re_boxinput);
        addcust = (ImageButton)d.findViewById(R.id.add_cust);
        nsbspin = (Spinner)d.findViewById(R.id.nsbtype);
        bcontent = (Spinner)d.findViewById(R.id.boxcont);

        //populate box contents
        String[] bbcont = rate.getAllBoxContents();
        ArrayAdapter<String> bcontadapter =
                new ArrayAdapter<>(getContext(), R.layout.spinneritem,
                        bbcont);
        bcontent.setAdapter(bcontadapter);

        itemsboxnsb = getNSBboxes();
        ad = new LinearList(getContext(), itemsboxnsb);
        nsbspin.setAdapter(ad);
        nsbspin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nsbid = (TextView)view.findViewById(R.id.dataid);
                boxid = nsbid.getText().toString();
                Log.e("nsbboxid", boxid+"");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                nsbid = (TextView)parent.findViewById(R.id.dataid);
                boxid = nsbid.getText().toString();
                Log.e("nsbboxid", boxid+"");
            }
        });

        addnewcustomer();
        autoFullname();
        focus();

        Button close = (Button) d.findViewById(R.id.cancel);
        Button confirm = (Button) d.findViewById(R.id.confirm);

        dialogBuilder.setView(d);
        final AlertDialog alertDialog = dialogBuilder.show();
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("box_type_id", boxid+"");
                if (checkIFNSB(boxid)){
                    addlengthwidth();
                    alertDialog.dismiss();
                }else{
                    newboxsourcedes(0);
                    alertDialog.dismiss();
                }
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }

    public void addlengthwidth(){
        AlertDialog.Builder dlwh = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = Receiver.this.getLayoutInflater();
        View d = inflater.inflate(R.layout.nsblwh, null);
        dlwh.setTitle("NSB Box dimensions");

        l = (EditText)d.findViewById(R.id.length);
        w = (EditText)d.findViewById(R.id.width);
        h = (EditText)d.findViewById(R.id.height);

        dlwh.setView(d);
        dlwh.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface di, int which) {
                len = l.getText().toString();
                wid = w.getText().toString();
                hei = h.getText().toString();
                newboxsourcedes(1);
                di.dismiss();
            }
        });
        dlwh.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dicancel, int which) {
                dicancel.dismiss();
            }
        });
//        final AlertDialog alertDialog = dlwh.show();
        dlwh.show();

    }

    public void newboxsourcedes(final int i){
        AlertDialog.Builder dlwh = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = Receiver.this.getLayoutInflater();
        View d = inflater.inflate(R.layout.nsbsourcedest, null);
        dlwh.setTitle("New box destinations");

        sour = (Spinner)d.findViewById(R.id.source);
        dest = (Spinner)d.findViewById(R.id.destination);

        //populate sources
        String[] sr = rate.getSources();
        ArrayAdapter<String> sourceadapter =
                new ArrayAdapter<>(getContext(), R.layout.spinneritem,
                        sr);
        sour.setEnabled(false);
        sour.setAdapter(sourceadapter);
        sour.setSelection(sourceadapter.getPosition(getYourBranch()));
        sourceadapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        sour.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sourceid = getSourceid(sour.getSelectedItem().toString());
                Log.e("sourceid", sourceid+"");
//                Log.e("sourceid", sour.getSelectedItem().toString()+"");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sourceid = getSourceid(sour.getSelectedItem().toString());
            }
        });

        //populate destinations
        String[] des = rate.getDest();
        ArrayAdapter<String> destadapter =
                new ArrayAdapter<>(getContext(), R.layout.spinneritem, des);
        dest.setAdapter(destadapter);
        destadapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        dest.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                destid = getDestid(dest.getSelectedItem().toString());
                Log.e("destid", destid+"");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                destid = getDestid(dest.getSelectedItem().toString());
            }
        });

        dlwh.setView(d);
        dlwh.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface di, int which) {
                Log.e("box_type_id", boxid+"");
                if (i == 1){
                    savePartialConsigneeNSB();
                }else{
                    savePartialConsignee();
                }
                di.dismiss();
            }
        });
        dlwh.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dicancel, int which) {
                dicancel.dismiss();
            }
        });
        dlwh.show();

    }

    public int getBoxId(String id){
        int i = 0;
        SQLiteDatabase db = gen.getReadableDatabase();
        Cursor c = db.rawQuery(" SELECT * FROM "+gen.tbname_boxes+" WHERE "+gen.box_name+" = '"+id+"'", null);
        if (c.moveToNext()){
            i = c.getInt(c.getColumnIndex(gen.box_id));
        }
        return i;
    }

    public String calculate(String t, String sid, String did){
        String rateval = null;
        SQLiteDatabase db = rate.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM " + rate.tbname_rates
                + " WHERE " + rate.rate_boxtype + " = '" + t + "' AND " + rate.rate_source_id + " = '" + sid + "' AND " + rate.rate_destination_id + " = '" + did + "'", null);
        if (x.getCount() != 0) {
            x.moveToNext();
            rateval = x.getString(x.getColumnIndex(rate.rate_amount));
        }
        return rateval;
    }

    public  int getSourceid(String name){
        int x = 0;
        SQLiteDatabase db = rate.getReadableDatabase();
        Cursor c = db.rawQuery(" SELECT "+rate.sd_id+" FROM "+rate.tbname_sourcedes
                +" WHERE "+rate.sd_name+" = '"+name+"'", null);
        if (c.moveToNext()){
            x = c.getInt(c.getColumnIndex(rate.sd_id));
        }
        return x;
    }

    public  int getDestid(String name){
        int x = 0;
        SQLiteDatabase db = rate.getReadableDatabase();
        Cursor c = db.rawQuery(" SELECT "+rate.sd_id+" FROM "+rate.tbname_sourcedes
                +" WHERE "+rate.sd_name+" = '"+name+"'", null);
        if (c.moveToNext()){
            x = c.getInt(c.getColumnIndex(rate.sd_id));
        }
        return x;
    }

    public int getLastId(){
        int lastid = 0;
        SQLiteDatabase db = gen.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+gen.tbname_booking_consignee_box, null);
        if (x.moveToLast()){
            lastid = x.getInt(x.getColumnIndex(gen.book_con_box_id));
        }
        return lastid;
    }

    public boolean checkBoxnum(String bn){
        SQLiteDatabase db = gen.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+gen.tbname_booking_consignee_box
        +" WHERE "+gen.book_con_box_number+" = '"+bn+"' AND "+gen.book_con_stat+" = '2'", null);
        if (x.getCount() > 0){
            return true;
        }
        return false;
    }

    public boolean checkBoxnumOne(String bn){
        SQLiteDatabase db = gen.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+gen.tbname_booking_consignee_box
        +" WHERE "+gen.book_con_box_number+" = '"+bn+"' AND "+gen.book_con_stat+" = '2'", null);
        if (x.getCount() > 0){
            return true;
        }
        return false;
    }

    public boolean checkBoxnumNSB(String bn){
        SQLiteDatabase db = gen.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+gen.tbname_booking_consignee_box
        +" WHERE "+gen.book_con_box_number+" = '"+bn+"' AND "+gen.book_con_stat+" = '2'", null);
        if (x.getCount() > 0){
            return true;
        }
        return false;
    }

    public boolean checkBoxnumNSBOne(String bn){
        SQLiteDatabase db = gen.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+gen.tbname_booking_consignee_box
                +" WHERE "+gen.book_con_box_number+" = '"+bn+"' AND "+gen.book_con_stat+" = '1'", null);
        if (x.getCount() > 0){
            return true;
        }
        return false;
    }

    public boolean checkIfInInventory(String bn){
        SQLiteDatabase db = gen.getReadableDatabase();
        String q = " SELECT * FROM "+gen.tbname_driver_inventory
                +" WHERE "+gen.sdinv_boxnumber+" = '"+bn+"'";
        Cursor x = db.rawQuery( q, null);
        if (x.getCount() != 0 ){
            return true;
        }else{
            return false;
        }
    }

    public String getBoxName(String bn){
        String btype = null;
        SQLiteDatabase db = gen.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+gen.tbname_boxes
                +" JOIN "+gen.tbname_tempboxes+" ON "+gen.tbname_tempboxes+"."+gen.dboxtemp_boxid
                +" = "+gen.tbname_boxes+"."+gen.box_id
        +" WHERE "+gen.tbname_tempboxes+"."+gen.dboxtemp_boxnumber+" = '"+bn+"'", null);
        if (x.moveToNext()){
            btype = x.getString(x.getColumnIndex(gen.box_name));
        }
        x.close();
        return btype;
    }

    public String getBoxNameIDused(String id){
        String btype = null;
        SQLiteDatabase db = gen.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+gen.tbname_boxes
        +" WHERE "+gen.box_id+" = '"+id+"'", null);
        if (x.moveToNext()){
            btype = x.getString(x.getColumnIndex(gen.box_name));
        }
        x.close();
        return btype;
    }

    public boolean getBoxNumber(String bn){
        SQLiteDatabase db = gen.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+gen.tbname_driver_inventory
        +" WHERE "+gen.sdinv_boxnumber+" = '"+bn+"'", null);
        if (x.getCount() != 0){
            return true;
        }else {
            return false;
        }
    }

    public boolean getBoxNumberBarcode(String bn){
        SQLiteDatabase db = rate.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+rate.tbname_barcode_driver_inventory
        +" WHERE "+rate.barcodeDriverInv_boxnumber+" = '"+bn+"'", null);
        if (x.getCount() != 0){
            return true;
        }else {
            return false;
        }
    }

    @Override
    public void onPause(){
        try{
            super.onPause();
            if ((result.size() != clickids.size()) || (clickids.size() == 0)){
                String x = "Please make sure to re-add the receiver name if you are editing this transaction," +
                        " thank you.";
                customToast(x);
            }
            book.setClickcount(clickids.size());
            book.setClicksids(clickids);
            if (book.getReserveno() != null) {
                Log.e("reservenum", book.getReserveno());
            }else{
                Log.e("reservenum", "reservenum null");
            }

            book.setBoxnumbers(boxnumbers);
            book.setBoxids(boxesids);
            Log.e("boxnumbers", book.getBoxnumbers().toString());

        }catch (Exception e){}
    }

    public void customToast(String txt){
        Toast toast = new Toast(getContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        LayoutInflater inflater = Receiver.this.getLayoutInflater();
        View view = inflater.inflate(R.layout.toast, null);
        TextView t = (TextView)view.findViewById(R.id.toasttxt);
        t.setText(txt);
        toast.setView(view);
        toast.setGravity(Gravity.TOP | Gravity.RIGHT, 15, 50);
        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.enterright);
        view.startAnimation(animation);
        toast.show();
    }

    public ArrayList<LinearItem> getNSBboxes() {
        ArrayList<LinearItem> result = new ArrayList<>();
        SQLiteDatabase dbrate = rate.getReadableDatabase();
        String x = " SELECT * FROM "+rate.tbname_boxes;
        Cursor xc = dbrate.rawQuery( x, null);
        xc.moveToFirst();
        while (!xc.isAfterLast()){
            String boxid = xc.getString(xc.getColumnIndex(rate.box_id));
            String boxname = xc.getString(xc.getColumnIndex(rate.box_name));
            String size = xc.getString(xc.getColumnIndex(rate.box_length))+" * "
                    + xc.getString(xc.getColumnIndex(rate.box_width))+" * "
                    + xc.getString(xc.getColumnIndex(rate.box_height));
            String f = Html.fromHtml("<small>"+size+"</small>").toString();
            LinearItem item = new LinearItem(boxid, boxname, f);
            result.add(item);
            xc.moveToNext();
        }
        xc.close();
        return result;
    }

    public void delExistStatOne(String boxnum) {
        SQLiteDatabase db = gen.getWritableDatabase();
        db.delete(gen.tbname_booking_consignee_box,
                gen.book_con_box_number + " = '" + boxnum + "' AND " + gen.book_con_stat + " = '1'", null);
        Log.e("deleted", boxnum);
        db.close();
    }

    public String getProvince(String accnt){
        String provincecode = "";
        SQLiteDatabase db = gen.getReadableDatabase();
        String x = " SELECT * FROM "+gen.tbname_customers
                +" WHERE "+gen.cust_accountnumber+" = '"+accnt+"' AND "
                +gen.cust_type+" = 'receiver'";
        Cursor c = db.rawQuery(x, null);
        if (c.moveToNext()){
            provincecode = c.getString(c.getColumnIndex(gen.cust_prov));
        }
        Log.e("provcode", provincecode);
        return provincecode;
    }

    public boolean checkHardPort(String provcode){
        boolean ok = false;
        SQLiteDatabase db = rate.getReadableDatabase();
        String x = " SELECT * FROM "+rate.tbname_provinces
                +" WHERE "+rate.prov_code+" = '"+provcode+"'";
        Cursor c = db.rawQuery(x, null);
        if (c.getCount() != 1){
            String y = " SELECT * FROM "+rate.tbname_provinces
                    +" WHERE "+rate.prov_code+" = '"+provcode+"' ORDER BY "+rate.prov_hardport+" DESC LIMIT 1 ";
            Cursor cd = db.rawQuery(y, null);
            if (cd.moveToNext()){
                int port = cd.getInt(cd.getColumnIndex(rate.prov_hardport));
                if (port == 1 ){
                    ok = true;
                }else{
                    ok = false;
                }
            }
        }else{
            if (c.moveToNext()){
                int port = c.getInt(c.getColumnIndex(rate.prov_hardport));
                if (port == 1 ){
                    ok = true;
                }else{
                    ok = false;
                }
            }
        }

        return ok;
    }

    public boolean checkIFNSB(String boxid){
        boolean ok = false;
        SQLiteDatabase db = gen.getReadableDatabase();
        String x = " SELECT * FROM "+gen.tbname_boxes
                +" WHERE "+gen.box_id+" = '"+boxid+"'";
        Cursor c = db.rawQuery(x, null);
        if (c.moveToNext()){
         int nsbi = c.getInt(c.getColumnIndex(gen.box_nsb));
         if (nsbi == 1){
             ok = true;
         }else if (nsbi == 0){
             ok = false;
         }
        }
        return ok;
    }

    public void savePartialConsignee(){
        try {
            String re = getAccntNo(receivername.getText().toString());
            String bnum = boxnum.getText().toString();
            String bcont = bcontent.getSelectedItem().toString();
            if (re.equals("")) {
                String t = "Receiver name is invalid.";
                customToast(t);
            } else if (bnum.equals("")) {
                String t = "Box number is empty.";
                customToast(t);
            } else {
                if (!boxnumbers.contains(bnum)) {
                    boxnumbers.add(bnum);
                }
                boxesids.add(boxid);
                Log.e("boxesids", boxesids.toString());
                boolean feed = checkHardPort(getProvince(getAccntNo(receivername.getText().toString())));
                if (feed == true) {
                    gen.addConsigneeBooking(re, getBoxNameIDused(boxid), sourceid + "", destid + "",
                            book.getTransNo(), bnum, "0", "1", bcont);
                    clickids.add(getLastId() + "");
                    Log.e("ids", clickids.toString() + "");
                } else {
                    gen.addConsigneeBooking(re, getBoxNameIDused(boxid), sourceid + "", destid + "",
                            book.getTransNo(), bnum, "0", "0", bcont);
                    clickids.add(getLastId() + "");
                    Log.e("ids", clickids.toString() + "");
                }
                if (book.getPayamount() == 0) {
                    String calc = calculate(boxid + "",
                            sourceid + "", destid + "");
                    if (calc != null){
                        book.setPayamount(Double.valueOf(calc));
                        Log.e("amount", book.getPayamount() + "");
                    }
                    Log.e("rate", calc+ "");
                } else {
                    String calc = calculate(boxid + "",
                            sourceid + "", destid + "");
                    if (calc != null){
                        Double famount = ((book.getPayamount()) + (Double.valueOf(calc)));
                        book.setPayamount(famount);
                    }

                    Log.e("amount", book.getPayamount() + "");
                }
                withReservation();
            }
        }catch (Exception e){
            Log.e("exception_con", e.getMessage());
        }
    }

    public void savePartialConsigneeNSB(){
        try{
            String re = getAccntNo(receivername.getText().toString());
            double length = Double.valueOf(len);
            double width = Double.valueOf(wid);
            double height = Double.valueOf(hei);
            double staticmillion = 1000000;
            String bnum = boxnum.getText().toString();
            String bcont = bcontent.getSelectedItem().toString();
            if (re.equals("")){
                String t ="Receiver name is invalid.";
                customToast(t);
            }else if (bnum.equals("")){
                String t ="Box number is empty.";
                customToast(t);
            }
            else {
                if (!boxnumbers.contains(bnum)){
                    boxnumbers.add(bnum);
                }
                boxesids.add(boxid);
                Log.e("boxesids", boxesids.toString());
                boolean feed = checkHardPort(getProvince(getAccntNo(receivername.getText().toString())));
                if (feed == true) {
                    gen.addConsigneeBooking(re, getBoxNameIDused(boxid), sourceid + "", destid + "",
                            book.getTransNo(), bnum, "0", "1", bcont);
                    clickids.add(getLastId()+"");
                    Log.e("ids", clickids.toString() + "");
                }else{
                    gen.addConsigneeBooking(re, getBoxNameIDused(boxid), sourceid + "", destid + "",
                            book.getTransNo(), bnum, "0", "0",bcont);
                    clickids.add(getLastId()+"");
                    Log.e("ids", clickids.toString() + "");
                }
                String lastid = getLastId()+"";
                if (book.getPayamount() == 0) {
                    //calculation formula
                    double frate = Double.valueOf(getNSBrate(boxid,sourceid+"",destid+""));
                    double fratestat = (length * width * height)/staticmillion;
                    double fratetotal = (fratestat)* frate;
                    book.setPayamount(fratetotal);
                    //clicked ids
                    Log.e("amount", book.getPayamount() + "");
                } else {
                    //calculation formula
                    double frate = Double.valueOf(getNSBrate(boxid,sourceid+"",destid+""));
                    double fratestat = (length * width * height)/staticmillion;
                    double fratetotal = (fratestat)* frate;
                    double getPayAm = book.getPayamount();
                    double total = getPayAm + fratetotal;
                    book.setPayamount(total);
                    Log.e("amount", book.getPayamount() + "");
                }
                withReservation();
            }
        }catch (Exception e){
            Log.e("exception_conNSB", e.getMessage());
        }
    }

    public String getYourBranch(){
        String branchname = null;
        SQLiteDatabase db = rate.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+rate.tbname_branch
                +" WHERE "+rate.branch_id+" = '"+home.getBranch(""+home.logcount())+"'", null);
        if (x.moveToNext()){
            branchname = x.getString(x.getColumnIndex(rate.branch_name));
        }
        return branchname;
    }

    public String getNSBrate(String boxid, String source, String dest){
        String rate = null;
        SQLiteDatabase db = gen.getReadableDatabase();
        Cursor x = db.rawQuery(" SELECT * FROM "+gen.tbname_nsbrate
                +" WHERE "+gen.nsbr_boxid+" = '"+boxid+"' AND "+gen.nsbr_sourceid+" = '"+source+"' AND "
                +gen.nsbr_destid+" = '"+dest+"'", null);
        if (x.moveToNext()){
            rate = x.getString(x.getColumnIndex(gen.nsbr_rate));
        }
        return rate;
    }

    //get booking boxes by transaction number
    public ArrayList<ListItem> getAllBoxInTransNo(String trans){
        ArrayList<ListItem> results = new ArrayList<>();
        SQLiteDatabase db = gen.getReadableDatabase();
        Cursor res = db.rawQuery(" SELECT * FROM " + gen.tbname_booking_consignee_box + " WHERE "
                + gen.book_con_transaction_no + " = '" + trans + "'", null);
        res.moveToFirst();
        while (!res.isAfterLast()) {
            String acount = res.getString(res.getColumnIndex(gen.book_con_box_account_no));
            String ids = res.getString(res.getColumnIndex(gen.book_con_box_id));
            String sub = res.getString(res.getColumnIndex(gen.book_con_box_number));
            String a = res.getString(res.getColumnIndex(gen.book_con_boxtype));
            String topitem = "", temptop = "";
            Cursor getname = db.rawQuery("SELECT " + gen.cust_fullname + " FROM " + gen.tbname_customers
                    + " WHERE " + gen.cust_accountnumber + " = '" + acount + "'", null);
            if (getname.moveToNext()) {
                temptop = getname.getString(getname.getColumnIndex(gen.cust_fullname));
            }
            ListItem list = new ListItem(ids, temptop, sub, a);
            results.add(list);
            res.moveToNext();
        }
        res.close();
        // Add some more dummy data for testing
        return results;
    }

}
