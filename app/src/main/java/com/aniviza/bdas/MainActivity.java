package com.aniviza.bdas;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private TextView bdasDigitalClock, standardDigitalClock, standardClockSunrise, standardClockSunset, bdasClockSunrise, bdasClockSunset;
    private Handler handler;
    private double latitude = 0.0; // Default value in case location is not fetched
    private BDASClockWidget standardAnalogClock;
    private BDASClockWidget bdasAnalogClock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        standardAnalogClock = findViewById(R.id.standardAnalogClock);
        bdasAnalogClock = findViewById(R.id.bdasAnalogClock);
        bdasDigitalClock = findViewById(R.id.bdasDigitalClock);
        standardDigitalClock = findViewById(R.id.standardDigitalClock);
        standardClockSunrise = findViewById(R.id.standardClockSunrise);
        standardClockSunset = findViewById(R.id.standardClockSunset);
        bdasClockSunrise = findViewById(R.id.bdasClockSunrise);
        bdasClockSunset = findViewById(R.id.bdasClockSunset);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        handler = new Handler(Looper.getMainLooper());
        fetchLocationAndStartUpdates();
    }

    private void fetchLocationAndStartUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    latitude = location.getLatitude();
                }
                startClockUpdates();
            }
        });
    }

    private void startClockUpdates() {
        handler.post(updateClocksRunnable);
    }

    private final Runnable updateClocksRunnable = new Runnable() {
        @Override
        public void run() {
            // Update Standard (Local) Clock
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            LocalTime standardTime = now.toLocalTime();
            boolean isDST = now.getZone().getRules().isDaylightSavings(now.toInstant());
            String dstIndicator = isDST ? " (DST)" : "";

            // Format standard time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
            String formattedStandardTime = standardTime.format(formatter) + dstIndicator;

            standardDigitalClock.setText(formattedStandardTime + " \nTimezone: " + now.getZone().getId());
            standardClockSunrise.setText("Sunrise: " + calculateSunrise(latitude, LocalDate.now().getDayOfYear(), standardTime));
            standardClockSunset.setText("Sunset: " + calculateSunset(latitude, LocalDate.now().getDayOfYear(), standardTime));
            standardAnalogClock.setTime(standardTime); // Update standard analog clock

            // Update BDAS Clock
            String band = determineBand(latitude);
            double dilationFactor = calculateBDASDilationFactor(band);
            long bdasAdjustment = calculateBDASAdjustment(band);
            LocalTime bdasTime = standardTime.plusSeconds(bdasAdjustment);

            // Format BDAS time
            String formattedBDASTime = bdasTime.format(formatter);
            bdasDigitalClock.setText(formattedBDASTime + " \nLatitude Band: " + band);
            bdasClockSunrise.setText("Sunrise: " + calculateBDASSunrise(latitude, LocalDate.now().getDayOfYear()));
            bdasClockSunset.setText("Sunset: " + calculateBDASSunset(latitude, LocalDate.now().getDayOfYear()));
            bdasAnalogClock.setDilationFactor(dilationFactor);
            bdasAnalogClock.setTime(bdasTime); // Update BDAS analog clock

            // Re-run every second
            handler.postDelayed(this, 1000);
        }
    };

    private String determineBand(double latitude) {
        if (latitude >= 30.0 && latitude < 35.0) {
            return "SOUTHERN";
        } else if (latitude >= 35.0 && latitude < 40.0) {
            return "MID";
        } else {
            return "NORTHERN";
        }
    }

    private String calculateSunrise(double latitude, int dayOfYear, LocalTime baseTime) {
        // Calculate declination
        double axialTilt = 23.44; // Earth's axial tilt in degrees
        double declination = axialTilt * Math.sin(2 * Math.PI * (dayOfYear - 81) / 365.25);

        // Approximate hour angle of sunrise based on declination and latitude
        double latitudeRadians = Math.toRadians(latitude);
        double declinationRadians = Math.toRadians(declination);
        double hourAngle = Math.acos(-Math.tan(latitudeRadians) * Math.tan(declinationRadians));

        // Adjust for standard sunrise
        double sunriseTime = 12 - (hourAngle * 12 / Math.PI);
        int sunriseHour = (int) sunriseTime;
        int sunriseMinute = (int) ((sunriseTime - sunriseHour) * 60);

        return String.format("%02d:%02d AM", sunriseHour, sunriseMinute);
    }

    private String calculateSunset(double latitude, int dayOfYear, LocalTime baseTime) {
        // Calculate declination
        double axialTilt = 23.44; // Earth's axial tilt in degrees
        double declination = axialTilt * Math.sin(2 * Math.PI * (dayOfYear - 81) / 365.25);

        // Approximate hour angle of sunset based on declination and latitude
        double latitudeRadians = Math.toRadians(latitude);
        double declinationRadians = Math.toRadians(declination);
        double hourAngle = Math.acos(-Math.tan(latitudeRadians) * Math.tan(declinationRadians));

        // Adjust for standard sunset
        double sunsetTime = 12 + (hourAngle * 12 / Math.PI);
        int sunsetHour = (int) sunsetTime;
        int sunsetMinute = (int) ((sunsetTime - sunsetHour) * 60);

        return String.format("%02d:%02d %s", sunsetHour > 12 ? sunsetHour - 12 : sunsetHour, sunsetMinute, sunsetHour >= 12 ? "PM" : "AM");
    }

    private String calculateBDASSunrise(double latitude, int dayOfYear) {
        double sunriseHour;
        switch (determineBand(latitude)) {
            case "SOUTHERN":
                sunriseHour = 6.5; // 6:30 AM
                break;
            case "MID":
                sunriseHour = 7.0; // 7:00 AM
                break;
            default: // NORTHERN
                sunriseHour = 7.5; // 7:30 AM
                break;
        }
        int hour = (int) sunriseHour;
        int minute = (int) ((sunriseHour - hour) * 60);
        return String.format("%02d:%02d AM", hour, minute);
    }

    private String calculateBDASSunset(double latitude, int dayOfYear) {
        double sunsetHour;
        switch (determineBand(latitude)) {
            case "SOUTHERN":
                sunsetHour = 18.0; // 6:00 PM
                break;
            case "MID":
                sunsetHour = 18.5; // 6:30 PM
                break;
            default: // NORTHERN
                sunsetHour = 19.0; // 7:00 PM
                break;
        }
        int hour = (int) sunsetHour;
        int minute = (int) ((sunsetHour - hour) * 60);
        return String.format("%02d:%02d %s", hour > 12 ? hour - 12 : hour, minute, hour >= 12 ? "PM" : "AM");
    }

    private double calculateBDASDilationFactor(String band) {
        int dayOfYear = LocalDate.now().getDayOfYear();

        // Approximate average daylight duration and shift for each band
        int averageDaylightSeconds;
        int maxDaylightDeviationSeconds;

        switch (band) {
            case "SOUTHERN":
                averageDaylightSeconds = 43200; // 12 hours in seconds
                maxDaylightDeviationSeconds = 7200; // ±2 hours
                break;
            case "MID":
                averageDaylightSeconds = 43200; // 12 hours in seconds
                maxDaylightDeviationSeconds = 8100; // ±2.25 hours
                break;
            default: // NORTHERN
                averageDaylightSeconds = 43200; // 12 hours in seconds
                maxDaylightDeviationSeconds = 9000; // ±2.5 hours
                break;
        }

        // Calculate seasonal factor for daylight duration deviation
        double seasonalFactor = Math.sin(2 * Math.PI * (dayOfYear - 81) / 365.25);

        // Adjust daylight duration based on the seasonal factor
        int daylightDuration = (int) (averageDaylightSeconds + (maxDaylightDeviationSeconds * seasonalFactor));

        // Calculate dilation factor
        return (double) daylightDuration / 86400; // Compare BDAS day length to standard 86400 seconds
    }

    private long calculateBDASAdjustment(String band) {
        int dayOfYear = LocalDate.now().getDayOfYear();

        // Approximate average daylight duration and shift for each band
        int averageDaylightSeconds;
        int maxDaylightDeviationSeconds;

        switch (band) {
            case "SOUTHERN":
                averageDaylightSeconds = 43200; // 12 hours in seconds
                maxDaylightDeviationSeconds = 7200; // ±2 hours
                break;
            case "MID":
                averageDaylightSeconds = 43200; // 12 hours in seconds
                maxDaylightDeviationSeconds = 8100; // ±2.25 hours
                break;
            default: // NORTHERN
                averageDaylightSeconds = 43200; // 12 hours in seconds
                maxDaylightDeviationSeconds = 9000; // ±2.5 hours
                break;
        }

        // Calculate seasonal factor for daylight duration deviation
        double seasonalFactor = Math.sin(2 * Math.PI * (dayOfYear - 81) / 365.25);

        // Adjust daylight duration based on the seasonal factor
        int daylightDuration = (int) (averageDaylightSeconds + (maxDaylightDeviationSeconds * seasonalFactor));

        // Find the midpoint of daylight hours
        int daylightMidpoint = daylightDuration / 2;

        // Calculate adjustment to align BDAS time
        return daylightMidpoint - averageDaylightSeconds / 2; // Shift to maintain consistent midpoint
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndStartUpdates();
        } else {
            standardDigitalClock.setText("Permission denied");
            bdasDigitalClock.setText("Permission denied");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Clean up the handler to prevent memory leaks
    }
}
