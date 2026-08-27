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
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "GirlPeriod-App/1.0");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "HTTP Response Code: " + responseCode + " for URL: " + urlString);
            
            if (responseCode != 200) {
                // Read error response
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                errorReader.close();
                throw new Exception("HTTP " + responseCode + ": " + errorResponse.toString());
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            Log.d(TAG, "Response length: " + response.length() + " chars");
            // Log first 500 chars of response for debugging
            Log.d(TAG, "Raw response: " + response.substring(0, Math.min(response.length(), 500)));
            return response.toString();
        } catch (Exception e) {
            Log.e(TAG, "HTTP Request failed: " + e.getMessage(), e);
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Parse current weather JSON response - robust parsing with fallbacks.
     */
    private WeatherData parseCurrentWeather(String json) throws Exception {
        WeatherData data = new WeatherData();
        try {
            JSONObject root = new JSONObject(json);
            Log.d(TAG, "Parsing JSON, keys: " + root.length());

            // Parse temperature - pick King's Park or Hong Kong Observatory
            if (root.has("temperature")) {
                Object tempObj = root.get("temperature");
                if (tempObj instanceof JSONArray) {
                    JSONArray tempArray = (JSONArray) tempObj;
                    // Try to find King's Park or Hong Kong Observatory first
                    for (int i = 0; i < tempArray.length(); i++) {
                        JSONObject station = tempArray.getJSONObject(i);
                        String place = station.optString("place", "").toLowerCase();
                        if (place.contains("king") || place.contains("observatory")) {
                            data.temperature = station.optDouble("value", 28.0);
                            break;
                        }
                    }
                    // Fallback to first valid temperature in reasonable range
                    if (data.temperature == 0) {
                        for (int i = 0; i < tempArray.length(); i++) {
                            JSONObject station = tempArray.getJSONObject(i);
                            double val = station.optDouble("value", 0);
                            if (val > 10 && val < 45) {
                                data.temperature = val;
                                break;
                            }
                        }
                    }
                }
            }

            // Parse humidity - get from Hong Kong Observatory
            if (root.has("humidity")) {
                Object humObj = root.get("humidity");
                if (humObj instanceof JSONArray) {
                    JSONArray humArray = (JSONArray) humObj;
                    if (humArray.length() > 0) {
                        data.humidity = humArray.getJSONObject(0).optInt("value", 75);
                    }
                } else if (humObj instanceof JSONObject) {
                    data.humidity = ((JSONObject) humObj).optInt("value", 75);
                }
            }

            // Parse rainfall - get max value from all stations
            if (root.has("rainfall")) {
                Object rainObj = root.get("rainfall");
                if (rainObj instanceof JSONArray) {
                    JSONArray rainArray = (JSONArray) rainObj;
                    double maxRain = 0;
                    for (int i = 0; i < rainArray.length(); i++) {
                        JSONObject station = rainArray.getJSONObject(i);
                        double max = station.optDouble("max", 0);
                        if (max > maxRain) maxRain = max;
                    }
                    data.rainfall = maxRain;
                }
            }

            // Parse wind
            if (root.has("wind")) {
                Object windObj = root.get("wind");
                if (windObj instanceof JSONArray) {
                    JSONArray windArray = (JSONArray) windObj;
                    if (windArray.length() > 0) {
                        JSONObject first = windArray.getJSONObject(0);
                        data.windSpeed = first.optDouble("speed", 10.0);
                        data.windDirection = first.optString("direction", "NE");
                    }
                } else if (windObj instanceof JSONObject) {
                    JSONObject wind = (JSONObject) windObj;
                    data.windSpeed = wind.optDouble("speed", 10.0);
                    data.windDirection = wind.optString("direction", "NE");
                }
            }

            // Parse icon/weather condition
            if (root.has("icon")) {
                Object iconObj = root.get("icon");
                if (iconObj instanceof JSONArray) {
                    JSONArray iconArray = (JSONArray) iconObj;
                    if (iconArray.length() > 0) {
                        int iconCode = iconArray.optInt(0, 50);
                        data.iconCode = String.valueOf(iconCode);
                        data.description = getWeatherDescription(iconCode);
                    }
                }
            }

            // Parse weather description if available (more accurate)
            if (root.has("weatherDesc")) {
                try {
                    JSONObject weatherDesc = root.getJSONObject("weatherDesc");
                    if (weatherDesc.has("en")) {
                        data.description = weatherDesc.getString("en");
                    }
                } catch (Exception e) {
                    // Ignore, use icon-based description
                }
            }

            // Parse UV index - may not be in current weather response
            if (root.has("uvindex")) {
                try {
                    Object uvObj = root.get("uvindex");
                    if (uvObj instanceof JSONArray) {
                        JSONArray uvArray = (JSONArray) uvObj;
                        if (uvArray.length() > 0) {
                            data.uvIndex = uvArray.getJSONObject(0).optInt("value", 5);
                        }
                    } else if (uvObj instanceof JSONObject) {
                        data.uvIndex = ((JSONObject) uvObj).optInt("value", 5);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "UV index parse error: " + e.getMessage());
                    data.uvIndex = 5;
                }
            } else {
                data.uvIndex = 5; // Default
            }

            Log.d(TAG, "Parsed weather: temp=" + data.temperature + ", hum=" + data.humidity + ", rain=" + data.rainfall + ", wind=" + data.windSpeed);
            
        } catch (Exception e) {
            Log.e(TAG, "JSON parse error: " + e.getMessage() + "\nRaw JSON snippet: " + json.substring(0, Math.min(json.length(), 300)));
            throw new Exception("Failed to parse weather data: " + e.getMessage());
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
                
                // Parse date from "yyyyMMdd" to "yyyy-MM-dd" format
                String rawDate = day.optString("forecastDate", "");
                if (rawDate.length() == 8) {
                    forecastDay.date = rawDate.substring(0, 4) + "-" + rawDate.substring(4, 6) + "-" + rawDate.substring(6, 8);
                } else {
                    forecastDay.date = rawDate;
                }
                
                forecastDay.week = day.optString("week", "");
                forecastDay.weather = day.optString("forecastWeather", "");
                
                // Parse nested temperature objects
                if (day.has("forecastMaxtemp")) {
                    forecastDay.tempMax = day.getJSONObject("forecastMaxtemp").optInt("value", 28);
                } else {
                    forecastDay.tempMax = 28;
                }
                if (day.has("forecastMintemp")) {
                    forecastDay.tempMin = day.getJSONObject("forecastMintemp").optInt("value", 22);
                } else {
                    forecastDay.tempMin = 22;
                }
                if (day.has("forecastMaxrh")) {
                    forecastDay.humidityMax = day.getJSONObject("forecastMaxrh").optInt("value", 80);
                } else {
                    forecastDay.humidityMax = 80;
                }
                if (day.has("forecastMinrh")) {
                    forecastDay.humidityMin = day.getJSONObject("forecastMinrh").optInt("value", 60);
                } else {
                    forecastDay.humidityMin = 60;
                }
                
                // Parse forecast icon
                forecastDay.icon = day.optInt("ForecastIcon", 50);
                
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
        // HKO Weather Icon Codes
        switch (iconCode) {
            case 50: return "Fine";
            case 51: return "Sunny Periods";
            case 52: return "Sunny with Showers";
            case 53: return "Cloudy";
            case 54: return "Overcast";
            case 60: return "Rain";
            case 61: return "Heavy Rain";
            case 62: return "Thunderstorm";
            case 63: return "Showers";
            case 64: return "Heavy Showers";
            case 65: return "Isolated Showers";
            case 70: return "Light Rain";
            case 71: return "Rain";
            case 72: return "Heavy Rain";
            case 73: return "Rain";
            case 74: return "Heavy Rain";
            case 75: return "Heavy Rain";
            case 80: return "Thunderstorm";
            case 81: return "Thunderstorm";
            case 82: return "Severe Thunderstorm";
            case 83: return "Thunderstorm";
            case 84: return "Severe Thunderstorm";
            case 85: return "Severe Thunderstorm";
            default: return iconCode >= 50 && iconCode <= 54 ? "Sunny" : "Cloudy";
        }
    }

    /**
     * Data class for current weather.
     */
    public static class WeatherData {
        public double temperature = 28.0;
        public int humidity = 75;
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
        public int icon = 50;
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
