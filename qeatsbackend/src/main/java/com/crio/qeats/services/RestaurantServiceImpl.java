
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetMenuResponse;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import com.crio.qeats.utils.Helpers;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.experimental.Helper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;

  private final Double getServingRadius(LocalTime currentTime) {
    int currentHour = currentTime.getHour();
    if ((currentHour >= 8 && currentHour <= 10)
        || (currentHour >= 13 && currentHour <= 14) 
        || (currentHour >= 19 && currentHour <= 21)) {
      return peakHoursServingRadiusInKms;
    } else {
      return normalHoursServingRadiusInKms;
    }
  }
  
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    Double servingRadiusInKms = getServingRadius(currentTime);
    
    List<Restaurant> restaurants = restaurantRepositoryService.findAllRestaurantsCloseBy(
        getRestaurantsRequest.getLatitude(), 
        getRestaurantsRequest.getLongitude(), 
        currentTime,
        servingRadiusInKms
    );
    return new GetRestaurantsResponse(restaurants);
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    if (getRestaurantsRequest.getSearchFor().isEmpty()) {
      return new GetRestaurantsResponse(Collections.emptyList());
    }
    Double latitude = getRestaurantsRequest.getLatitude();
    Double longitude = getRestaurantsRequest.getLongitude();
    String searchString = getRestaurantsRequest.getSearchFor();
    Double servingRadiusInKms = getServingRadius(currentTime);
    List<Restaurant> restaurants = restaurantRepositoryService.findRestaurantsByName(
        latitude, longitude, searchString, currentTime, servingRadiusInKms);
    restaurants.addAll(restaurantRepositoryService.findRestaurantsByAttributes(
        latitude, longitude, searchString, currentTime, servingRadiusInKms));
    restaurants.addAll(restaurantRepositoryService.findRestaurantsByItemName(
        latitude, longitude, searchString, currentTime, servingRadiusInKms));
    restaurants.addAll(restaurantRepositoryService.findRestaurantsByItemAttributes(
        latitude, longitude, searchString, currentTime, servingRadiusInKms));
    List<Restaurant> distinctRestaurants = restaurants.stream() 
        .filter(Helpers.distinctByKey(Restaurant::getRestaurantId)) 
        .collect(Collectors.toList());
    return new GetRestaurantsResponse(distinctRestaurants);
  }

  @Override
  public GetMenuResponse getMenuByRestaurantId(String restaurantId) {
    return new GetMenuResponse(restaurantRepositoryService.getMenuByRestaurantId(restaurantId));
  }
  
  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  // Implement multi-threaded version of RestaurantSearch.
  // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
  // findRestaurantsBySearchQuery.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<Callable<List<Restaurant>>> callableTasks = new ArrayList<>();
    Double latitude = getRestaurantsRequest.getLatitude();
    Double longitude = getRestaurantsRequest.getLongitude();
    String searchString = getRestaurantsRequest.getSearchFor();
    Double servingRadiusInKms = getServingRadius(currentTime);
    
    callableTasks.add(() -> restaurantRepositoryService.findRestaurantsByName(
        latitude, longitude, searchString, currentTime, servingRadiusInKms));
    callableTasks.add(() -> restaurantRepositoryService.findRestaurantsByAttributes(
        latitude, longitude, searchString, currentTime, servingRadiusInKms));
    callableTasks.add(() -> restaurantRepositoryService.findRestaurantsByItemName(
        latitude, longitude, searchString, currentTime, servingRadiusInKms));
    callableTasks.add(() -> restaurantRepositoryService.findRestaurantsByItemAttributes(
        latitude, longitude, searchString, currentTime, servingRadiusInKms));
    try {
      List<Restaurant> restaurants = new ArrayList<>();
      executor.invokeAll(callableTasks).forEach(restaurantFuture -> {
        try {
          restaurants.addAll((List<Restaurant>)restaurantFuture.get());
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      });
      return new GetRestaurantsResponse(restaurants);     
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }
}

