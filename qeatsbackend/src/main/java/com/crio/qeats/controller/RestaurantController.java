/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.controller;

import com.crio.qeats.exchanges.GetMenuResponse;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.services.RestaurantService;
import java.time.LocalTime;
import javax.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@Controller
@RequestMapping("/qeats/v1")
public class RestaurantController {

  public static final String RESTAURANT_API_ENDPOINT = "/qeats/v1";
  public static final String RESTAURANTS_API = "/restaurants";
  public static final String MENU_API = "/menu";
  public static final String CART_API = "/cart";
  public static final String CART_ITEM_API = "/cart/item";
  public static final String CART_CLEAR_API = "/cart/clear";
  public static final String POST_ORDER_API = "/order";
  public static final String GET_ORDERS_API = "/orders";

  @Autowired
  private RestaurantService restaurantService;

  @GetMapping(RESTAURANTS_API)
  public ResponseEntity<GetRestaurantsResponse> getRestaurants(
      @Valid GetRestaurantsRequest getRestaurantsRequest) {
    
    log.info("getRestaurants called with {}", getRestaurantsRequest);
    GetRestaurantsResponse getRestaurantsResponse;
    
    if (getRestaurantsRequest.getSearchFor().isEmpty()) {
      log.info("returned all");
      getRestaurantsResponse = restaurantService
        .findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());
    } else {
      log.info("returned from search query {}", getRestaurantsRequest.getSearchFor());
      getRestaurantsResponse = restaurantService
        .findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.now());
    }
    if (getRestaurantsResponse != null) {
      log.info("getRestaurants returned {} restaurants", 
          getRestaurantsResponse.getRestaurants());
      getRestaurantsResponse.getRestaurants().forEach(res -> {
        res.setName(res.getName().replaceAll("[^\\p{ASCII}]", ""));
      });
    }
    return ResponseEntity.ok().body(getRestaurantsResponse);
  }

  @GetMapping(MENU_API)
  public ResponseEntity<GetMenuResponse> getMenu(@RequestParam String restaurantId) {
    GetMenuResponse getMenuResponse = restaurantService.getMenuByRestaurantId(restaurantId);
    return ResponseEntity.ok().body(getMenuResponse);
  }
}

