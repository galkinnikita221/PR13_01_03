package com.nik.geolocation_galkin;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "2a412388-eb83-4ac6-b34f-01b0e3509ef6";
    private OkHttpClient client = new OkHttpClient();
    LocationManager _LocationManager;
    int ACCESS_FINE_LOCATION;
    int ACCESS_COARSE_LOCATION;
    EditText editTextDolgota, editTextShirota;
    TextView result;
    TextView result2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        editTextDolgota = findViewById(R.id.dolgota);
        editTextShirota = findViewById(R.id.shirota);
        result = findViewById(R.id.results);
        result2 = findViewById(R.id.results2);
        _LocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }
    LocationListener _LocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (location == null) return;
            else {
                String message = "";
                if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                    message += "\nМестоположение определено с помощью GPS: долгота - " +
                            location.getLatitude() + "широта - " + location.getLongitude();
                }
                if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
                    message += "\nМестоположение определено с помощью интернета: долгота - " +
                            location.getLatitude() + "широта - " + location.getLongitude();
                }
                result.setText(message);
            }
        }
    };

    private void getAddressFromCoordinates(double latitude, double longitude) {
        Log.d("GEOLOCATION", "Getting address for coordinates: " + latitude + ", " + longitude);
        String url = String.format("https://geocode-maps.yandex.ru/1.x/?format=json&apikey=%s&geocode=%f,%f",
                API_KEY, longitude, latitude);

        Log.d("GEOLOCATION", "Request URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.e("GEOLOCATION", "API request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("GEOLOCATION", "Unexpected code " + response);
                    throw new IOException("Unexpected code " + response);
                }

                String responseData = response.body().string();
                Log.d("GEOLOCATION", "Response data: " + responseData);
                try {
                    JSONObject json = new JSONObject(responseData);
                    JSONObject responseObject = json.optJSONObject("response");
                    if (responseObject != null) {
                        JSONObject geoObjectCollection = responseObject.optJSONObject("GeoObjectCollection");
                        if (geoObjectCollection != null) {
                            JSONArray featureMember = geoObjectCollection.optJSONArray("featureMember");
                            if (featureMember != null && featureMember.length() > 0) {
                                String address = featureMember.getJSONObject(0)
                                        .getJSONObject("GeoObject")
                                        .getJSONObject("metaDataProperty")
                                        .getJSONObject("GeocoderMetaData")
                                        .getString("text");

                                runOnUiThread(() -> result2.setText("Адрес: " + address));
                                return;
                            }
                        }
                    }
                    runOnUiThread(() -> result2.setText("Адрес не найден"));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("GEOLOCATION", "Error processing response: " + e.getMessage());
                    runOnUiThread(() -> result2.setText("Ошибка при обработке ответа"));
                }
            }
        });
    }

    public void onGetAddress(View view) {
        if (GetPermissGPS()) {
            Location location = _LocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                getAddressFromCoordinates(location.getLatitude(), location.getLongitude());
            } else {
                result2.setText("Местоположение не определено");
            }
        }
    }

    public boolean GetPermissGPS() {
        ACCESS_FINE_LOCATION = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        ACCESS_COARSE_LOCATION = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        return ACCESS_FINE_LOCATION == PackageManager.PERMISSION_GRANTED || ACCESS_COARSE_LOCATION == PackageManager.PERMISSION_GRANTED;
    }

    public void onGetGPS(View view) {
        if (GetPermissGPS() == false) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (GetPermissGPS() == false) {
            return;
        }
        else {
            _LocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, _LocationListener);
            _LocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10, _LocationListener);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        _LocationManager.removeUpdates(_LocationListener);
    }

    public void onCalculate(View view) {
        String dolgotaStr = editTextDolgota.getText().toString();
        String shirotaStr = editTextShirota.getText().toString();

        Log.d("GEOLOCATION", "Input coordinates: longitude = " + dolgotaStr + ", latitude = " + shirotaStr);

        if (dolgotaStr.isEmpty() || shirotaStr.isEmpty()) {
            result2.setText("Введите координаты");
            return;
        }

        try {
            double dolgota = Double.parseDouble(dolgotaStr);
            double shirota = Double.parseDouble(shirotaStr);

            // Запрос адреса по координатам
            getAddressFromCoordinates(shirota, dolgota);

            // координаты дома
            double homeDolgota = 56.302892;
            double homeShirota = 58.109346;

            Location startPoint = new Location("start");
            startPoint.setLatitude(shirota);
            startPoint.setLongitude(dolgota);

            Location endPoint = new Location("end");
            endPoint.setLatitude(homeShirota);
            endPoint.setLongitude(homeDolgota);

            float distanceInMeters = startPoint.distanceTo(endPoint);
            double distanceInKm = distanceInMeters / 1000;

            double timeInHours = distanceInKm / 6;

            result.setText("Расстояние до дома: " + distanceInKm + " км\n"
                    + "Время в пути: " + timeInHours + " ч");
        } catch (NumberFormatException e) {
            result2.setText("Некорректные координаты");
            Log.e("GEOLOCATION", "Invalid coordinates format: " + e.getMessage());
        }
    }
}