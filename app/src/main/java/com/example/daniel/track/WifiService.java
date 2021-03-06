package com.example.daniel.track;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.provider.SyncStateContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.auth.AuthScope;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HttpContext;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.http.HTTP;

public class WifiService extends Service {

    public static String APurl = "https://api.wigle.net/api/v2/network/detail";
    public static String PythonURL = "http://35.178.107.39/location";
    public Boolean success = false;
    WifiManager wifiMan;
    SyncHttpClient client = new SyncHttpClient();
    SyncHttpClient client2 = new SyncHttpClient();
    public HashMap APhm = new HashMap();
    double userLON = 0;
    double userLAT = 0;
    //Context cont;


    private final IBinder MapsWifiBind = new MyLocalBinder();




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Service" , "Started Service!");
        WifiEXECUTE(this);
        return super.onStartCommand(intent, flags, startId);
    }

    public void WifiEXECUTE(final Context cont){

        Thread t = new Thread() {
            public void run() {
                getWirelessInfo1(cont);
                GetWigleCall();
                tryUserLocation();

                Intent local = new Intent(Intent.ACTION_SEND).putExtra("APS", APhm);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(local);
            }
        };
        t.start();



    }

    @Override
    public IBinder onBind(Intent intent) {

        return MapsWifiBind;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("SERVICE", "Oncreate called");

    }

    public class MyLocalBinder extends Binder{

        WifiService getService(){

            return WifiService.this;
        }

    }

    public void getWirelessInfo1(Context cont){

            String Resp = "";
            Date time = Calendar.getInstance().getTime();
            Context wc = this;

            wifiMan = (WifiManager) wc.getSystemService(wc.WIFI_SERVICE);
            wifiMan.setWifiEnabled(true);
            wifiMan.startScan();

            //Setup comparator to sort wireless results by signal strength.
            Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult lhs, ScanResult rhs) {
                    return (lhs.level > rhs.level ? -1 : (lhs.level == rhs.level ? 0 : 1));
                }
            };

            //Get Wifi scan results from Wifi man
            List<ScanResult> wifiLis = wifiMan.getScanResults();


            //Sort Wifi scan results by comparator.
            Collections.sort(wifiLis, comparator);
            if (wifiLis.isEmpty()) {
                Log.d("WIFI", "No Wireless networks found! " + wifiLis.toString());
            }

            APhm.put("AP1", wifiLis.get(0).SSID);
            APhm.put("AP1TIME", time);
            APhm.put("AP1BSSID", wifiLis.get(0).BSSID.toString());
            APhm.put("AP1LEVEL", wifiLis.get(0).level);
            APhm.put("AP1FREQ", wifiLis.get(0).frequency);
            APhm.put("AP1LON", "");
            APhm.put("AP1LAT", "");
            APhm.put("AP2", wifiLis.get(1).SSID);
            APhm.put("AP2TIME", time);
            APhm.put("AP2BSSID", wifiLis.get(1).BSSID.toString());
            APhm.put("AP2LEVEL", wifiLis.get(1).level);
            APhm.put("AP2FREQ", wifiLis.get(1).frequency);
            APhm.put("AP2LON", "");
            APhm.put("AP2LAT", "");
            APhm.put("AP3", wifiLis.get(2).SSID);
            APhm.put("AP3TIME", time);
            APhm.put("AP3BSSID", wifiLis.get(2).BSSID.toString());
            APhm.put("AP3LEVEL", wifiLis.get(2).level);
            APhm.put("AP3FREQ", wifiLis.get(2).frequency);
            APhm.put("AP3LON", "");
            APhm.put("AP3LAT", "");

            System.out.println("This is the first network :::: " + wifiLis.get(0).SSID.toString());
            System.out.println(APhm.get("AP1BSSID") + " " + APhm.get("AP2BSSID") + " " + APhm.get("AP3BSSID"));
            System.out.println(APhm.get("AP1") + " " + APhm.get("AP2") + " " + APhm.get("AP3"));

            if (APhm.isEmpty()) {
                Resp = "FAIL";
                Log.d("ASYNC", "Method " + Resp);
            } else if (APhm.isEmpty() == false) {
                Resp = "SUCCESS";
                Log.d("ASYNC", "Method " + Resp);
            }
        }
        public void GetWigleCall(){

            int APcount = 0;

            while(APcount <= 3){

                if(APcount == 1){GetWigle(APcount, APhm);}
                else if(APcount == 2){GetWigle(APcount, APhm);}
                else if(APcount == 3){GetWigle(APcount, APhm);}

                APcount+=1;
            }

        }

    public void GetWigle(final int APcount, final HashMap APhm){

        Log.i("GET", "Executing Get Req.");


        RequestParams params = new RequestParams();


        client.setBasicAuth("AID5de8eada7ff8fd72337913007b346e1f", "b9da51d6f0621b4dd95fa517da6a932d", new AuthScope(AuthScope.ANY_REALM, AuthScope.ANY_PORT));

        switch(APcount){
            case 1:
                params.put("netid", APhm.get("AP1BSSID"));
                break;
            case 2:
                params.put("netid", APhm.get("AP2BSSID"));
                break;
            case 3:
                params.put("netid", APhm.get("AP3BSSID"));
                break;
        }

        client.get(APurl, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("Track", "Great Success" + response.toString());

                try {
                    if (response.get("success").toString() == "false") {

                        //Switch to work out which AP Lon Lat to update.

                        switch (APcount){
                            case 1:

                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                        }

                    }
                    if (response.get("success").toString() == "true") {

                        switch (APcount){
                            case 1:
                                Log.d( "CURRENT :::", response.getJSONArray("results").toString());
                                JSONArray jsAP1 = response.getJSONArray("results");
                                System.out.println("This should be the trilat" + jsAP1.getJSONObject(0).getString("trilong").toString());

                                APhm.put("AP1LON", jsAP1.getJSONObject(0).getString("trilong"));
                                APhm.put("AP1LAT", jsAP1.getJSONObject(0).getString("trilat"));

                                Log.d("CASE3 AP3 ",  APhm.get("AP1LON").toString());
                                break;
                            case 2:
                                Log.d( "CURRENT :::", response.getJSONArray("results").toString());
                                JSONArray jsAP2 = response.getJSONArray("results");

                                APhm.put("AP2LON", jsAP2.getJSONObject(0).getString("trilong"));
                                APhm.put("AP2LAT", jsAP2.getJSONObject(0).getString("trilat"));

                                Log.d("CASE2 AP2 ",  APhm.toString());
                                break;
                            case 3:
                                Log.d( "CURRENT :::", response.getJSONArray("results").toString());
                                JSONArray jsAP3 = response.getJSONArray("results");

                                APhm.put("AP3LON", jsAP3.getJSONObject(0).getString("trilong"));
                                APhm.put("AP3LAT", jsAP3.getJSONObject(0).getString("trilat"));

                                Log.d("CASE3 AP3 ",  APhm.toString());
                                break;
                        }


                        if(APcount == 3){        }



                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {
                Log.d("Track", "Fail" + e.toString());

            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                super.onProgress(bytesWritten, totalSize);
            }

        });

    }
    public void tryUserLocation(){




            JSONObject UserReq = new JSONObject();
            StringEntity ReqEnt = null;

        try {


            UserReq.put("AP1" , APhm.get("AP1").toString());
            UserReq.put("AP1lon" , APhm.get("AP1LON").toString());
            UserReq.put("AP1lat", APhm.get("AP1LAT").toString());

            double AP1LEVEL = Double.parseDouble(APhm.get("AP1LEVEL").toString());
            double AP1FREQUENCY = Double.parseDouble(APhm.get("AP1FREQ").toString());

            UserReq.put("AP1dist" , calculateDistance( AP1LEVEL, AP1FREQUENCY));
            UserReq.put("AP2" , APhm.get("AP2"));
            UserReq.put("AP2lon" , APhm.get("AP2LON").toString());
            UserReq.put("AP2lat", APhm.get("AP2LAT").toString());

            double AP2LEVEL = Double.parseDouble(APhm.get("AP2LEVEL").toString());
            double AP2FREQUENCY = Double.parseDouble(APhm.get("AP2FREQ").toString());

            UserReq.put("AP2dist" , calculateDistance(AP2LEVEL, AP2FREQUENCY));

            UserReq.put("AP3" , APhm.get("AP3").toString());
            UserReq.put("AP3lon" , APhm.get("AP3LON").toString());
            UserReq.put("AP3lat", APhm.get("AP3LAT").toString());

            double AP3LEVEL = Double.parseDouble(APhm.get("AP3LEVEL").toString());
            double AP3FREQUENCY = Double.parseDouble(APhm.get("AP3FREQ").toString());

            UserReq.put("AP3dist" , calculateDistance( AP3LEVEL, AP3FREQUENCY));

        } catch (JSONException e) {
            e.printStackTrace();
        }


        Context web = this.getApplicationContext();


        RequestParams jsparams = new RequestParams( "json", UserReq.toString());

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        OkHttpClient ok = new OkHttpClient();

        RequestBody body = RequestBody.create(JSON, UserReq.toString());
        Request req = new Request.Builder().url(PythonURL).post(body).addHeader("content-type", "application/json").build();

        ok.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String UserResponse = response.body().string();

                Log.i("JSON Response " , "Resp " + UserResponse);

//                userLON = (double) response.body().string("lon");
//                userLAT = (double) response.get("lat");
//
//
//
//                APhm.put("userLON", userLON);
//                APhm.put("userLAT", userLAT);
            }
        });




    }

    public double calculateDistance(double signalLevelInDb, double freqInMHz) {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(signalLevelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }
}





