
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({ "id" })
public class Restaurant {
  private String restaurantId;
  private String name;
  private String city;
  private String imageUrl;
  private Double latitude;
  private Double longitude;
  private String opensAt;
  private String closesAt;
  private List<String> attributes = new ArrayList<>();
}

