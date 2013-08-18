
## Location object
This location object is published by the mobile apps and delivered by the backend JSON API
```json
{
    "lat": "xx.xxxxxx", 
    "lon": "y.yyyyyy", 
    "tst": "1376715317",
    "acc": "75m",
    "mo" : "<type>",
    "alt" : "mmmmm",
    "vac" : "xxxx"
}
```

* `lat` is latitude as decimal, represented as a string
* `lon` is longitude as decimal, represented as a string
* `tst` is a UNIX [epoch timestamp](http://en.wikipedia.org/wiki/Unix_time)
* `acc` is accuracy if available
* `mo` is motion (e.g. `vehicle`, `on foot`, etc.) if available
* `alt` altitude, measured in meters (i.e. units of 100cm)
* "vac" : "xxxx" for vertical accuracy in meters - negative value indicates no valid altitude information

## User object
```json
{
    "name": "testuser"
}
```

## Backend API

```none
GET /users
> {"items":[{"name" : foo}, {"name" : "bar"}]}
```

```none
GET /users/1
> {"name" : foo}
```

```none
GET /users/locations?year=2013
```
```none
GET /users/locations?year=2013&month=1
```
```none
GET /users/locations?year=2013&month=1&day=13
```
