import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class WeatherApp {
    public static JSONObject getWeatherData(String locationName) {
        JSONArray locationData = getLocationData(locationName);

        // Latitude e longitude
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        // Construção da URL da API meteorológica
        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=America%2FSao_Paulo";

        try{
            // Conexão com a API meteorológica
            HttpURLConnection conn = fetchApiResponse(urlString);
            if(conn.getResponseCode() != 200){
                System.out.println("Erro de conexão com a API");
                return null;
            }

            // Leitura do JSON retornado pela API meteorológica
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while(scanner.hasNext()){
                resultJson.append(scanner.nextLine());
            }

            // Fechamento do scanner e desconexão com a API
            scanner.close();
            conn.disconnect();

            // String -> JSONObject
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            // Obtenção do tempo atual
            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            // Obtenção da temperatura atual
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            // Obtenção do clima atual
            JSONArray weathercode = (JSONArray) hourly.get("weather_code");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            // Obtenção da umidade atual
            JSONArray relativeHumidity = (JSONArray) hourly.get("relative_humidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            // Obtenção da velocidade do vento atual
            JSONArray windspeedData = (JSONArray) hourly.get("wind_speed_10m");
            double windspeed = (double) windspeedData.get(index);

            // Construção do JSON com os dados meteorológicos
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);

            return weatherData;

        } catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    // Método para obter os dados de localização
    public static JSONArray getLocationData(String locationName) {
        // Substituição dos espaços por "+"
        locationName = locationName.replaceAll(" ", "+");

        // Construção da URL da API de geocodificação
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" + locationName + "&count=10&language=en&format=json";

        try {
            // Conexão com a API de geocodificação
            HttpURLConnection conn = fetchApiResponse(urlString);
            if(conn.getResponseCode() != 200) {
                System.out.println("Erro de conexão com a API");
                return null;
            }

            // Leitura do JSON retornado pela API de geocodificação
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while(scanner.hasNext()) {
                resultJson.append(scanner.nextLine());
            }

            // Fechamento do scanner e desconexão com a API
            scanner.close();
            conn.disconnect();

            // String -> JSONObject
            JSONParser parser = new JSONParser();
            JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            // Obtenção dos dados de localização
            JSONArray locationData = (JSONArray) resultsJsonObj.get("results");

            return locationData;

        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Método para conectar com a API
    private static HttpURLConnection fetchApiResponse(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            conn.connect();
            return conn;
        } catch(IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Método para encontrar o índice do tempo atual no JSON da API meteorológica
    private static int findIndexOfCurrentTime(JSONArray timeList){
        String currentTime = getCurrentTime();

        for(int i = 0; i < timeList.size(); i++){
            String time = (String) timeList.get(i);
            if(time.equalsIgnoreCase(currentTime)){
                return i;
            }
        }

        return 0;
    }

    // Método para obter a hora atual no formato específico
    private static String getCurrentTime(){
        LocalDateTime currentDateTime = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    // Método para converter o código do clima em uma condição meteorológica
    private static String convertWeatherCode(long weathercode){
        String weatherCondition = "";
        if (weathercode == 0L) {
            weatherCondition = "Clear";
        } else if(weathercode > 0L && weathercode <= 3L){
            weatherCondition = "Cloudy";
        } else if((weathercode >= 51L && weathercode <= 67L)
                || (weathercode >= 80L && weathercode <= 99L)){

            weatherCondition = "Rain";
        } else if (weathercode >= 71L && weathercode <= 77) {
            weatherCondition = "Snow";
        }

        return weatherCondition;
    }
}