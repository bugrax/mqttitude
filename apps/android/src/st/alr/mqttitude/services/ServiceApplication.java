package st.alr.mqttitude.services;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import st.alr.mqttitude.ActivityMain;
import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.support.BackgroundPublishReceiver;
import st.alr.mqttitude.support.Contact;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import de.greenrobot.event.EventBus;

public class ServiceApplication extends ServiceBindable {
    private static SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
    private NotificationManager notificationManager;
    private static NotificationCompat.Builder  notificationBuilder;
    private static Class<?> locatorClass;
    private GeocodableLocation lastPublishedLocation;
    private Date lastPublishedLocationTime;
    private static ServiceApplication instance;
    private boolean even = false;
    private SimpleDateFormat dateFormater;
    private Handler handler;
    private static Map<String,Contact> contacts;
    
    private static ServiceLocator serviceLocator;
    private static ServiceMqtt serviceMqtt;

    
    @Override
    public void onCreate(){
        super.onCreate();
    }
    
    @Override
    protected void onStartOnce() {
        
        Log.v(this.toString(), "ServiceApplication starting");
        
        instance = this;

        contacts = new HashMap<String,Contact>();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                onHandlerMessage(msg);
            }
        };

        EventBus.getDefault().registerSticky(this);



        this.dateFormater = new SimpleDateFormat("H:m:s", getResources().getConfiguration().locale);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder (this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                if (key.equals(Defaults.SETTINGS_KEY_NOTIFICATION_ENABLED))
                    handleNotification();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener);
        handleNotification();

        ServiceConnection mqttConnection = new ServiceConnection() {
            
            @Override
            public void onServiceDisconnected(ComponentName name) {
                serviceMqtt = null;                
            }
            
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceMqtt = (ServiceMqtt) ((ServiceBindable.ServiceBinder)service).getService();                
            }
        };        
        bindService(new Intent(this, ServiceMqtt.class), mqttConnection, Context.BIND_AUTO_CREATE);

        
        ServiceConnection locatorConnection = new ServiceConnection() {
            

            @Override
            public void onServiceDisconnected(ComponentName name) {
                serviceLocator = null;                
            }
            
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                serviceLocator = (ServiceLocator) ((ServiceBindable.ServiceBinder)service).getService();                
            }
        };
        
        bindService(new Intent(this, ServiceLocatorFused.class), locatorConnection, Context.BIND_AUTO_CREATE);

        updateTicker("MQTTitude service started");
        
    }

       
    public String formatDate(Date d) {
        return dateFormater.format(d);
    }

    /**
     * @category NOTIFICATION HANDLING
     */
    private void handleNotification() {
        Log.v(this.toString(), "handleNotification()");
        stopForeground(true);
        if (notificationEnabled())
            createNotification();
    }

    private boolean notificationEnabled() {
        return sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_NOTIFICATION_ENABLED,
                Defaults.VALUE_NOTIFICATION_ENABLED);
    }

    private void createNotification() {
        Log.v(this.toString(), "createNotification");

        Intent resultIntent = new Intent(this, ActivityMain.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");

        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(resultPendingIntent);

        Intent intent = new Intent(Defaults.INTENT_ACTION_PUBLISH_LASTKNOWN);
        intent.setClass(this, BackgroundPublishReceiver.class);

        PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        notificationBuilder.addAction(
                R.drawable.ic_upload,
                "Publish location",
                pIntent);

        updateNotification();
    }

    public void updateTicker(String text) {
        notificationBuilder.setTicker(text + ((even = even ? false : true) ? " " : ""));
        notificationBuilder.setSmallIcon(R.drawable.ic_notification);
        notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());

        // if the notification is not enabled, the ticker will create an empty
        // one that we get rid of
        if (!notificationEnabled())
            notificationManager.cancel(Defaults.NOTIFCATION_ID);
    }

    public void updateNotification() {
        if (!notificationEnabled())
            return;

        String title = null;
        String subtitle = null;
        long time = 0;

        if (lastPublishedLocation != null
                && sharedPreferences.getBoolean("notificationLocation", true)) {
            time = lastPublishedLocationTime.getTime();

            if (lastPublishedLocation.getGeocoder() != null
                    && sharedPreferences.getBoolean("notificationGeocoder", false)) {
                title = lastPublishedLocation.toString();
            } else {
                title = lastPublishedLocation.toLatLonString();
            }
        } else {
            title = getString(R.string.app_name);
        }

        subtitle = ServiceLocator.getStateAsString() + " | " + ServiceMqtt.getStateAsString();

        notificationBuilder.setContentTitle(title);
        notificationBuilder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentText(subtitle)
                .setPriority(android.support.v4.app.NotificationCompat.PRIORITY_MIN);
        if (time != 0)
            notificationBuilder.setWhen(lastPublishedLocationTime.getTime());

        startForeground(Defaults.NOTIFCATION_ID, notificationBuilder.build());
       //notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());
    }

    public void onEventMainThread(Events.StateChanged.ServiceMqtt e) {
        updateNotification();
    }

    public void onEventMainThread(Events.StateChanged.ServiceLocator e) {
        updateNotification();
    }

    private void onHandlerMessage(Message msg) {
        switch (msg.what) {
            case ReverseGeocodingTask.GEOCODER_RESULT:
                geocoderAvailableForLocation(((GeocodableLocation) msg.obj));
                break;
        }
    }

    private void geocoderAvailableForLocation(GeocodableLocation l) {
        if (l == lastPublishedLocation) {
            Log.v(this.toString(), "Geocoder now available for lastPublishedLocation");
            updateNotification();
        } else {
            Log.v(this.toString(), "Geocoder now available for an old location");
        }
    }

    public void onEvent(Events.PublishSuccessfull e) {
        Log.v(this.toString(), "Publish successful");
        if (e.getExtra() != null && e.getExtra() instanceof GeocodableLocation) {
            GeocodableLocation l = (GeocodableLocation) e.getExtra();

            this.lastPublishedLocation = l;
            this.lastPublishedLocationTime = e.getDate();

            if (sharedPreferences.getBoolean("notificationGeocoder", false)
                    && l.getGeocoder() == null)
                (new ReverseGeocodingTask(this, handler)).execute(new GeocodableLocation[] {
                    l
                });

            updateNotification();

            if (sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_TICKER_ON_PUBLISH,
                    Defaults.VALUE_TICKER_ON_PUBLISH))
                updateTicker(getString(R.string.statePublished));

        }
    }
    
    

    public void onEvent(Events.LocationUpdated e) {
        if (e.getGeocodableLocation() == null)
            return;

        Log.v(this.toString(), "LocationUpdated: " + e.getGeocodableLocation().getLatitude() + ":"
                + e.getGeocodableLocation().getLongitude());
        
    }

    public boolean isDebugBuild() {
        return 0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE);
    }

    public static ServiceApplication getInstance() {
        return instance;
    }
    
    
    public ServiceLocator getServiceLocator() {
        return serviceLocator;        
    }
    
    public ServiceMqtt getServiceMqtt() {
        return serviceMqtt;
    }

    public static String getAndroidId() {
        return Secure.getString(instance.getContentResolver(), Secure.ANDROID_ID);
    }
    
    
    private Contact updateContact(String topic, GeocodableLocation location) {
        Contact c = contacts.get(topic);
        //Contact c = contactsAdapter.get(topic);

        if (c == null) {
            Log.v(this.toString(), "Allocating new contact for " + topic);
            c = new Contact(topic);
            Log.v(this.toString(), "looking for contact picture");
            findContactData(c);
        }

        c.setLocation(location);
        // Automatically fires onDatasetChanged of contacts adapter to update depending listViews
        //contactsAdapter.addItem(topic, c);
        contacts.put(topic, c);
            
        return c;
    }

    public void findContactData(Contact c){
        
        
        String imWhere = ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL + " = ? AND " + ContactsContract.CommonDataKinds.Im.DATA + " = ?";
        String[] imWhereParams = new String[] {"Mqttitude", c.getTopic() };
        Cursor imCur = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, imWhere, imWhereParams, null);
        
        while (imCur.moveToNext()) {
            Long cId = imCur.getLong(imCur.getColumnIndex(ContactsContract.Data.CONTACT_ID));                    
            Log.v(this.toString(), "found matching contact with id "+ cId + " to be associated with topic " + imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)));
            c.setUserImage(loadContactPhoto(getContentResolver(), cId));
            c.setName(imCur.getString(imCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));               
        }
        imCur.close();
        Log.v(this.toString(), "search finished");
        
}
    
    public static Bitmap loadContactPhoto(ContentResolver cr, long id) {
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        Log.v("loadContactPhoto", "using URI " + uri);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
        if (input == null) {
            return null;
        }
        return BitmapFactory.decodeStream(input);
    }


    


    
    
    public static Map<String, Contact> getContacts() {
        return contacts;
    }


    public void onEventMainThread(Events.ContactLocationUpdated e) {
        Log.v(this.toString(), "Contact location updated: " + e.getTopic() + " ->"
                + e.getGeocodableLocation().toString() + " @ "
                + new Date(e.getGeocodableLocation().getLocation().getTime() * 1000));

        // Updates a contact or allocates a new one
        Contact c = updateContact(e.getTopic(), e.getGeocodableLocation());

        
        
        // Fires a new event with the now updated or created contact to which fragments can react
        EventBus.getDefault().post(new Events.ContactUpdated(c));       
        
    }

    public void enableForegroundMode() {
            getServiceLocator().enableForegroundMode();
    }

    public void enableBackgroundMode() {
            getServiceLocator().enableBackgroundMode();
    }
    
    
}
