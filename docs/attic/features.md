# Features


This is **preliminary** documentation and **subject to change dramatically!**

## General

* Location information is PUBlished as a JSON string. [See json-pub](json-pub.md).

# Currently implemented

## Android

* The broker connection works well with:

  * No TLS (i.e. plain)
  * TLS using the Android build in certificate authorities (either the shipped
ones, or user provided ones that require a lock screen password to be set)
  * TLS with user-provided certificates via an absolute path (e.g. to Downloads).
    This doesn't require a password to be set on the device, but is a bit fiddly
    to set up.
  * Username/password auth works.

* Automatic publishes at configurable intervals (disabled or > 1 minute)

* Display of a marker at which the app believes the user to be at
  (lastKnownLocation)

* Reverse geo coding that displays the address of lastKnownLocation

* Accuracy of lastKnownLocation (if the accuracy is > 250m, the marker
  changes to a circle centered around lastKnownLocation with radius == accuracy)

* Button to manually publish  lastKnownLocation

* Button to share a Google Maps link that places a marker at lastKnownLocation

* For location the Google Fused Location Provider at Balanced Battery
  Settings is used. This one can re use GPS or other Position fixes that are
  requested by other apps in order to save battery and select the most
  appropriate position source.


## iOS

* Runs on iPhones running IOS >=6.1 (3GS, 4, 4S, 5, ...) and iPads running IOS >=6.1 as an iPhone app. Not tested on iPods yet.
* UI is IOS7 compliant

* Monitors "significant location changes" as define by Apple Inc (about 5 minutes AND 
  "significant location changes" (>500m)) or as described in Move Mode below. In addition version >= 5.3 supports
  region monitoring (aka geo fences).

* publishes this locations via MQTT to the configured server while in foreground and background.

* The current location can be send on request.

### Configuration

Configuration is done via the system settings app.

* MQTT server is configured by 
	* specifying hostname/ip-address,
	* port,
	* whether TLS is used and/or 
	* authentication is done via user/password

* If TLS is used, the server certificate needs to be distributed separately and installed on the IOS device

* Data is published on the server under a user-configurable `topic` with
	* QoS and
	* Retain

* App listens to one subscription topic and receives publications with the specified QoS lebel

### Map Display

* Displays a map with the current location and marks the last ~50 locations with timestamp and topic

* Shows a button to center the map on the current location and follow the current user location

* Shows a button to zoom out until all marked locations of user and friends are displayed.

* Connection indicator light shows current status of the MQTT server connection
  The server connection is established automatically when a new location shall be published.
  When the applicationo is moved to background, the connection is disconnected.
	* BLUE=IDLE, no connection established
	* GREEN=CONNECTED, server connection established
	* AMBER=ERROR OCCURED, WAITING FOR RECONNECT, app will automatically try to reconnect to the server
	* RED=ERROR, no connection to the server possible or transient errror condition

### Move mode

The standard tracking mode  reports significant location changes only (>500m and at most once every 5 minutes). This is defined by Apple and is optimal with respect to battery usage.

A user now switch to Move Mode by tapping the little wooden locomotive. In Move Mode location changes are reported in user specified intervals every x meters or every y seconds. The payoff is higher battery usage as high as in navigation or tracker app. So it is recommend to use Move Mode while charging or during moves only.


### Background Mode

The Application supports background-mode
* "significant location changes" are automatically published to the MQTT server
* app shows an application badge indicating the number of sent location updates since the app went into background mode
* app shows notifications (in notification center) when publishing the user's location

### UI buttons

1. First Button is User Tracking Mode: Follow with Heading, follow view north, show all friends, don't follow
2. Second Button is Map Mode: Map, Satellite, Hybrid
3. Third Button: Pin to publish manually (purple color), automatic publishes are red pins
4. Forth Button: Publish mode: manually (empty circle), normal (filled circle), move mode (car)
5. Fifth Button: Connection on/off (reconnect) and colored indicator for connection status

Activity Indicator on right side of map in the middle of the screen. Indicates
queued or sent messages not acknowledged yet by server. If not visible, all
publishes are sent and acknowledged.

Status: shows URL and last error (invalid user or password, etc)
Might show topic, subscription, ... in future

### Friends

Friends: shows self and all Friends published by the server, their picture or MQTTitude default icon, and their last location.
Tapping on the entry switches back to map centered on friend's last location.

Tapping on the disclosure indicator (little right-arrow) lists all received locations of the friend.
App keeps track of last 100 automatic own locations and 3 locations of others. Tapping on location entry centers the map on the selected location.

If the location was set manually, a disclosure indicator allows changing the remark of the location (e.g. parked my car here!, started my run here!). Manual locations can only be deleted manually.

Deleting locations or all locations of a friend is done by left-swipe on the entry.

Q. As regards the friends list: when is reverse geo-coding done?

Reverse geo-coding is done when displaying/editing location details by either
* tapping the info button of a location callout on the map or
* navigating via friends/locations/...

No geo-coding is done automatically in the background to limit mobile data usage.

### Friendly faces

If your iOS addressbook has an entry for, say, Jane Jolie, and Jane has an image associated with her addressbook entry, MQTTitude will show Jane's image on the map and on the Friends list, as soon as a location update for Jane is seen by the app.

In order to associate an MQTT topic with our friend (Jane, in this case), edit your addressbook entry on iOS for Jane and do either of the following:

* Create a new relashionship (like 'Spouse') called `MQTTitude` (case insensitive), and add your friend's topic name to that (e.g. `mqttitude/jane/loc`)

The addressbook API might need a while to be refreshed, but you may be able to speed that up by swiping the Friends list downward until the activity indicator appears, then let go).

or...

Tap the Bookmark Button in the Navigation Bar on the Location screen. Select an entry from your Address Book.
The entry will be marked with a relationship to the current friend's topic name.

Beginning with iPhone app version 5.1, there's now a switch in Expert Mode settings titled _Update Address Book". If this is _on_, the app uses related names from the address book to identify topics and updates address book as described above. If the switch is _off_, the app maintains an internal reference to the address book record and doesn't modify the address book.


### Region Monitoring and Waypoints

For all manually published locations, a description, a region radius and a share flag can be edited.

Setting the description of a location helps you to remember places.

If a description is entered and the share flag set, the location is published to the MQTT broker as a `waypoint` once.
Waypoints shared by other users are displayed on the map and are visible in the Friends/Locations... table.

If the description is non-empty and a radius > 0 (meters) is set, the app starts monitoring the circular region around the coordinate. A region may be 'waypoint'.
The regions are shown on the map as blue-ish circles. If the device is within a region, the corresponding circle turns red-ish.
Everytime the devices enters or leaves a monitored region, an additional location message is published to the MQTT broker.

Use cases:
* Define a `home` region to insure that the device publishes a new location when coming home or leaving home even if you do not move more than 500m.
* Share your favorite places with your friends ("Best Sushi in Town").
* Keep a private note ("Parked Car here").


### Settings

	Field							Default			Expert	Remarks
	
	DeviceID						none			no		
	
	Minimum Distance in Move Mode	200m			no		in Move Mode, app publishes when travelled 200m
	Minimum Time in Move Mode		180sec			no		in Move Mode, app publishes every 180 seconds
	
	ClientID						$UserId/$DeviceID Yes	If both not set, IOSDeviceName
	host							<none>			no		IP or name
	port							8883			yes
	TLS								YES				yes
	Authorize						YES				yes
	UserID							<none>			no
	Password						<none>			no
	
	Update Address Book				YES				yes	
	Subscription					mqttitude/#		yes
	Subscription QOS				1				yes
	
	Topic-Name						mqttitude/$UserId/$DeviceId			yes	If both not set, IOSDeviceName
	QOS								1				yes
	Retain							YES				yes
	
	Clean Session					NO				yes
	Keep Alive						60sec			yes
	WillTopic Name					$Topic-Name		yes
	Will							lwt				yes
	Will QOS						1				yes
	Will Retain						NO				yes
	
	Monitoring						1				n/a					0 = no monitorint, 1 = standard, 2 = move mode

### Remote Configuration

Getting bored and frustrated by telling other people how to edit settings on their iPhone to connect to your server?

Here's what you can do with iPhone App Version >= 5.1:

* Create a configuration file in JSON with a .mqtc suffix (for MQTTitude Configuration).
* Send the file to the device via email, http, dropbox, ... you name it. 
* Open the file on the device with MQTTitude

Here is a sample configuration file which follows the settings as explaind abov. Boolean values are represented as
0/1 for FALSE/TRUE. The _type entry is mandatory, all other entries are optional.

	{
		"_type": "configuration",
		"deviceid": "iphone",
    	"clientid": "",
		"subscription": "mqttitude/#",
    	"topic": "",
    	"host": "broker.my.net",
    	"user": "user",
    	"pass": "passed",
        "will": "lwt",
        "willtopic": "",
        "subscriptionqos": "1",
        "qos": "1",
        "port": "8883",
        "keepalive": "60",
        "willqos": "1",
        "retain": "1",
        "tls": "1",
        "auth": "1",
        "clean": "0",
        "willretain": "0",
        "mindist": "200",
        "mintime": "180",
        "monitoring": "1",
        "ab": "0"
	}
