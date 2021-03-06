/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hoho.android.usbserial.examples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.*;

/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialConsoleActivity extends Activity {

    private final String TAG = SerialConsoleActivity.class.getSimpleName();

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialPort)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialPort sPort = null;


    private static String url = "https://script.googleusercontent.com/macros/echo?user_content_key=Mz9oMnfXcasnEQL1cZmGnsf7EodLxb7rs2_wOpNsve8KOkuRIq5nSW0ydIWnzsFudpvwXUGAMgFNn7B9FKHPcVgGZJWfRPGrOJmA1Yb3SEsKFZqtv3DaNYcMrmhZHmUMWojr9NvTBuBLhyHCd5hHa1GhPSVukpSQTydEwAEXFXgt_wltjJcH3XHUaaPC1fv5o9XyvOto09QuWI89K6KjOu0SP2F-BdwU4K-6ACMkzAYXyqQugWa-JSsSVo_j15p99MDBn26Q6TrN8uYpFm42KU8tj7zq3GK55y7FLqOV0Tk27B8Rh4QJTQ&lib=MnrE7b2I2PjfH799VodkCPiQjIVyBAxva";


    private TextView mTitleTextView;
    private TextView consoleText;
    private ScrollView mScrollView;

    private TextView weightTextView;
    private TextView foodTextView;
    private TextView caloriesTextView;
    private TextView servingTextView;

    private EditText barcodeEditText;

    private TextView totalCaloriesTextView;
    private Double totalCalories = 0.0;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    AsyncTask<String, String, String> request;


    private SerialInputOutputManager mSerialIoManager;
    String message="";
    String barcode="";
    JSONArray jsonArray;
    JSONObject currentItem = null;
    Float currentWeight= new Float(0.0);
    Float weightZeroOffset = new Float(0.0);


    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SerialConsoleActivity.this.updateReceivedData(data);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_console);
        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);

        consoleText = (TextView) findViewById(R.id.consolelog);
        weightTextView = (TextView) findViewById(R.id.weight);
        foodTextView = (TextView) findViewById(R.id.foodname);
        caloriesTextView = (TextView) findViewById(R.id.calories);
        servingTextView = (TextView) findViewById(R.id.serving);

        totalCaloriesTextView = (TextView) findViewById(R.id.total);
        barcodeEditText = (EditText) findViewById(R.id.barcode);

        //Request the Json
        new RequestTask().execute(url);

        Button button = (Button) findViewById(R.id.tare);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                writeMessage("Tare pressed");
                tare();
            }
        });

        Button addToTotalButton = (Button) findViewById(R.id.addToTotal);
        addToTotalButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Float weight = currentWeight - weightZeroOffset;
                    Double caloriesPerHundred = currentItem.getDouble("calories");
                    Double calories = caloriesPerHundred / 100 * weight;
                    totalCalories = totalCalories + calories;
                    totalCaloriesTextView.setText(String.format("Total %.0f Cal", totalCalories));
                    tare();
                } catch (Exception e) {

                }
            }
        });

        barcodeEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return true;
                }

                char unicodeChar = (char) event.getUnicodeChar();

                // A digit was typed (thats good :) )
                if (Character.isDigit(unicodeChar)) {
                    writeMessage("Writing digit" + unicodeChar);
                    barcode += unicodeChar;
                    return false;
                }
                // Anything else except the return key
                if (keyCode != 31) {
                    writeMessage("Random character");
                    return true;
                }

                String fullBarcode = barcode;
                // clear the barcode now
                barcode = "";
                barcodeEditText.setText("");

                writeMessage("Barcode: " + fullBarcode);
                currentItem = getNutrition(fullBarcode); //getNutrition if barcode not found, this function returns jsonObject with name: Product not found
                if (currentItem == null) {
                    foodTextView.setText("Product not recognized");
                    return false;
                }
                try {
                    String name = currentItem.getString("name");
                    foodTextView.setText(name);
                }catch (Exception e){
                }

                return true;
            }
        });
    }




    //This function to clean up bar code, make sure it is only contains digits
    public String cleanNumber(String value) {
        String str = value.replaceAll("[^A-Za-z0-9 ]", "");
        str=str.replaceAll("ONE", "1");
        return str;
    }

    private JSONObject getNutrition(String matchBarcode) {
        JSONObject nulljson = null;
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject json;
            try {
                json = jsonArray.getJSONObject(i);
                String barcode;
                barcode = json.getString("barcode");
                String toLookForBarcode=cleanNumber(matchBarcode);
                if (barcode.equals(toLookForBarcode)) {
                    this.barcode="";
                    return json;
                }
            } catch (JSONException e) {
                consoleText.append(e.toString()+"\n");
            }
        }


        try {
            nulljson = new JSONObject();
            nulljson.put("name", "Product not recorgnized");
            nulljson.put("barcode", "null");
            nulljson.put("calories", 0);
            nulljson.put("servingSize", 0);
        }catch (Exception e){
            consoleText.append(e.toString()+"\n");
        }
        return nulljson;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
            mTitleTextView.setText("No serial device.");
        } else {
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                mTitleTextView.setText("Opening device failed");
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_2, UsbSerialPort.PARITY_NONE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                mTitleTextView.setText("Error opening device: " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            mTitleTextView.setText("Serial device: " + sPort.getClass().getSimpleName());
        }
        onDeviceStateChange();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    public void writeMessage(String message) {
        consoleText.append(message + "\n");
        mScrollView.smoothScrollTo(0, consoleText.getBottom());
    }

    public void tare() {
        writeMessage("Weight zeroed");
        weightZeroOffset = currentWeight;
        updateWeightDisplay();
    }

    private void updateReceivedData(byte[] data) {
        for (int i=0; i<data.length;i++) {
            if (data[i] == '\n') {
//                writeMessage("Real reading " + message);
                mScrollView.smoothScrollTo(0, consoleText.getBottom());

                try {
                    currentWeight = Float.parseFloat(message);
                    message = "";
                    updateWeightDisplay();
                } catch (Exception e) {
                    message = "";
                    consoleText.append(e.toString()+ "\n");
                }
            } else {
                message += (char) data[i];
            }
        }
    }

    private Float updateWeightDisplay() {
        Float zeroedWeight = currentWeight - weightZeroOffset;
        weightTextView.setText(String.format("%.0f", (zeroedWeight)) + "g" + "\n");
        updateNutrition(zeroedWeight);
        return zeroedWeight;
    }

    private void updateNutrition(Float weight) {
        if (weight < 1 || currentItem == null) {
            caloriesTextView.setText("0 Cal");
            servingTextView.setText("0 Servings");
            return;
        }

        try {
            Double calories = currentItem.getDouble("calories");
            Double servingSize = currentItem.getDouble("servingSize");
            consoleText.append("Calories:" + calories + " Cal\n");
            consoleText.append("Serving:" + servingSize + " Servings\n");
            mScrollView.smoothScrollTo(0, consoleText.getBottom());

            caloriesTextView.setText(String.format("%.0f Cal", (calories / 100 * weight)));
            servingTextView.setText(String.format("%.2f Servings", (weight / servingSize)));
        } catch (Exception e) {

        }
    }

    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     */
    static void show(Context context, UsbSerialPort port) {
        sPort = port;
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    class RequestTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                response = httpclient.execute(new HttpGet(uri[0]));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    responseString = out.toString();
                    out.close();
                } else{
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                //TODO Handle problems..
            } catch (IOException e) {
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        public void onPostExecute(String result) {
            super.onPostExecute(result);

            if(result!=null){
                try {
                    JSONObject jObjec= new JSONObject(result);
                    jsonArray = jObjec.getJSONArray("Sheet1");
                    consoleText.append(jsonArray.toString() + "\n");
                }catch (Exception e){
                    consoleText.append(e.toString()+"\n");
                }
            }
        }
    }


}