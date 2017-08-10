package com.example.jkakeno.positiontracker;

import android.os.Parcel;
import android.os.Parcelable;

/*This class records the timestamp, latitude, longitude of a location
and implements parcelable to be able to send location from service to activity using broadcast receiver.*/

public class Position implements Parcelable{
    long mTime;
    double mLatitude;
    double mLongitude;

    public Position(long time, double latitude, double longitude){
        mTime = time;
        mLatitude = latitude;
        mLongitude = longitude;
    }
//Read the data from parcelable object
    public Position(Parcel in) {
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

//Parcelable method but ignored
    @Override
    public int describeContents() {
        return 0;
    }

//Write data to parcelable object
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(mLatitude);
        parcel.writeDouble(mLongitude);
    }

//Constructor required to implement parcelable
    public static final Creator<Position> CREATOR = new Creator<Position>() {
        @Override
        public Position createFromParcel(Parcel in) {
            return new Position(in);
        }

        @Override
        public Position[] newArray(int size) {
            return new Position[size];
        }
    };
}
