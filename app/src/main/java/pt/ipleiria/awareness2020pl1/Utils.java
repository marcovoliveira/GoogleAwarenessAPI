package pt.ipleiria.awareness2020pl1;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Utils {
    public static void checkFineLocationPermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1
            );
        }
        try {
            int locationMode = Settings.Secure.getInt(activity.getContentResolver(),
                    Settings.Secure.LOCATION_MODE);
            if (locationMode != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
                Toast.makeText(activity,
                        "Error: high accuracy location mode must be enabled in the device.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Settings.SettingNotFoundException e) {
            Toast.makeText(activity, "Error: could not access location mode.",
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }
    }
}
