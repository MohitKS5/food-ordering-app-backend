/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.exchanges;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetRestaurantsRequest {
  @NotNull
  @Min(-90)
  @Max(90)
  private Double latitude;
  @Min(-180)
  @Max(180)
  @NotNull
  private Double longitude;
  private String searchFor = "";
  
  public GetRestaurantsRequest(Double lat, Double longt) {
    this.latitude = lat;
    this.longitude = longt;
  }
}

