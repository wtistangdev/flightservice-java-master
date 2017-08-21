/*******************************************************************************
* Copyright (c) 2013 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package com.acmeair.web;

import com.acmeair.securityutils.SecurityUtils;
import com.acmeair.service.FlightService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.inject.Inject;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/* microprofile-1.1 */
import org.eclipse.microprofile.config.inject.ConfigProperty;


@Path("/")
public class FlightServiceRest {

  private static final JsonBuilderFactory factory = Json.createBuilderFactory(null);
  private static final int year =  Calendar.getInstance().get(Calendar.YEAR);

  /* microprofile-1.1 */
  @Inject @ConfigProperty(name="SECURE_SERVICE_CALLS", defaultValue="false") private Boolean SECURE_SERVICE_CALLS;
  /*
  private static final Boolean SECURE_SERVICE_CALLS = 
          Boolean.valueOf((System.getenv("SECURE_SERVICE_CALLS") == null) ? "false" 
                  : System.getenv("SECURE_SERVICE_CALLS"));
  */
  /* cannot use injected member variables in the constructor
  static {
    System.out.println("SECURE_SERVICE_CALLS: " + SECURE_SERVICE_CALLS);
  }
  */
    
  @Inject
  private FlightService flightService;
    
  @Inject
  private SecurityUtils secUtils;
  
  /**
   * Get flights.
   */
  // Had to change Dates to strings + extra processing below because 
  // Payara did not handle the Dates well.
  @POST
  @Path("/queryflights")
  @Consumes({"application/x-www-form-urlencoded"})
  @Produces("text/plain")
  public String getTripFlights(
          @FormParam("fromAirport") String fromAirport,
          @FormParam("toAirport") String toAirport,
          @FormParam("fromDate") String fromDate,
          @FormParam("returnDate") String returnDate,
          @FormParam("oneWay") boolean oneWay
  ) throws ParseException {

    String options = "";

    // convert date to local timezone
    Calendar tempDate = new GregorianCalendar();
   
    SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd");   
    tempDate.setTime(sdf.parse(fromDate.substring(0,10)));
    
    // reset hour, minutes, seconds and millis
    tempDate.set(Calendar.HOUR_OF_DAY, 0);
    tempDate.set(Calendar.MINUTE, 0);
    tempDate.set(Calendar.SECOND, 0);
    tempDate.set(Calendar.MILLISECOND, 0);
    tempDate.set(Calendar.YEAR, year);

    List<String> toFlights = flightService.getFlightByAirportsAndDepartureDate(fromAirport, 
            toAirport, tempDate.getTime());

    if (!oneWay) {
      // convert date to local timezone
      tempDate.setTime(sdf.parse(returnDate.substring(0,10)));
      
      // reset hour, minutes, seconds and millis
      tempDate.set(Calendar.HOUR_OF_DAY, 0);
      tempDate.set(Calendar.MINUTE, 0);
      tempDate.set(Calendar.SECOND, 0);
      tempDate.set(Calendar.MILLISECOND, 0);
      tempDate.set(Calendar.YEAR, year);
      
      List<String> retFlights = flightService.getFlightByAirportsAndDepartureDate(toAirport, 
              fromAirport, tempDate.getTime());
        
      // TODO: Why are we doing it like this?
      options = "{\"tripFlights\":"  
              + "[{\"numPages\":1,\"flightsOptions\": " 
              + toFlights 
              + ",\"currentPage\":0,\"hasMoreOptions\":false,\"pageSize\":10}, " 
              + "{\"numPages\":1,\"flightsOptions\": " 
              + retFlights 
              + ",\"currentPage\":0,\"hasMoreOptions\":false,\"pageSize\":10}], " 
              + "\"tripLegs\":2}";
    } else {
      options = "{\"tripFlights\":" 
              + "[{\"numPages\":1,\"flightsOptions\": " 
              + toFlights 
              + ",\"currentPage\":0,\"hasMoreOptions\":false,\"pageSize\":10}], " 
              + "\"tripLegs\":1}";
    }
    return options;
  }

  /**
   * Get reward miles for flight segment.
   */
  @POST
  @Path("/getrewardmiles")
  @Consumes({"application/x-www-form-urlencoded"})
  @Produces("application/json")
  public Response getRewardMiles(
          @HeaderParam("acmeair-id") String headerId,
          @HeaderParam("acmeair-date") String headerDate,
          @HeaderParam("acmeair-sig-body") String headerSigBody, 
          @HeaderParam("acmeair-signature") String headerSig,
          @FormParam("flightSegment") String segmentId
  ) {
    if (SECURE_SERVICE_CALLS) { 
      String body = "flightSegment=" + segmentId;
      secUtils.verifyBodyHash(body, headerSigBody);
      secUtils.verifyFullSignature("POST", "/getrewardmiles",headerId,headerDate,
              headerSigBody,headerSig);
    }

    Long miles = flightService.getRewardMiles(segmentId); 
    JsonObjectBuilder job = factory.createObjectBuilder();
    JsonObject value = job.add("miles", miles).build();
    return Response.ok(value.toString()).build();
  }

  @GET
  public Response checkStatus() {
    return Response.ok("OK").build();        
  }
}