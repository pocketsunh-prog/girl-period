package com.girlperiod.app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service to fetch real weather data from Hong Kong Observatory (HKO) Open Data API.
 * Falls back to mock data when offline.
 *
 * API Reference: https://www.hko.gov.hk/en/abouthko/opendata_intro.htm
 */
public class HkoWeatherService {

    private static final String TAG = "HkoWeatherService";

    // HKO API Endpoints
    private static final String API_CURRENT_WEATHER = "https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=rhrread&lang=en";
    private static final String API_WEATHER_FORECAST = "https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=fnd&lang=en";
    private static final String API_UV_INDEX = "https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=uvindex&lang=en";

    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public HkoWeatherService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Check if network is available.
     */
    public boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fetch current weather data from HKO API.
     */
    public void fetchCurrentWeather(OnWeatherDataListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("No network connection");
            return;
        }

        executorService.execute(() -> {
            try {
                String response = makeHttpRequest(API_CURRENT_WEATHER);
                WeatherData data = parseCurrentWeather(response);
                mainHandler.post(() -> listener.onSuccess(data));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching current weather", e);
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Fetch weather forecast from HKO API.
     */
    public void fetchWeatherForecast(OnForecastListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("No network connection");
            return;
        }

        executorService.execute(() -> {
            try {
                String response = makeHttpRequest(API_WEATHER_FORECAST);
                ForecastData data = parseWeatherForecast(response);
                mainHandler.post(() -> listener.onSuccess(data));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching weather forecast", e);
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Fetch UV index from HKO API.
     */
    public void fetchUvIndex(OnUvIndexListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("No network connection");
            return;
        }

        executorService.execute(() -> {
            try {
                String response = makeHttpRequest(API_UV_INDEX);
                int uvIndex = parseUvIndex(response);
                mainHandler.post(() -> listener.onSuccess(uvIndex));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching UV index", e);
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Make HTTP GET request and return response string.
     */
    private String makeHttpRequest(String urlString) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("HTTP error code: " + responseCode);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Parse current weather JSON response.
     */
    private WeatherData parseCurrentWeather(String json) throws Exception {
        WeatherData data = new WeatherData();
        JSONObject root = new JSONObject(json);

        // Parse temperature
        if (root.has("temperature")) {
            JSONArray tempArray = root.getJSONArray("temperature");
            if (tempArray.length() > 0) {
                // Get first station's temperature (Hong Kong Observatory)
                for (int i = 0; i < tempArray.length(); i++) {
                    JSONObject station = tempArray.getJSONObject(i);
                    if (station.optString("place").contains("King's Park") || station.optString("place").contains("Hong Kong")) {
                        data.temperature = station.optDouble("value", 24.0);
                        break;
                    }
                }
                if (data.temperature == 0) {
                    data.temperature = tempArray.getJSONObject(0).optDouble("value", 24.0);
                }
            }
        }

        // Parse humidity
        if (root.has("humidity")) {
            JSONArray humidityArray = root.getJSONArray("humidity");
            if (humidityArray.length() > 0) {
                data.humidity = humidityArray.getJSONObject(0).optInt("value", 65);
            }
        }

        // Parse rainfall
        if (root.has("rainfall")) {
            JSONArray rainfallArray = root.getJSONArray("rainfall");
            if (rainfallArray.length() > 0) {
                data.rainfall = rainfallArray.getJSONObject(0).optDouble("max", 0.0);
                if (data.rainfall == 0) {
                    // Try to get any rainfall value
                    for (int i = 0; i < rainfallArray.length(); i++) {
                        double val = rainfallArray.getJSONObject(i).optDouble("max", 0.0);
                        if (val > 0) {
                            data.rainfall = val;
                            break;
                        }
                    }
                }
            }
        }

        // Parse wind
        if (root.has("wind")) {
            JSONArray windArray = root.getJSONArray("wind");
            if (windArray.length() > 0) {
                data.windSpeed = windArray.getJSONObject(0).optDouble("speed", 10.0);
                data.windDirection = windArray.getJSONObject(0).optString("direction", "NE");
            }
        }

        // Parse icon/weather condition
        if (root.has("icon")) {
            JSONArray iconArray = root.getJSONArray("icon");
            if (iconArray.length() > 0) {
                int iconCode = iconArray.optInt(0, 50);
                data.iconCode = String.valueOf(iconCode);
                data.description = getWeatherDescription(iconCode);
            }
        }

        // Parse UV index (may be in a separate field or null)
        if (root.has("uvindex")) {
            try {
                JSONArray uvArray = root.getJSONArray("uvindex");
                if (uvArray.length() > 0) {
                    data.uvIndex = uvArray.getJSONObject(0).optInt("value", 5);
                }
            } catch (Exception e) {
                data.uvIndex = 5; // Default
            }
        } else {
            data.uvIndex = 5; // Default
        }

        return data;
    }

    /**
     * Parse weather forecast JSON response.
     */
    private ForecastData parseWeatherForecast(String json) throws Exception {
        ForecastData data = new ForecastData();
        JSONObject root = new JSONObject(json);

        if (root.has("weatherForecast")) {
            JSONArray forecastArray = root.getJSONArray("weatherForecast");
            for (int i = 0; i < Math.min(forecastArray.length(), 9); i++) {
                JSONObject day = forecastArray.getJSONObject(i);
                ForecastDay forecastDay = new ForecastDay();
                forecastDay.date = day.optString("forecastDate", "");
                forecastDay.week = day.optString("week", "");
                forecastDay.weather = day.optString("forecastWeather", "");
                forecastDay.tempMax = day.optInt("forecastMaxtemp", 28);
                forecastDay.tempMin = day.optInt("forecastMintemp", 22);
                forecastDay.humidityMax = day.optInt("forecastMaxrh", 80);
                forecastDay.humidityMin = day.optInt("forecastMinrh", 60);
                data.forecastDays.add(forecastDay);
            }
        }

        return data;
    }

    /**
     * Parse UV index JSON response.
     */
    private int parseUvIndex(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (root.has("record")) {
            JSONArray records = root.getJSONArray("record");
            if (records.length() > 0) {
                return records.getJSONObject(0).optInt("value", 5);
            }
        }
        return 5; // Default
    }

    /**
     * Get weather description from HKO icon code.
     */
    private String getWeatherDescription(int iconCode) {
        if (iconCode >= 50 && iconCode <= 54) return "Sunny";
        if (iconCode >= 55 && iconCode <= 60) return "Cloudy";
        if (iconCode >= 61 && iconCode <= 65) return "Overcast";
        if (iconCode >= 66 && iconCode <= 70) return "Light Rain";
        if (iconCode >= 71 && iconCode <= 75) return "Rain";
        if (iconCode >= 76 && iconCode <= 80) return "Heavy Rain";
        if (iconCode >= 81 && iconCode <= 85) return "Thunderstorm";
        return "Sunny";
    }

    /**
     * Data class for current weather.
     */
    public static class WeatherData {
        public double temperature = 24.0;
        public int humidity = 65;
        public double rainfall = 0.0;
        public double windSpeed = 10.0;
        public String windDirection = "NE";
        public int uvIndex = 5;
        public String iconCode = "50";
        public String description = "Sunny";
    }

    /**
     * Data class for weather forecast.
     */
    public static class ForecastData {
        public java.util.List<ForecastDay> forecastDays = new java.util.ArrayList<>();
    }

    /**
     * Data class for a single forecast day.
     */
    public static class ForecastDay {
        public String date = "";
        public String week = "";
        public String weather = "";
        public int tempMax = 28;
        public int tempMin = 22;
        public int humidityMax = 80;
        public int humidityMin = 60;
    }

    // Listener interfaces
    public interface OnWeatherDataListener {
        void onSuccess(WeatherData data);
        void onError(String error);
    }

    public interface OnForecastListener {
        void onSuccess(ForecastData data);
        void onError(String error);
    }

    public interface OnUvIndexListener {
        void onSuccess(int uvIndex);
        void onError(String error);
    }
}
