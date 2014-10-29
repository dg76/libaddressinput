namespace java com.foursquare.geo.gen

include "com/foursquare/twofishes/geocoder.thrift"

// A US-centric simplification of address structure, that should mostly work
struct PostalAddress {
  // globally: address line 1
  // ex: 348 W 23rd St
  1: optional string streetAddress

  // One of the subdivisions within a town. This category includes suburbs, neighborhoods, wards.
  2: optional string neighborhood

  // One of the major populated places within a country.
  // This category includes incorporated cities and towns, major unincorporated towns and villages.
  3: optional string city

  // leaving out admin-3 from this hierarchy because of how rarely we care

  // globally: admin-2
  // ex: County, Province, Parish, Department, District.
  4: optional string county

  // globally: admin-1
  // ex: State, Province, Prefecture, Country, Region, Federal District.
  5: optional string province
  6: optional string provinceCode

  7: optional string postalCode

  // Convention: iso 2-letter country code
  8: optional string countryCode
  12: optional string country

  9: optional geocoder.GeocodePoint latlng

  // these are very specific to the maponics data model ... but, they're here :-/
  // alphabet city = sub
  // east village = neighborhood
  // downtown / midtown = macro
  10: optional string subneighborhood
  11: optional string macroneighborhood

  13: optional string crossStreet
}
