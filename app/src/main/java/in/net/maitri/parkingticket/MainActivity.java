package in.net.maitri.parkingticket;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import in.net.maitri.parkingticket.billScreen.BillScreen;

public class MainActivity extends AppCompatActivity {

    private DbHandler mDbHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDbHandler = new DbHandler(MainActivity.this);
        int dateCount = mDbHandler.getDateCount(dbDate());
        int sysDate = Integer.parseInt(mDbHandler.getSysValue("sys_current_date"));
        Log.d("dateCount", String.valueOf(dateCount));
        Log.d("sysDate", String.valueOf(sysDate));
        if (dateCount == sysDate){
            startActivity(new Intent(MainActivity.this, BillScreen.class));
        } else{
            if (mDbHandler.updateSysValue("sys_current_date", String.valueOf(dateCount))){

                TicketMst tm = new TicketMst();
                tm.setTktDate(String.valueOf(dateCount));
                tm.setTktVehicle1(0);
                tm.setTktVehicle2(0);
                if (mDbHandler.addTicketMst(tm)){
                    mDbHandler.updateBillNo(1);
                    startActivity(new Intent(MainActivity.this, BillScreen.class));
                }
            } else{

            }
        }
    }

    private String dbDate(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return formatter.format(date);
    }

}
