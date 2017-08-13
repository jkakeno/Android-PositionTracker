package com.example.jkakeno.positiontracker;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/*This class handles the UI and starts a service for the app to do work in the background.
This class gets location updates from the service and adds markers on the map.
Also it provides a date picker dialog to set a date range to display markers on the map.*/

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = MapsActivity.class.getSimpleName();
    //Shared preference keys and table name for period formated dates
    public static final String PREFS_NAME = "MyPrefs";
    public static final String KEY_MINDATE = "MinDate";
    public static final String KEY_MAXDATE = "MaxDate";
    //Shared preference keys and table name for period long dates
    public static final String PREFS_PERIOD_LONG = "MyPrefsLong";
    public static final String KEY_MINDATE_LONG = "MinDateLong";
    public static final String KEY_MAXDATE_LONG = "MaxDateLong";
    private static final int PERMISSION_FINE_LOCATION = 100;
    private static final int PERMISSION_REQUEST_CODE = 11;
    private static ArrayList<Position> mPositions;

    private GoogleMap mMap;
    private LocationService mLocationService;
    public DatePickerDialog mDatePickerDialog;
    //Broad cast receiver object
    private BroadcastReceiver mBroadcastReceiver;
    private DbHelper mDbHelper;

    private Button mSettingButton;
    private Button mPeriodButton;
    private EditText mMinDate;
    private EditText mMaxDate;

    private int mYear;
    private int mMonth;
    private int mDay;
    private long mDateLongMin;
    private long mDateLongMax;

    private String mDateFormattedMin;
    private String mDateFormattedMax;

    private boolean mBound = false;

    //Formatted dates shared preference fields
    private SharedPreferences mPrefPeriod;
    private SharedPreferences.Editor mEditor;
    //Long dates shared preference fields
    private SharedPreferences mPrefPeriodLong;
    private SharedPreferences.Editor mEditorLong;

    //Create a service connection field so we can use it on onStart() and onStop().
//NOTE: We created an anonymous class so we don't have to create a separate class just for Service Connection.
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service connected");
            mBound = true;
            LocationService.LocalBinder localBinder = (LocationService.LocalBinder) iBinder;
            mLocationService = localBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "Service disconnected");
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Log.d(TAG, "onCreate");

//Get all positions in db and store it in member variable
        mDbHelper = new DbHelper(this);
        mPositions = mDbHelper.getPositionList();
//Initialize shared preference and editor for formatted dates
        mPrefPeriod = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mEditor = mPrefPeriod.edit();
//Initialize shared preference and editor for long dates
        mPrefPeriodLong = getSharedPreferences(PREFS_PERIOD_LONG, Context.MODE_PRIVATE);
        mEditorLong = mPrefPeriodLong.edit();
//Create calender object to set member variables year, month, day
        final Calendar calendar = Calendar.getInstance();
        mYear = calendar.get(Calendar.YEAR); // current year
        mMonth = calendar.get(Calendar.MONTH); // current month
        mDay = calendar.get(Calendar.DAY_OF_MONTH); // current day
//Set bound to false so that the service can be started
        mBound = false;
//Start Location Service
        Intent intent = new Intent(MapsActivity.this, LocationService.class);
        startService(intent);
//Get the position from LocationService using broadcast receiver
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Broadcast receiver initialized");
                Position position = intent.getParcelableExtra(LocationService.NEW_POSITION);
                addNewPosition(position);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(LocationService.POSITION_INTENT));

        mPeriodButton = (Button) findViewById(R.id.period);
//Period button function
        mPeriodButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//Create period dialog
                final Dialog dialog = new Dialog(MapsActivity.this);
                dialog.setContentView(R.layout.period_dialog);
//Pick period min date
                mMinDate = (EditText) dialog.findViewById(R.id.min_date);
                mMinDate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mDatePickerDialog = new DatePickerDialog(MapsActivity.this, new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
//Set year, month, day member variables
                                calendar.set(Calendar.YEAR, datePicker.getYear());
                                calendar.set(Calendar.MONTH, datePicker.getMonth());
                                calendar.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
//Convert date to dateLong in milliseconds since 1/1/1970.
                                mDateLongMin = calendar.getTimeInMillis();
//Store long dates in their respective shared preference keys
                                mEditorLong.putLong(KEY_MINDATE_LONG, mDateLongMin);
                                mEditorLong.apply();
//Format dateLong to string representation of date
                                mDateFormattedMin = new SimpleDateFormat("yyyy/MM/dd").format(new Date(mDateLongMin));
//Store the formatted date in the respective shared preference key
                                mEditor.putString(KEY_MINDATE, mDateFormattedMin);
                                mEditor.apply();
//Set dateFormatted to date picker dialog dateEditText
                                mMinDate.setText(mDateFormattedMin);
                            }
                        }, mYear, mMonth, mDay);
                        mDatePickerDialog.show();
                    }
                });
//Set shared preference stored value to min date edit text
                mMinDate.setText(mPrefPeriod.getString(KEY_MINDATE, null));
//Pick period max date
                mMaxDate = (EditText) dialog.findViewById(R.id.max_date);
                mMaxDate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mDatePickerDialog = new DatePickerDialog(MapsActivity.this, new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
//Set year, month, day member variables
                                calendar.set(Calendar.YEAR, datePicker.getYear());
                                calendar.set(Calendar.MONTH, datePicker.getMonth());
                                calendar.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
//Convert date to dateLong in milliseconds since 1/1/1970.
                                mDateLongMax = calendar.getTimeInMillis();
//Store the formatted date in the respective shared preference key
                                mEditorLong.putLong(KEY_MAXDATE_LONG, mDateLongMax);
                                mEditorLong.apply();
//Format dateLong to string representation of date
                                mDateFormattedMax = new SimpleDateFormat("yyyy/MM/dd").format(new Date(mDateLongMax));
//Store the formatted date in the respective shared preference key
                                mEditor.putString(KEY_MAXDATE, mDateFormattedMax);
                                mEditor.apply();
//Set dateFormatted to date picker dialog dateEditText
                                mMaxDate.setText(mDateFormattedMax);
                            }
                        }, mYear, mMonth, mDay);
                        mDatePickerDialog.show();
                    }
                });
//Set shared preference stored value to max date edit text
                mMaxDate.setText(mPrefPeriod.getString(KEY_MAXDATE, null));
//Dialog Cancel button function
                Button cancel = (Button) dialog.findViewById(R.id.cancel);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
//Exit dialog
                        dialog.cancel();
                    }
                });
//Dialog Set button function
                Button set = (Button) dialog.findViewById(R.id.set);
                set.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        addMarkers();
                        dialog.dismiss();
                    }
                });
//Show date picker dialog
                dialog.show();
            }
        });

// Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
//        checkLocationPermission();

//NOTE: Workshop on how to handle permissions https://teamtreehouse.com/library/handling-marshmallow
//NOTE: Although this logic creates the service only after permission has been granted by the user BUT still need to check for permission in service because FusedLocationApi.requestLocationUpdates request permission
// if the SDK is below marshmallow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//if the user did not allow ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permission
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//if the user has denied permission then explain to the user why the app needs permission to location by displaying it in a dialog on positive button request permission and on negative button toast a message
                if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                    new AlertDialog.Builder(MapsActivity.this)
                            .setTitle("Location Permission")
                            .setMessage("Hi there! The app can't track you without the location permission, could you please grant it?")
                            .setPositiveButton("Yep", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
                                }
                            })
                            .setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Toast.makeText(MapsActivity.this,"The app won't be able to track you...",Toast.LENGTH_SHORT).show();
                                }
                            }).show();
//if the user did not allow ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permission then request permission
                } else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
                }
//if the user has granted permission create the service
            } else {
                createService();
            }
//if the SDK is bellow marshmallow create the service
        }else {
                createService();
        }
    }

//This method is to handle the permission result described in workshop
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==PERMISSION_REQUEST_CODE){
            if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
//Notify the user of important message after permission has been granted
                new AlertDialog.Builder(MapsActivity.this)
                        .setTitle("IMPORTANT!!!")
                        .setMessage("Please close and reopen the app for permission changes to take effect.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).show();
//Create service after permission has been granted
                createService();
            }
        }
    }

    private void createService() {
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }

    /* Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app. */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        mMap = googleMap;
        addMarkers();
    }

    public void addNewPosition(Position position) {
        Log.d(TAG, "addNewPosition");
        mPositions.add(position);
        addMarker(position);
        LatLng latLng = new LatLng(position.getLatitude(), position.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    public void addMarkers() {
        Log.d(TAG, "addMarkers");
        mMap.clear();
        for (Position position : mPositions) {
            Long positionTime = position.getTime();
            long minDate = mPrefPeriodLong.getLong(KEY_MINDATE_LONG, mDateLongMin);
            long maxDate = mPrefPeriodLong.getLong(KEY_MAXDATE_LONG, mDateLongMax);
            if (positionTime.compareTo(minDate) > 0 && positionTime.compareTo(maxDate) < 0) {
                addMarker(position);
            }
        }
    }

    public void addMarker(Position position) {
        Log.d(TAG, "addMarker");
        LatLng latLng = new LatLng(position.getLatitude(), position.getLongitude());
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("I am here!");
        mMap.addMarker(options);
    }


    public void checkLocationPermission() {
        Log.d(TAG, "checkLocationPermission");
// if the SDK is below marshmallow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//if the user did not allow ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permission then request permission
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Ask for permissions!");
//Activity class method which takes a permission and a permission code
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }
        }
    }
}
