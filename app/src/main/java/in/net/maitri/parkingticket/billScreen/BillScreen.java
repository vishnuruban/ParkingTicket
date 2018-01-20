package in.net.maitri.parkingticket.billScreen;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.zj.btsdk.BluetoothService;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import in.net.maitri.parkingticket.DbHandler;
import in.net.maitri.parkingticket.NewTicketMst;
import in.net.maitri.parkingticket.R;
import in.net.maitri.parkingticket.TicketMst;
import in.net.maitri.parkingticket.bluetooth.BackUpAndRestoreDb;
import in.net.maitri.parkingticket.bluetooth.Bluetooth;
import in.net.maitri.parkingticket.bluetooth.DeviceListActivity;
import in.net.maitri.parkingticket.bluetooth.PlainPrint;


public class BillScreen extends AppCompatActivity {

    int REQUEST_ENABLE_BT = 4, REQUEST_CONNECT_DEVICE = 6;
    int effectivePrintWidth = 48;//set effective print width of bluetooth thermal printer e.g. set 48 for 2 inch/58 mm and 72 for 3 inch/80 mm printer


    //ArrayList<ChkOut> chk;
    int curTokenNo;
    private DbHandler mDbHandler;
    private int currentTicketNo;
    private TextView descriptionView, amountView;
    int code = 0;
    Button printButton, newTicketButton;
    RadioGroup radioGroup;
    String strDate;
    BluetoothService mService = null;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_screen);


        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        strDate = sdf.format(c.getTime());
        mDbHandler = new DbHandler(BillScreen.this);
        currentTicketNo = mDbHandler.getBillSeries().getCurrentBillNo();
        final TextView ticketNoView = (TextView) findViewById(R.id.ticketNo);
        ticketNoView.setText(String.valueOf(currentTicketNo));

        final TextView dateTimeView = (TextView) findViewById(R.id.dateTime);
        dateTimeView.setText(currentDateAndTime());
        descriptionView = (TextView) findViewById(R.id.desc);
        descriptionView.setText("2 Wheeler");
        amountView = (TextView) findViewById(R.id.amt);
        amountView.setText(String.valueOf(mDbHandler.getVehicleMst(1).getRate()));

        printButton = (Button) findViewById(R.id.print);
        newTicketButton = (Button) findViewById(R.id.new_ticket);

        radioGroup = (RadioGroup) findViewById(R.id.radiogroup);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                Log.d("id", String.valueOf(i));
                RadioButton r = (RadioButton) findViewById(i);
                String wheelerType = r.getText().toString();
                descriptionView.setText(wheelerType);
                dateTimeView.setText(currentDateAndTime());
                String[] reportype = {"2 Wheeler", "4 Wheeler"};
                amountView.setText(String.valueOf(mDbHandler.getVehicleMst(Arrays.asList(reportype).indexOf(r.getText().toString()) + 1).getRate()));

            }
        });

        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            printFunc();
            }
        });


        newTicketButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dateTimeView.setText(currentDateAndTime());
                currentTicketNo = mDbHandler.getBillSeries().getCurrentBillNo();
                ticketNoView.setText(String.valueOf(currentTicketNo));
                newTicketButton.setVisibility(View.INVISIBLE);
                printButton.setVisibility(View.VISIBLE);
                radioGroup.setEnabled(true);

            }
        });


    }


    void printFunc() {


        if (Bluetooth.isPrinterConnected(getApplicationContext(), BillScreen.this)) {
            mService = Bluetooth.getServiceInstance();
            if(!mService.isAvailable())
            {
                Toast.makeText(BillScreen.this,"Unable to connect Printer.try again",Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedId = radioGroup.getCheckedRadioButtonId();
            RadioButton rb = (RadioButton) findViewById(selectedId);
            String vehicle = rb.getText().toString();
            curTokenNo = currentTicketNo;
            System.out.println("DBDATE " + dbDate());
            Log.d("date", String.valueOf(mDbHandler.getDateCount(dbDate())));
            TicketMst tm = mDbHandler.getTicketMst(String.valueOf(mDbHandler.getDateCount(dbDate())));


//                Log.d("date", tm.getTktDate());
            // Log.d("veh1", String.valueOf(tm.getTktVehicle1()));
            //Log.d("veh2", String.valueOf(tm.getTktVehicle2()));
            //Log.d("vehicle", vehicle);
            tm.setTktDate(String.valueOf(mDbHandler.getDateCount(dbDate())));
            if (vehicle.equals("2 Wheeler")) {
                int v1 = tm.getTktVehicle1() + 1;
                tm.setTktVehicle1(v1);
            } else if (vehicle.equals("4 Wheeler")) {
                int v2 = tm.getTktVehicle2() + 1;
                tm.setTktVehicle2(v2);
            }
            if (mDbHandler.updateTicketNum(tm)) {
                NewTicketMst ntm = new NewTicketMst();
                int intTktNo = Integer.parseInt(mDbHandler.getSysValue("sys_internal_ticket"));
                ntm.setId(intTktNo);
                ntm.setDocNo(currentTicketNo);
                ntm.setDate(mDbHandler.getDateCount(dbDate()));
                ntm.setVehicle(vehicle);
                ntm.setRate(Integer.parseInt(amountView.getText().toString()));
                ntm.setDateTime(strDate);
                if (mDbHandler.addNewTicketMst(ntm)) {
                    mDbHandler.updateSysValue("sys_internal_ticket", String.valueOf(intTktNo + 1));
                    mDbHandler.updateBillNo(++currentTicketNo);
                    printButton.setVisibility(View.INVISIBLE);
                    radioGroup.setEnabled(false);
                    newTicketButton.setVisibility(View.VISIBLE);
                }
            }
            successmsg();

            printTabularData(descriptionView.getText().toString(), amountView.getText().toString());
        }
        else
    {
        Bluetooth.connectPrinter(getApplicationContext(), BillScreen.this);
    }

}
   /*     Button btnTabular=(Button)findViewById(R.id.btnTabular);
        btnTabular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //following is the function to print data in tabular format
                printTabularData();
            }
        });*/



    private String currentDateAndTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();
        return formatter.format(date);
    }


    private String dbDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        //    System.out.println("DBDATE "+date);
        return formatter.format(date);
    }

    private String getRsSymbol() {
        String rs = "l";
        try {
            byte[] utf8 = rs.getBytes("UTF-8");
            rs = new String(utf8, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "Rs.";
        }
        return rs;
    }

    protected void printTabularData(String vehType, String vehAmt) {
        int totalNoOfColumns = 2;//no of columns
        int minChars = 4;/*this defines minimum characters per column; since you can
        create multiple rows, this value will define how many columns you want per row */
        boolean doubleWidthColumnExists = true;//this is true if you want a column with double space than other others
        int doubleWidthColRank = 0;//this is the rank of column which will have double space than other columns; starts from 0
        int rightAlignColRank = 3;//this is the rank of column whose content is right aligned; starts from 0

        PlainPrint pp = new PlainPrint(getApplicationContext(), effectivePrintWidth, minChars);
        pp.prepareTabularForm(totalNoOfColumns, doubleWidthColRank, rightAlignColRank, doubleWidthColumnExists);
        //create array list
        ArrayList<String> itemsList = new ArrayList<String>();
        String legalName = "Hardcore Facility And Parking   Service.";
        String tradeName = "";
        String address = " Hopcoms,Lalbagh.";
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy hh:mm a");
        String formattedDate = dateFormat.format(new Date()).toString();
      //  if (Bluetooth.isPrinterConnected(getApplicationContext(), BillScreen.this)) {



            byte[] normalText = {27, 33, 0};
            byte[] boldText = {27, 33, 0};
            boldText[2] = ((byte) (0x8 | normalText[2]));
            pp.startAddingContent4printFields();
            pp.addStarsFullLine();
            pp.addTextCenterAlign(legalName, true);
            pp.addTextCenterAlign(tradeName, true);
            pp.addTextCenterAlign(address, true);
            //	pp.addLeftRightTextJustified("Ph : 8867518994","GSTIN :GDBDHDTEBEBDG12",true);
            pp.addStarsFullLine();
            //	pp.addTextCenterAlign("CASH BILL",true);

            pp.addTextLeftAlign("Ticket # : " + String.valueOf(curTokenNo));
            pp.addTextLeftAlign("Date     : " + formattedDate);

          //  pp.addLeftRightTextJustified("Ticket #:" + String.valueOf(curTokenNo), formattedDate, true);
            //	pp.addLeftRightTextJustified("Cust Name - Vishnu","Cashier Name  - Bala",true);
            mService.sendMessage(pp.getContent4PrintFields(), "GBK");


            mService.sendMessage(pp.getDashesFullLine(), "GBK");

            itemsList.add("Vehicle Type");
            itemsList.add("Amount");
            Log.v("mainact", "bbbeb");
            pp.startAddingContent4printFields();
            pp.addItemContent(itemsList);
            Log.v("mainact", pp.getContent4PrintFields());

            mService.write(boldText);
            mService.sendMessage(pp.getContent4PrintFields(), "GBK");
            mService.sendMessage(pp.getDashesFullLine(), "GBK");

            itemsList.clear();


            itemsList.add(vehType);
            itemsList.add("  "+vehAmt);

            pp.startAddingContent4printFields();
            pp.addItemContent(itemsList);
            mService.write(normalText);


            itemsList.clear();
            mService.sendMessage(pp.getContent4PrintFields(), "GBK");
            pp.startAddingContent4printFields();
            pp.addNewLine();
            pp.addNewLine();
            pp.addNewLine();
            pp.addNewLine();
            mService.sendMessage(pp.getContent4PrintFields(), "GBK");
            pp.startAddingContent4printFields();
            // pp.startAddingContent4printFields();


            //  pp.startAddingContent4printFields();
            //pp.addLeftRightTextJustified("Items  2","Net Amt   65.00",true);

            //mService.sendMessage(pp.getContent4PrintFields()+"\n\n", "GBK");

            //	pp.startAddingContent4printFields();
            //pp.getTextRightAlign("Discount    5.00",true);
            //	mService.sendMessage(pp.getContent4PrintFields()+"\n\n", "GBK");
            //mService.sendMessage(pp.getContent4PrintFields()+"\n\n", "GBK");
            //mService.sendMessage(pp.getDashesFullLine(), "GBK");
            //pp.getDashesFullLine();

            //	pp.startAddingContent4printFields();
            //pp.addDashesFullLine();
            //pp.addLeftRightTextJustified("Total","Rs 60.00",true);
            //	pp.addDashesFullLine();
            //mService.sendMessage(pp.getContent4PrintFields()+"\n\n", "GBK");
            //	mService.sendMessage(pp.getContent4PrintFields()+"\n\n", "GBK");
            //mService.sendMessage(pp.getDashesFullLine(), "GBK");
            //pp.getDashesFullLine();
            //	pp.startAddingContent4printFields();
            //  pp.addTextCenterAlign("Thank you. Visit Again",true);
            //    mService.sendMessage(pp.getContent4PrintFields()+"\n\n", "GBK");
            //	mService.sendMessage(pp.getContent4PrintFields()+"\n\n", "GBK");

            // pp.startAddingContent4printFields();


     //   } else {

            //Printer not connected and send request for connecting printer
         //   Bluetooth.connectPrinter(getApplicationContext(), BillScreen.this);
        //}
    }


    private void printTextData() {
        if (Bluetooth.isPrinterConnected(getApplicationContext(), BillScreen.this)) {
            //Printer connected
            //Send ESC/POS commands for printing data
            byte[] normalText = {27, 33, 0};
            byte[] boldText = {27, 33, 0};
            byte[] normalLeftText = {27, 97, 0};
            byte[] normalCenterText = {27, 97, 1};
            byte[] normalRightText = {27, 97, 2};

            boldText[2] = ((byte) (0x8 | normalText[2]));


            byte[] doubleHeightText = {27, 33, 0};
            doubleHeightText[2] = ((byte) (0x10 | normalText[2]));

            byte[] doubleHeightBoldText = {27, 33, 0};
            doubleHeightBoldText[2] = ((byte) (0x8 | 0x10 | normalText[2]));

            byte[] doubleWidthText = {27, 33, 0};
            doubleWidthText[2] = ((byte) (0x20 | normalText[2]));

            byte[] boldDoubleWidthText = {27, 33, 0};
            boldDoubleWidthText[2] = ((byte) (0x20 | normalText[2]));

            byte[] doubleWidthHeightText = {27, 33, 0};
            doubleWidthHeightText[2] = ((byte) (0x10 | 0x20 | normalText[2]));

            byte[] boldDoubleWidthHeightText = {27, 33, 0};
            boldDoubleWidthHeightText[2] = ((byte) (0x10 | 0x20 | normalText[2]));


            mService = Bluetooth.getServiceInstance();

            //examples for printing data
            String str1 = "This is normal text";
            String str2 = "This is normal bold text";
            String str3 = "This is double width text";
            String str4 = "This is double height text";
            String str5 = "This is center align text";

            mService.write(normalText);
            mService.sendMessage(str1, "GBK");

            mService.write(boldText);


            mService.sendMessage(str2, "GBK");

            mService.write(doubleWidthText);
            mService.sendMessage(str3, "GBK");

            mService.write(doubleHeightText);
            mService.sendMessage(str4, "GBK");

            mService.write(normalCenterText);
            mService.sendMessage(str5, "GBK");
            mService.write(normalLeftText);

        } else {
            //Printer not connected and send request for connecting printer
            Bluetooth.connectPrinter(getApplicationContext(), BillScreen.this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            //bluetooth enabled and request for showing available bluetooth devices
            Bluetooth.pairPrinter(getApplicationContext(), BillScreen.this);
        } else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK) {
            //bluetooth device selected and request pairing with device
            String address = data.getExtras()
                    .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            Bluetooth.pairedPrinterAddress(getApplicationContext(), BillScreen.this, address);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //printTextData();
                      //  printTabularData(descriptionView.getText().toString(), amountView.getText().toString());
                        printFunc();
                    }
                }, 4000);
            }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_tokenscreen, menu);
      /*  MenuItem item = menu.findItem(R.id.bill_report);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(BillingActivity.this);
        if (!sharedPreferences.getBoolean("user_is_admin",false)){
            item.setVisible(false);
        }*/
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {


            case R.id.today_bill_report:
                startActivity(new Intent(BillScreen.this, TokenReport.class));
                break;

            case R.id.ticket_report:
                startActivity(new Intent(BillScreen.this, DetailedTokenReport.class));
                break;

            case R.id.backup_db:
                new BackUpAndRestoreDb(BillScreen.this).exportDB();
                break;

            case R.id.reset_data:
               resetTokenData();

                break;

        }
        return super.onOptionsItemSelected(item);
    }





    void successmsg()
    {

        AlertDialog.Builder builder = new AlertDialog.Builder(BillScreen.this);
        builder.setIcon(R.mipmap.s);
       builder.setTitle("Done");
         builder.setMessage("");
        builder.setCancelable(true);

        final AlertDialog closedialog= builder.create();

        closedialog.show();

        final Timer timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            public void run() {
                closedialog.dismiss();
                timer2.cancel(); //this will cancel the timer of the system
            }
        }, 2000); // the timer will count 5 seconds....

    }




    private void resetTokenData()
    {
        android.support.v7.app.AlertDialog.Builder builder
                = new android.support.v7.app.AlertDialog.Builder(BillScreen.this);
        builder.setMessage("Do you want to reset the data?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mDbHandler.resetData();
                        finish();
                        System.exit(0);

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        android.support.v7.app.AlertDialog alert = builder.create();
        alert.show();
    }




}





























