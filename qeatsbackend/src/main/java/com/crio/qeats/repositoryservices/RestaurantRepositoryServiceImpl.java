/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;

import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoUtils;
import com.crio.qeats.utils.Helpers;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Provider;

import lombok.extern.log4j.Log4j2;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;


@Log4j2
@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  @Autowired
  private RestaurantRepository repository;

  @Autowired
  private MenuRepository menuRepository;

  @Autowired
  private RedisConfiguration redisConfiguration;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());
    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude,
      Double longitude, LocalTime currentTime, Double servingRadiusInKms) {

    ObjectMapper objectMapper = new ObjectMapper();
    List<Restaurant> restaurants = new ArrayList<Restaurant>();
    boolean cacheHit = false;
    List<RestaurantEntity> resData = new ArrayList<>();
    if (redisConfiguration.isCacheAvailable()) {
      Jedis jedis = redisConfiguration.getJedisPool().getResource();
      String key = GeoHash.withCharacterPrecision(latitude, longitude, 7).toBase32();
      if (jedis.exists(key)) {
        String restaurantsString = jedis.get(key);
        cacheHit = true;
        try {
          resData = Arrays.asList(objectMapper.readValue(
              restaurantsString, RestaurantEntity[].class));
          log.info("returned from redis");
        } catch (Exception e) {
          throw new RuntimeException();
        }
      }
    }

    if (!cacheHit) {
      log.info("returned from mongodb");
      resData = repository.findAll().stream().filter(res -> isRestaurantCloseBy(
          res, 
          latitude, 
          longitude, 
          servingRadiusInKms
      )).collect(Collectors.toList());
      if (redisConfiguration.isCacheAvailable()) {
        Jedis jedis = redisConfiguration.getJedisPool().getResource();
        String key = GeoHash.withCharacterPrecision(latitude, longitude, 7).toBase32();
        try {
          String value = objectMapper.writeValueAsString(resData);
          jedis.set(key, value);
          jedis.expire(key, RedisConfiguration.REDIS_ENTRY_EXPIRY_IN_SECONDS);
        } catch (Exception e) {
          throw new RuntimeException();
        }
      }
    }

    resData.forEach(res -> {
      if (isOpenNow(currentTime, res)) {
        restaurants.add(convertToRestaurant(res));
      }
    });
    return restaurants;
  }

  private static Restaurant convertToRestaurant(RestaurantEntity res) {
    return new Restaurant(
      res.getRestaurantId(),
      res.getName(),
      res.getCity(),
      res.getImageUrl(),
      res.getLatitude(),
      res.getLongitude(),
      res.getOpensAt(),
      res.getClosesAt(),
      res.getAttributes()
    );
  }


  private boolean isRestaurantCloseBy(RestaurantEntity restaurantEntity,
      Double latitude, Double longitude, Double servingRadiusInKms) {
    return GeoUtils.findDistanceInKm(latitude, longitude,
        restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
        < servingRadiusInKms;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurants = new ArrayList<>();
    repository.findByNameExact(searchString).forEach(restaurantEntity -> {
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, 
          latitude, longitude, servingRadiusInKms)) {
        restaurants.add(convertToRestaurant(restaurantEntity));
      }
    });
    repository.findByNameLike(searchString).forEach(restaurantEntity -> {
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, 
          latitude, longitude, servingRadiusInKms)) {
        restaurants.add(convertToRestaurant(restaurantEntity));
      }
    });
    return restaurants.stream()
        .filter(Helpers.distinctByKey(Restaurant::getRestaurantId))
        .collect(Collectors.toList());
  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurants = new ArrayList<>();
    repository.findByAttributesIn(Arrays.asList(searchString)).forEach(restaurantEntity -> {
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, 
          latitude, longitude, servingRadiusInKms)) {
        restaurants.add(convertToRestaurant(restaurantEntity));
      }
    });
    return restaurants;
  }




  @Override
  public List<Restaurant> findRestaurantsByItemName(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurants = new ArrayList<>();
    List<String> restaurantIds = menuRepository
        .findMenusByItemsNameExact(searchString).stream()
        .map(MenuEntity::getRestaurantId).collect(Collectors.toList());
    repository.findByRestaurantIdIn(restaurantIds).forEach(restaurantEntity -> {
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, 
          latitude, longitude, servingRadiusInKms)) {
        restaurants.add(convertToRestaurant(restaurantEntity));
      }
    });
    restaurantIds = menuRepository
        .findMenusByItemsNameLike(searchString).stream()
        .map(MenuEntity::getRestaurantId).collect(Collectors.toList());
    repository.findByRestaurantIdIn(restaurantIds).forEach(restaurantEntity -> {
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, 
          latitude, longitude, servingRadiusInKms)) {
        restaurants.add(convertToRestaurant(restaurantEntity));
      }
    });
    List<Restaurant> distinctRestaurants = restaurants.stream()
        .filter(Helpers.distinctByKey(Restaurant::getRestaurantId))
        .collect(Collectors.toList());
    return distinctRestaurants;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurants = new ArrayList<>();
    List<String> restaurantIds = menuRepository
        .findMenusByItemsAttributesLike(searchString).stream()
        .map(MenuEntity::getRestaurantId).collect(Collectors.toList());
    repository.findByRestaurantIdIn(restaurantIds).forEach(restaurantEntity -> {
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, 
          latitude, longitude, servingRadiusInKms)) {
        restaurants.add(convertToRestaurant(restaurantEntity));
      }
    });
    return restaurants;
  }



  @Override
  public MenuEntity getMenuByRestaurantId(String restaurantId) {
    return menuRepository.findMenuByRestaurantId(restaurantId).get();
  }

  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return isRestaurantCloseBy(restaurantEntity, latitude, longitude, servingRadiusInKms);
    }
    return false;
  }

}

