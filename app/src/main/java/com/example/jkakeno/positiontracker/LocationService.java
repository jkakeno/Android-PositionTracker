package com.example.jkakeno.positiontracker;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import java.util.ArrayList;

/*This class gets the current location and sends it to DbHelper to be added to the data base and to the activity to update the UI.*/

public class LocationService  extends Service implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,LocationListener{
//Define a request code to send to Google Play services
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final String TAG = LocationService.class.getSimpleName();
//Broad cast receiver fields
    public static String INTENT_LOCATION_RECEIVED = "location_received";
    public static final String EXTRA_LOCATION = "location";

//Create GoogleApiClient field to access Google Play services.
    private GoogleApiClient mGoogleApiClient;
//Create LocationRequest field to request a quality of service for location updates from the FusedLocationProviderApi.
    private LocationRequest mLocationRequest;
    private IBinder mIBinder = new LocalBinder();
    private DbHelper mDbHelper;
    private Activity mActivity;


    @Override
    public void onCreate() {
        Log.d(TAG,"Service created");
//        super.onCreate();
        mDbHelper = new DbHelper(this);
        mActivity = new Activity();
//Create and Initialize the google API client object and build it to connect to Google Play Service
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

//Create and Initialize the LocationRequest object to receive location updates from the Google API Client
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)    //Request as accurate location as possible
                .setInterval(20 * 1000)        // interval between active location updates every 20 sec
                .setFastestInterval(10 * 1000); // fastest interval at which our app will receive updates every 10sec
        mGoogleApiClient.connect();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"Service started");
        return Service.START_STICKY;
    }

//Service method
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound");
        return mIBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service unbound");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }

//ConnectionCallBack method
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");
//Get the last known location of the user's device
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//If this is the first time Google Play Services is checking location, location might be null.
        if (location == null) {
//Request location updates
            if(hasLocationPermission()) {
                Log.d(TAG, "Has location permissions");
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
        } else {
//If location is not null handle it in this method
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            insertNewPosition(location);
        }
    }

//ConnectionCallBack method
    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Location services suspended. Please reconnect.");
    }

//ConnectionFailedListener method
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(mActivity, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.e(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

//LocationListener method
/*This method gets called every time a new location is detected by Google Play Services.
So as the user is moving around with their phone or tablet, the location APIs are updating the location silently in the background.*/
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG,"Location changed to: " + location);
        insertNewPosition(location);
    }

//Helper method to check for permission
    private boolean hasLocationPermission() {
        Log.d(TAG, "hasLocationPermission");
// if we are below API 23, we already have permission from when the app was installed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "No permissions!");
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

//Helper method to insert new position
    private void insertNewPosition(Location location) {
//NOTE: Location depends if the emulator or device we are testing has a location stored in the last known location from Google Play Services
        long time = location.getTime();
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Log.d(TAG, "Insert Location: " + location.toString());
//Send the position and time stamp to the db
        mDbHelper.insertPosition(time,latitude,longitude);

//Send the position to MapsActivity using broadcast receiver
        Position position = new Position(time,latitude,longitude);
        Intent intent = new Intent(INTENT_LOCATION_RECEIVED);
        intent.putExtra(EXTRA_LOCATION, position);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }



    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    public static class Activity extends AppCompatActivity {
    }

}
