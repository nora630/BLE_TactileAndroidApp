package com.onodera.BleApp.template.signal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.onodera.BleApp.R;
import com.onodera.BleApp.template.TemplateService;

import java.util.ArrayList;


public class SendingSignalActivity extends AppCompatActivity {

    private LineChart mChart;

    //private int[] intValue = new int[20];
    //private int index = 0;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra(TemplateService.EXTRA_DEVICE);
            if (TemplateService.BROADCAST_TEMPLATE_MEASUREMENT.equals(action)) {
                byte[] value = intent.getByteArrayExtra(TemplateService.EXTRA_DATA);
                // Update GUI
                //int[] intValue = new int[20];
                int intValue;
                for(int i=0; i<20; i++){
                    intValue = value[i] & 0xFF;
                    //if(i+1==index+20) index += 20;
                    setData(intValue);
                }
                /*
                if(index==20){
                    setData(intValue);
                    index = 0;
                } */
                //setData(intValue);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sending_signal);

        final Toolbar toolbar  = findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mChart = findViewById(R.id.line_chart);
        initChart();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, makeIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }


    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void initChart() {
        // Grid背景色
        mChart.setDrawGridBackground(true);

        // no description text
        mChart.getDescription().setEnabled(true);

        // Grid縦軸を破線
        XAxis xAxis = mChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = mChart.getAxisLeft();
        // Y軸最大最小設定
        leftAxis.setAxisMaximum(250f);
        leftAxis.setAxisMinimum(0f);
        // Grid横軸を破線
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(true);

        // 右側の目盛り
        mChart.getAxisRight().setEnabled(false);

        // add data
        //setData();

        //mChart.animateX(25);
        //mChart.invalidate();

        // dont forget to refresh the drawing
        // mChart.invalidate();
    }

    private void setData(int intValue) {
        // Entry()を使ってLineDataSetに設定できる形に変更してarrayを新しく作成
        /*int data[] = {116, 111, 112, 121, 102, 83,
                99, 101, 74, 105, 120, 112,
                109, 102, 107, 93, 82, 99, 110,
        }; */
        /*
        ArrayList<Entry> values = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            values.add(new Entry(i, intValue[i], null, null));
        } */

        LineDataSet set1;
        //ILineDataSet set1 = mChart.getData().getDataSetByIndex(0);

        if (mChart.getData() != null &&
                mChart.getData().getDataSetCount() > 0) {

            ILineDataSet set2 = mChart.getData().getDataSetByIndex(0);
            //set1 = (LineDataSet) mChart.getData().getDataSetByIndex(0);
            //set1.setValues(values);
            //set1.notifyDataSetChanged();
            mChart.getData().addEntry(new Entry(set2.getEntryCount(), (float)intValue), 0);

            mChart.getData().notifyDataChanged();
            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(800);
            mChart.moveViewToX(mChart.getData().getEntryCount());


        } else {
            //LineData set1 = (LineData) mChart.getData().getDataSetByIndex(0);
            // create a dataset and give it a type
            set1 = new LineDataSet(null, "DataSet");
            //set1.setDrawIcons(false);
            set1.setColor(Color.BLACK);
            //set1.setCircleColor(Color.BLACK);
            //set1.setLineWidth(1f);
            //set1.setCircleRadius(3f);
            //set1.setDrawCircleHole(false);
            //set1.setValueTextSize(0f);
            //set1.setDrawFilled(true);
            //set1.setFormLineWidth(1f);
            //set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            //set1.setFormSize(15.f);

            //set1.setFillColor(Color.BLUE);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            LineData lineData = new LineData(dataSets);

            // set data
            mChart.setData(lineData);
        }
    }



    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TemplateService.BROADCAST_TEMPLATE_MEASUREMENT);
        return intentFilter;
    }
}
