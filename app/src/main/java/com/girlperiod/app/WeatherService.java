package com.girlperiod.app;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Mock weather service that provides weather data for the calendar.
 * Generates semi-random but consistent data based on the date.
 */
public final class WeatherService {

    private WeatherService() {
        throw new AssertionError("No instances.");
    }

    // Weather condition codes
    public static final int WEATHER_SUNNY = 0;
    public static final int WEATHER_CLOUDY = 1;
    public static final int WEATHER_OVERCAST = 2;
    public static final int WEATHER_LIGHT_RAIN = 3;
    public static final int WEATHER_MODERATE_RAIN = 4;
    public static final int WEATHER_HEAVY_RAIN = 5;
    public static final int WEATHER_THUNDERSTORM = 6;
    public static final int WEATHER_LIGHT_SNOW = 7;
    public static final int WEATHER_MODERATE_SNOW = 8;
    public static final int WEATHER_HEAVY_SNOW = 9;
    public static final int WEATHER_FOG = 10;
    public static final int WEATHER_HAZE = 11;
    public static final int WEATHER_WINDY = 12;

    private static final Map<String, double[]> CITY_COORDINATES = new HashMap<>();

    static {
        // Latitude and longitude for major cities
        CITY_COORDINATES.put("北京", new double[]{39.9042, 116.4074});
        CITY_COORDINATES.put("上海", new double[]{31.2304, 121.4737});
        CITY_COORDINATES.put("广州", new double[]{23.1291, 113.2644});
        CITY_COORDINATES.put("深圳", new double[]{22.5431, 114.0579});
        CITY_COORDINATES.put("杭州", new double[]{30.2741, 120.1551});
        CITY_COORDINATES.put("成都", new double[]{30.5728, 104.0668});
        CITY_COORDINATES.put("武汉", new double[]{30.5928, 114.3055});
        CITY_COORDINATES.put("西安", new double[]{34.3416, 108.9398});
        CITY_COORDINATES.put("南京", new double[]{32.0603, 118.7969});
        CITY_COORDINATES.put("重庆", new double[]{29.5630, 106.5516});
        CITY_COORDINATES.put("天津", new double[]{39.3434, 117.3616});
        CITY_COORDINATES.put("苏州", new double[]{31.2989, 120.5853});
        CITY_COORDINATES.put("长沙", new double[]{28.2282, 112.9388});
        CITY_COORDINATES.put("郑州", new double[]{34.7466, 113.6253});
        CITY_COORDINATES.put("青岛", new double[]{36.0671, 120.3826});
        CITY_COORDINATES.put("沈阳", new double[]{41.8057, 123.4315});
        CITY_COORDINATES.put("哈尔滨", new double[]{45.8038, 126.5350});
        CITY_COORDINATES.put("昆明", new double[]{25.0389, 102.7183});
        CITY_COORDINATES.put("福州", new double[]{26.0745, 119.2965});
    }

    /**
     * Returns the current weather for the given city.
     *
     * @param city the city name
     * @return a WeatherData object with mock weather information
     */
    public static WeatherData getCurrentWeather(String city) {
        return generateWeatherData(city, new Date());
    }

    /**
     * Returns the weather for a specific date and city.
     *
     * @param city the city name
     * @param date the date
     * @return a WeatherData object with mock weather information
     */
    public static WeatherData getWeatherForDate(String city, Date date) {
        return generateWeatherData(city, date);
    }

    /**
     * Returns the UV index for the given city.
     *
     * @param city the city name
     * @return the UV index (0-11+)
     */
    public static int getUVIndex(String city) {
        WeatherData data = getCurrentWeather(city);
        return data.getUvIndex();
    }

    /**
     * Returns the humidity for the given city.
     *
     * @param city the city name
     * @return the humidity percentage (0-100)
     */
    public static int getHumidity(String city) {
        WeatherData data = getCurrentWeather(city);
        return data.getHumidity();
    }

    /**
     * Returns the rainfall for the given city.
     *
     * @param city the city name
     * @return the rainfall in mm
     */
    public static double getRainfall(String city) {
        WeatherData data = getCurrentWeather(city);
        return data.getRainfall();
    }

    /**
     * Returns the temperature for the given city.
     *
     * @param city the city name
     * @return the temperature in Celsius
     */
    public static double getTemperature(String city) {
        WeatherData data = getCurrentWeather(city);
        return data.getTemperature();
    }

    /**
     * Maps a weather code to a description string.
     *
     * @param code the weather condition code
     * @return the weather description like "晴朗", "多云", "小雨" etc.
     */
    public static String getWeatherDescription(int code) {
        switch (code) {
            case WEATHER_SUNNY:
                return "晴朗";
            case WEATHER_CLOUDY:
                return "多云";
            case WEATHER_OVERCAST:
                return "阴天";
            case WEATHER_LIGHT_RAIN:
                return "小雨";
            case WEATHER_MODERATE_RAIN:
                return "中雨";
            case WEATHER_HEAVY_RAIN:
                return "大雨";
            case WEATHER_THUNDERSTORM:
                return "雷阵雨";
            case WEATHER_LIGHT_SNOW:
                return "小雪";
            case WEATHER_MODERATE_SNOW:
                return "中雪";
            case WEATHER_HEAVY_SNOW:
                return "大雪";
            case WEATHER_FOG:
                return "雾";
            case WEATHER_HAZE:
                return "霾";
            case WEATHER_WINDY:
                return "大风";
            default:
                return "未知";
        }
    }

    /**
     * Returns the icon code for the given weather code.
     *
     * @param code the weather condition code
     * @return the icon code string
     */
    public static String getWeatherIconCode(int code) {
        switch (code) {
            case WEATHER_SUNNY:
                return "01d";
            case WEATHER_CLOUDY:
                return "02d";
            case WEATHER_OVERCAST:
                return "03d";
            case WEATHER_LIGHT_RAIN:
                return "09d";
            case WEATHER_MODERATE_RAIN:
                return "10d";
            case WEATHER_HEAVY_RAIN:
                return "11d";
            case WEATHER_THUNDERSTORM:
                return "11d";
            case WEATHER_LIGHT_SNOW:
                return "13d";
            case WEATHER_MODERATE_SNOW:
                return "13d";
            case WEATHER_HEAVY_SNOW:
                return "13d";
            case WEATHER_FOG:
                return "50d";
            case WEATHER_HAZE:
                return "50d";
            case WEATHER_WINDY:
                return "50d";
            default:
                return "unknown";
        }
    }

    // --- Private helper methods ---

    private static WeatherData generateWeatherData(String city, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);

        // Use city name hash + day of year as seed for consistent data
        long seed = (long) city.hashCode() * 31 + dayOfYear;
        Random random = new Random(seed);

        double[] coords = CITY_COORDINATES.getOrDefault(city, new double[]{35.0, 110.0});
        double latitude = coords[0];

        // Calculate base temperature based on latitude and day of year
        double baseTemp = calculateBaseTemperature(latitude, dayOfYear);
        double temperature = baseTemp + (random.nextDouble() * 6 - 3); // +/- 3 degrees variation

        // Determine weather code based on temperature and randomness
        int weatherCode = determineWeatherCode(temperature, dayOfYear, random);

        // UV index based on weather and day of year
        int uvIndex = calculateUVIndex(weatherCode, dayOfYear, random);

        // Humidity based on weather
        int humidity = calculateHumidity(weatherCode, random);

        // Rainfall based on weather
        double rainfall = calculateRainfall(weatherCode, random);

        String description = getWeatherDescription(weatherCode);
        String iconCode = getWeatherIconCode(weatherCode);

        return new WeatherData(temperature, uvIndex, humidity, rainfall, description, iconCode);
    }

    private static double calculateBaseTemperature(double latitude, int dayOfYear) {
        // Simplified seasonal temperature model
        // Peak summer around day 200, peak winter around day 1/365
        double seasonalFactor = Math.sin((dayOfYear - 80) * 2 * Math.PI / 365);
        double baseTemp = 25 - (latitude - 23.5) * 0.5; // Base temp decreases with latitude
        return baseTemp + seasonalFactor * 10;
    }

    private static int determineWeatherCode(double temperature, int dayOfYear, Random random) {
        double rand = random.nextDouble();

        if (temperature < 0) {
            // Snow possible
            if (rand < 0.3) return WEATHER_LIGHT_SNOW;
            if (rand < 0.5) return WEATHER_MODERATE_SNOW;
            if (rand < 0.6) return WEATHER_HEAVY_SNOW;
            if (rand < 0.8) return WEATHER_SUNNY;
            return WEATHER_CLOUDY;
        } else if (temperature < 10) {
            if (rand < 0.15) return WEATHER_LIGHT_RAIN;
            if (rand < 0.25) return WEATHER_MODERATE_RAIN;
            if (rand < 0.35) return WEATHER_OVERCAST;
            if (rand < 0.55) return WEATHER_CLOUDY;
            if (rand < 0.75) return WEATHER_SUNNY;
            return WEATHER_FOG;
        } else if (temperature < 25) {
            if (rand < 0.1) return WEATHER_LIGHT_RAIN;
            if (rand < 0.2) return WEATHER_MODERATE_RAIN;
            if (rand < 0.3) return WEATHER_THUNDERSTORM;
            if (rand < 0.45) return WEATHER_OVERCAST;
            if (rand < 0.65) return WEATHER_CLOUDY;
            return WEATHER_SUNNY;
        } else {
            if (rand < 0.05) return WEATHER_LIGHT_RAIN;
            if (rand < 0.1) return WEATHER_THUNDERSTORM;
            if (rand < 0.25) return WEATHER_HAZE;
            if (rand < 0.4) return WEATHER_CLOUDY;
            return WEATHER_SUNNY;
        }
    }

    private static int calculateUVIndex(int weatherCode, int dayOfYear, Random random) {
        double seasonalFactor = Math.sin((dayOfYear - 80) * 2 * Math.PI / 365);
        int baseUV = (int) Math.max(0, (seasonalFactor + 1) * 5);

        switch (weatherCode) {
            case WEATHER_SUNNY:
                return Math.min(11, baseUV + random.nextInt(3));
            case WEATHER_CLOUDY:
                return Math.max(0, baseUV - 2 + random.nextInt(2));
            case WEATHER_OVERCAST:
            case WEATHER_FOG:
            case WEATHER_HAZE:
                return Math.max(0, baseUV - 4);
            default:
                return Math.max(0, baseUV - 1);
        }
    }

    private static int calculateHumidity(int weatherCode, Random random) {
        switch (weatherCode) {
            case WEATHER_LIGHT_RAIN:
            case WEATHER_MODERATE_RAIN:
            case WEATHER_HEAVY_RAIN:
            case WEATHER_THUNDERSTORM:
                return 70 + random.nextInt(30);
            case WEATHER_LIGHT_SNOW:
            case WEATHER_MODERATE_SNOW:
            case WEATHER_HEAVY_SNOW:
                return 60 + random.nextInt(30);
            case WEATHER_FOG:
            case WEATHER_HAZE:
                return 80 + random.nextInt(20);
            case WEATHER_OVERCAST:
                return 50 + random.nextInt(25);
            case WEATHER_CLOUDY:
                return 40 + random.nextInt(25);
            case WEATHER_SUNNY:
                return 20 + random.nextInt(30);
            default:
                return 40 + random.nextInt(20);
        }
    }

    private static double calculateRainfall(int weatherCode, Random random) {
        switch (weatherCode) {
            case WEATHER_LIGHT_RAIN:
                return 1.0 + random.nextDouble() * 4.0;
            case WEATHER_MODERATE_RAIN:
                return 5.0 + random.nextDouble() * 10.0;
            case WEATHER_HEAVY_RAIN:
                return 15.0 + random.nextDouble() * 25.0;
            case WEATHER_THUNDERSTORM:
                return 10.0 + random.nextDouble() * 20.0;
            case WEATHER_LIGHT_SNOW:
                return 0.5 + random.nextDouble() * 2.0;
            case WEATHER_MODERATE_SNOW:
                return 2.0 + random.nextDouble() * 3.0;
            case WEATHER_HEAVY_SNOW:
                return 5.0 + random.nextDouble() * 5.0;
            default:
                return 0.0;
        }
    }

    /**
     * Data class holding weather information.
     */
    public static final class WeatherData {
        private final double temperature;
        private final int uvIndex;
        private final int humidity;
        private final double rainfall;
        private final String description;
        private final String iconCode;

        public WeatherData(double temperature, int uvIndex, int humidity,
                           double rainfall, String description, String iconCode) {
            this.temperature = temperature;
            this.uvIndex = uvIndex;
            this.humidity = humidity;
            this.rainfall = rainfall;
            this.description = description;
            this.iconCode = iconCode;
        }

        public double getTemperature() {
            return temperature;
        }

        public int getUvIndex() {
            return uvIndex;
        }

        public int getHumidity() {
            return humidity;
        }

        public double getRainfall() {
            return rainfall;
        }

        public String getDescription() {
            return description;
        }

        public String getIconCode() {
            return iconCode;
        }

        @Override
        public String toString() {
            return "WeatherData{" +
                "temperature=" + String.format("%.1f", temperature) +
                "°C, uvIndex=" + uvIndex +
                ", humidity=" + humidity +
                "%, rainfall=" + String.format("%.1f", rainfall) +
                "mm, description='" + description + '\'' +
                ", iconCode='" + iconCode + '\'' +
                '}';
        }
    }
}
