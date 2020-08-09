/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositories;

import com.crio.qeats.models.RestaurantEntity;
import com.mongodb.client.model.geojson.Point;

import java.util.List;
import java.util.Optional;

import org.springframework.data.geo.Distance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface RestaurantRepository extends MongoRepository<RestaurantEntity, String> {
  public List<RestaurantEntity> findByRestaurantIdIn(List<String> nameList);

  @Query("{'name': {$regex: '^?0$', $options: 'i'}}")
  public List<RestaurantEntity> findByNameLike(String name);

  @Query("{'name': {$regex: '.*?0.*', $options: 'i'}}")
  public List<RestaurantEntity> findByNameExact(String name);

  public List<RestaurantEntity> findByAttributesIn(List<String> attributes);
}

