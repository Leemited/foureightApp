package com.foureight;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/**
 * Created by Pc on 2015-06-26.
 */
public class LocationImpl implements LocationListener {
    @Override
    public void onLocationChanged(Location location) {
        Double lat = location.getLatitude();
        Double lng = location.getLongitude();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
