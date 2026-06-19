package com.example.fabibookingvendorsystem;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

import retrofit2.http.Query;

public interface ApiService {
    @GET("posts")
    Call<List<Post>> getPosts();

    // Weather API (Free: open-meteo.com)
    @GET("https://api.open-meteo.com/v1/forecast")
    Call<WeatherResponse> getWeather(
        @Query("latitude") double lat,
        @Query("longitude") double lon,
        @Query("current_weather") boolean current
    );
}

// Weather Response Model
class WeatherResponse {
    public CurrentWeather current_weather;
}

class CurrentWeather {
    public double temperature;
    public double windspeed;
}

// Simple Model for the API response
class Post {
    public int userId;
    public int id;
    public String title;
    public String body;
}
