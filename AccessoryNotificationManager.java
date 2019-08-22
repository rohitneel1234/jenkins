/* DTS,INC.
 * 5220 LAS VIRGENES ROAD
 * CALABASAS, CA 91302  USA
 *
 * ©DTS, INC.  ALL RIGHTS RESERVED.
 *
 * THIS SOFTWARE, ANY COMPONENTS THEREOF, AND ANY RELATED DOCUMENTATION (THE “PRODUCT”)
 * CONTAINS CONFIDENTIAL PROPRIETARY INFORMATION OWNED BY DTS, INC. AND/OR ITS
 * AFFILIATES (“DTS”) INCLUDING BUT NOT LIMITED TO TRADE SECRETS, KNOW-HOW,
 * TECHNICAL, AND BUSINESS INFORMATION.  UNLESS OTHERWISE PROVIDED UNDER THE
 * TERMS OF A FULLY-EXECUTED WRITTEN AGREEMENT BY AND BETWEEN THE RECIPIENT
 * HEREOF AND DTS, ALL USE, DUPLICATION, DISCLOSURE, OR DISTRIBUTION OF THE
 * PRODUCT, IN ANY FORM, IS PROHIBITED AND IS A VIOLATION OF STATE, FEDERAL, AND
 * INTERNATIONAL LAWS. THE PRODUCT CONTAINS CONFIDENTIAL, PROPRIETARY TRADE SECRETS,
 * AND IS PROTECTED BY APPLICABLE COPYRIGHT LAW AND/OR PATENT LAW. BOTH CIVIL AND
 * CRIMINAL PENALTIES APPLY.
 *
 * ALGORITHMS, DATA STRUCTURES AND METHODS CONTAINED IN THE PRODUCT MAY BE
 * PROTECTED BY ONE OR MORE PATENTS OR PATENT APPLICATIONS. UNLESS OTHERWISE
 * PROVIDED UNDER THE TERMS OF A FULLY-EXECUTED WRITTEN AGREEMENT BY AND BETWEEN
 * THE RECIPIENT HEREOF AND DTS, THE FOLLOWING TERMS SHALL APPLY TO ANY USE OF
 * THE PRODUCT:  (I) USE OF THE PRODUCT IS AT THE RECIPIENT'S SOLE RISK; (II) THE
 * PRODUCT IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND AND DTS EXPRESSLY
 * DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * ANY IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE,
 * REGARDLESS OF WHETHER DTS KNOWS OR HAS REASON TO KNOW OF THE USER'S PARTICULAR
 * NEEDS; (III) DTS DOES NOT WARRANT THAT THE PRODUCT MEET USER'S REQUIREMENTS,
 * OR THAT ANY ALLEGED DEFECTS IN THE PRODUCT WILL BE CORRECTED; (IV) DTS DOES
 * NOT WARRANT THAT THE OPERATION OF ANY HARDWARE OR SOFTWARE ASSOCIATED WITH THE
 * PRODUCT WILL BE UNINTERRUPTED OR ERROR-FREE; AND (V) UNDER NO CIRCUMSTANCES,
 * INCLUDING NEGLIGENCE, SHALL DTS OR THE DIRECTORS, OFFICERS, EMPLOYEES, OR
 * AGENTS OF DTS, BE LIABLE TO USER FOR ANY INCIDENTAL, INDIRECT, SPECIAL, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING BUT NOT LIMITED TO DAMAGES FOR LOSS OF BUSINESS
 * PROFITS, BUSINESS INTERRUPTION, AND LOSS OF BUSINESS INFORMATION) ARISING OUT
 * OF THE USE, MISUSE, OR INABILITY TO USE THE PRODUCT.
 */

package com.dts.dtsxultra.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.dts.dtssdk.DtsFeature;
import com.dts.dtssdk.DtsManager;
import com.dts.dtssdk.DtsFeatureChecker;
import com.dts.dtssdk.accessory.Accessory;
import com.dts.dtssdk.accessory.AccessoryIcon;
import com.dts.dtssdk.accessory.ContentMode;
import com.dts.dtssdk.accessory.StereoPreference;
import com.dts.dtssdk.callback.GenericCallback;
import com.dts.dtssdk.callback.OnCompleteCallback;
import com.dts.dtssdk.result.DtsResult;
import com.dts.dtssdk.util.AudioRoute;
import com.dts.dtsxultra.R;
import com.dts.dtsxultra.activities.AccessorySelectionActivity;
import com.dts.dtsxultra.activities.MainActivity;
import com.dts.dtsxultra.constants.UxConstants;

/**
 * Helper class that shows an advanced notification with the currently selected accessory and
 * other options.
 */
public class AccessoryNotificationManager extends RemoteViews {

    private static final String TAG = AccessoryNotificationManager.class.getSimpleName();

    private static final int NOTIFICATION_ID = 7;

    private static AccessoryNotificationManager sInstance;
    private Notification.Builder mNotifyBuilder;
    private Notification mNotification;
    private NotificationModerator mNotificationModerator;

    // DTS states and selection cache, so that the notification doesn't have to call the SDK APIs
    // every time the notification is shown
    private Boolean mDtsEnabled = null;
    private StereoPreference mStereoPreferenceSelection = null;
    private ContentMode mContentModeSelection = null;
    public Accessory mLineoutAccessory = null;
    private Accessory mBluetoothAccessory = null;
    private Accessory mUsbAccessory = null;
    private AudioRoute mCurrentAudioRoute = null;
    private Accessory mInternalSpeakerAccessory = null;

    private AccessoryNotificationManager(String packageName, int layoutId, Context context) {
        super(packageName, layoutId);
       // int color =  ContextCompat.getColor(context, R.color.theme_red);
        mNotifyBuilder = new Notification.Builder(context).setContentIntent(getPendingIntentClick(context))
                .setContentText(context.getResources().getString(R.string.notification_title)).setSmallIcon(R.drawable.ic_headset_black_24dp);
        mNotifyBuilder.setDeleteIntent(getDismissIntent(context));
        if (NotificationPolicy.DISMISSIBLE) {
            mNotifyBuilder.setOngoing(false);
        } else {
            mNotifyBuilder.setOngoing(true);
        }
        if (Build.VERSION.SDK_INT >= 26) {
            mNotifyBuilder.setChannelId(DtsNotificationChannel.NOTIFICATION_CHANNEL_ID);
        }
        // Register pending intent for different notification click interactions
        setOnClickPendingIntent(R.id.layoutSoundChange, getPendingIntentExpand(context));
        setOnClickPendingIntent(R.id.imgToggle, getPendingIntentToggle(context));
        setOnClickPendingIntent(R.id.layoutSpeakerClose, getPendingIntentCollapse(context));
        setOnClickPendingIntent(R.id.layoutSpeakerChange, getPendingIntentChange(context));
        setOnClickPendingIntent(R.id.layoutHeadphoneChange, getPendingIntentChange(context));
        setOnClickPendingIntent(R.id.layoutWide, getPendingIntentWide(context));
        setOnClickPendingIntent(R.id.layoutFront, getPendingIntentFront(context));
        setOnClickPendingIntent(R.id.layoutTraditional, getPendingIntentTraditional(context));

        mNotificationModerator = new NotificationModerator();
    }

   /* public static AccessoryNotificationManager createAccessory(){

        return new AccessoryNotificationManager("com.dts.dtsxultra.util", R.layout.view_bluetooth_and_usb_notification,);

    }*/
    public void setAccessoryNotificationManager(AccessoryNotificationManager instance){

        sInstance=instance;

    }

    /**
     * Show the notification, with the currently selected accessory/content mode of the provided audio route.
     * The notification will also contain the current DTS state and stereo preference selection.
     * This function will automatically call various SDK APIs as needed, such as getDtsEnabled(),
     * to get DTS status so that the correct statuses get displayed. After all information has
     * been gathered, it will fire off a notification showing the active accessory/content mode along
     * with other DTS statuses.
     * Set sound to true and a sound might be heard as the notification is shown (depending on user setting).
     * Setting to false a sound will definitely not be heard.
     *
     * @param context
     * @param audioRoute
     * @param sound
     */
    public static void showNotification(final Context context, final AudioRoute audioRoute, final boolean sound) {
        if (audioRoute == null || audioRoute == AudioRoute.UNKNOWN) {
            Log.e(TAG, "Invalid audio route. Aborting showing notification");
            return;
        }
        /*
         * Function to hide layouts when DTS state is ON or OFF
         * */
        showHideNotification(context);

        final AccessoryNotificationManager accessoryNotificationManager = getInstance(context.getApplicationContext());

        // If any of the selection are null (not initialized yet), call DTS SDK API to get their state
        accessoryNotificationManager.cacheDtsStateIfNotInitialized();
        accessoryNotificationManager.cacheStereoPreferenceIfNotInitialized(context, audioRoute);

        // Cache active audio route
        accessoryNotificationManager.mCurrentAudioRoute = audioRoute;

        Accessory accessory = null;

        switch (audioRoute) {
            case LINE_OUT:
                accessory = accessoryNotificationManager.mLineoutAccessory;
                break;
            case BLUETOOTH:
                accessory = accessoryNotificationManager.mBluetoothAccessory;
                break;
            case USB:
                accessory = accessoryNotificationManager.mUsbAccessory;
                break;
            case INTERNAL_SPEAKERS:
                accessory = accessoryNotificationManager.mInternalSpeakerAccessory;
                DtsManager.getInstance().getContentMode(context,new OnCompleteCallback() {
                    @Override
                    public void onComplete(DtsResult dtsResult) {
                        if (!dtsResult.isResultOk()) {
                            Log.e(TAG, "Getting content mode failed.");
                            Log.e(TAG, "DTS returned error code: " + dtsResult.getResultCode() + ". " + dtsResult.getResultMessage());
                            return;
                        }

                        ContentMode contentMode =(ContentMode) dtsResult.getData();
                        accessoryNotificationManager.mContentModeSelection = contentMode;
                        accessoryNotificationManager.mNotificationModerator.requestNotification(context, contentMode, sound);
                    }
                });
                break;
        }

        // For bluetooth, usb or lineout routes...
        if (audioRoute == AudioRoute.BLUETOOTH || audioRoute == AudioRoute.LINE_OUT || audioRoute == AudioRoute.USB ) {
            DtsManager.getInstance().getAccessory(context, audioRoute, new GenericCallback<Accessory>() {
                @Override
                public void onComplete(DtsResult dtsResult, final Accessory accessory) {
                    if (!dtsResult.isResultOk()) {
                        Log.e(TAG, "Getting headphone selection failed.");
                        Log.e(TAG, "DTS returned error code: " + dtsResult.getResultCode() + ". " + dtsResult.getResultMessage());
                        return;
                    }
                    if (audioRoute == AudioRoute.BLUETOOTH) {
                        accessoryNotificationManager.mBluetoothAccessory = accessory;
                    } else if (audioRoute == AudioRoute.LINE_OUT) {
                        accessoryNotificationManager.mLineoutAccessory = accessory;
                    } else if (audioRoute == AudioRoute.USB) {
                        accessoryNotificationManager.mUsbAccessory = accessory;
                    } else if(audioRoute == AudioRoute.INTERNAL_SPEAKERS){
                        accessoryNotificationManager.mInternalSpeakerAccessory = accessory;
                    }
                    accessoryNotificationManager.mNotificationModerator.requestNotification(context, accessory, sound);
                }
            });
        }
        setVisibilityInNotification(context);
    }

    /**
     * Current audio routes is internal speaker, and internal speaker is disabled in config file.
     * At this condition user should not be able to see change button in notification.
     * Function for managing the visibility of Change button in notification
     *
     * @param context
     *  */

    public static void setVisibilityInNotification(Context context)
    {
        if(!FeatureManager.hasSpeakerMode()) {
            AudioRoute CurrentAudioRoute = DtsManager.getInstance().getAudioRoute();
            if (CurrentAudioRoute == AudioRoute.INTERNAL_SPEAKERS) {
                getInstance(context).setViewVisibility(R.id.layoutHeadphoneChange, View.GONE);
                getInstance(context).setViewVisibility(R.id.layoutSpeakerChange, View.GONE);
            } else {
                getInstance(context).setViewVisibility(R.id.layoutHeadphoneChange, View.VISIBLE);
                getInstance(context).setViewVisibility(R.id.layoutSpeakerChange, View.VISIBLE);
            }
        }
    }


    /**
     * Updates the notification with the new DTS state.
     * If the notification is already active, then it will update the existing notification.
     * If the notification is not currently active, then it will cache the state so that when it becomes
     * active, it shows the correct state.
     *
     * @param context
     * @param enabled
     */
    public static void updateDtsState(Context context, boolean enabled) {
        // Cache the state
        AccessoryNotificationManager accessoryNotificationManager = getInstance(context.getApplicationContext());
        accessoryNotificationManager.mDtsEnabled = enabled;

        // If the notification is currently active, send a new notification to update the DTS state
        if (SharedPreferenceHelper.isNotificationActive(context)) {
            accessoryNotificationManager.updateNotificationDts(context, enabled, true);
        }
    }

    /**
     * Updates the notification with the new stereo preference selection.
     * If the notification is already active, then it will update the existing notification.
     * If the notification is not currently active, then it will cache the selection so that when it becomes
     * active, it shows the correct state.
     *
     * @param context
     * @param stereoPreference
     */
    public static void updateStereoPreferenceSelection(Context context, StereoPreference stereoPreference) {
        // Cache the selection
        AccessoryNotificationManager accessoryNotificationManager = getInstance(context.getApplicationContext());
        accessoryNotificationManager.mStereoPreferenceSelection = stereoPreference;

        // If the notification is currently active, send a new notification to update the stereo preference
        if (SharedPreferenceHelper.isNotificationActive(context)) {
            accessoryNotificationManager.updateNotificationStereoPreference(context, stereoPreference, true);
        }
    }

    /**
     * Updates the notification with the new content mode selection.
     * If the notification is already active, then it will update the existing notification.
     * If the notification is not currently active, then it will cache the selection so that when it becomes
     * active, it shows the correct state.
     *
     * @param context
     * @param contentMode
     */
    public static void updateContentModeSelection(Context context, final ContentMode contentMode) {
        // Cache the selection
        final AccessoryNotificationManager accessoryNotificationManager = getInstance(context.getApplicationContext());
        accessoryNotificationManager.mContentModeSelection = contentMode;

        Log.d(TAG, "Currently active audio route: " + accessoryNotificationManager.mCurrentAudioRoute);

        if (accessoryNotificationManager.mCurrentAudioRoute == null) {
            // Cache audio route
            accessoryNotificationManager.mCurrentAudioRoute = DtsManager.getInstance().getAudioRoute();

            Log.d(TAG, "Currently active audio route updated to: " + accessoryNotificationManager.mCurrentAudioRoute);

            // If the notification is currently active && the active notification is for internal speakers
            if (SharedPreferenceHelper.isNotificationActive(context) &&
                    accessoryNotificationManager.mCurrentAudioRoute == AudioRoute.INTERNAL_SPEAKERS) {
                accessoryNotificationManager.mNotificationModerator.requestNotification(context, contentMode, false);
            }
        } else {
            // If the notification is currently active && the active notification is for internal speakers
            if (SharedPreferenceHelper.isNotificationActive(context) &&
                    accessoryNotificationManager.mCurrentAudioRoute == AudioRoute.INTERNAL_SPEAKERS) {
                accessoryNotificationManager.mNotificationModerator.requestNotification(context, contentMode, false);
            }
        }
    }

    /**
     * Updates the notification with the new accessory selection.
     * If the notification is already active, then it will update the existing notification.
     * If the notification is not currently active, then it will cache the selection so that when it becomes
     * active, it shows the correct state.
     *
     * @param context
     * @param audioRoute
     * @param accessory
     */
    public static void updateAccessorySelection(Context context, final AudioRoute audioRoute, final Accessory accessory) {
        final AccessoryNotificationManager accessoryNotificationManager = getInstance(context);

        // Cache the selection
        if (audioRoute == AudioRoute.LINE_OUT) {
            accessoryNotificationManager.mLineoutAccessory = accessory;
        } else if (audioRoute == AudioRoute.BLUETOOTH) {
            accessoryNotificationManager.mBluetoothAccessory = accessory;
        } else if (audioRoute == AudioRoute.USB) {
            accessoryNotificationManager.mUsbAccessory = accessory;
        }else if (audioRoute == AudioRoute.INTERNAL_SPEAKERS){
            accessoryNotificationManager.mInternalSpeakerAccessory = accessory;
        }

        Log.d(TAG, "Currently active audio route: " + accessoryNotificationManager.mCurrentAudioRoute);

        if (accessoryNotificationManager.mCurrentAudioRoute == null) {
            // Cache the audio route
            accessoryNotificationManager.mCurrentAudioRoute = DtsManager.getInstance().getAudioRoute();

            Log.d(TAG, "Currently active audio route updated to: " + accessoryNotificationManager.mCurrentAudioRoute);

            // If the notification is currently active && the active notification is the same audio route
            if (SharedPreferenceHelper.isNotificationActive(context)
                    && accessoryNotificationManager.mCurrentAudioRoute == audioRoute) {
                accessoryNotificationManager.mNotificationModerator.requestNotification(context, accessory, false);
            }
        } else {
            // If the notification is currently active && the active notification is the same audio route
            if (SharedPreferenceHelper.isNotificationActive(context)
                    && accessoryNotificationManager.mCurrentAudioRoute == audioRoute) {
                accessoryNotificationManager.mNotificationModerator.requestNotification(context, accessory, false);
            } else {
                Log.d(TAG, "isNotificationActive: " + SharedPreferenceHelper.isNotificationActive(context) + " | " + audioRoute);
            }
        }
    }

    /**
     * Cancels any active notification managed by this class. If no notifications are active, will do nothing
     *
     * @param context
     */
    public static void cancelNotification(Context context) {
        Log.d(TAG, "Cancel notification");

        ((NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);

        SharedPreferenceHelper.setNotificationActive(context, false);
    }

    public static AccessoryNotificationManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AccessoryNotificationManager.class) {
                if (sInstance == null) {
                    sInstance = new AccessoryNotificationManager(context.getPackageName(), R.layout.view_notification, context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private PendingIntent getPendingIntentExpand(Context context) {
        Intent expandIntent = createExplicitIntentToNotificationReceiver(context, NotificationIntents.ACTION_SOUND_EXPAND);
        return PendingIntent.getBroadcast(context, 0, expandIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPendingIntentCollapse(Context context) {
        Intent collapseIntent = createExplicitIntentToNotificationReceiver(context, NotificationIntents.ACTION_SOUND_COLLAPSE);
        return PendingIntent.getBroadcast(context, 0, collapseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPendingIntentToggle(Context context) {
        Intent toggleIntent = createExplicitIntentToNotificationReceiver(context, NotificationIntents.ACTION_DTS_TOGGLE);
        return PendingIntent.getBroadcast(context, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPendingIntentChange(Context context) {
        Intent changeIntent = createExplicitIntentToNotificationReceiver(context, NotificationIntents.ACTION_CHANGE_HEADPHONE);
        return PendingIntent.getBroadcast(context, 0, changeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPendingIntentWide(Context context) {
        Intent wideIntent = createExplicitIntentToNotificationReceiver(context, NotificationIntents.ACTION_STEREO_WIDE);
        return PendingIntent.getBroadcast(context, 0, wideIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPendingIntentFront(Context context) {
        Intent frontIntent = createExplicitIntentToNotificationReceiver(context, NotificationIntents.ACTION_STEREO_FRONT);
        return PendingIntent.getBroadcast(context, 0, frontIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPendingIntentTraditional(Context context) {
        Intent traditionalIntent = createExplicitIntentToNotificationReceiver(context, NotificationIntents.ACTION_STEREO_TRADITIONAL);
        return PendingIntent.getBroadcast(context, 0, traditionalIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPendingIntentClick(Context context) {
        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getDismissIntent(Context context) {
        Intent intent = createExplicitIntentToNotificationReceiver(context, NotificationIntents.ACTION_NOTIFICATION_DISMISS);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /**
     * Gets the DTS status from the SDK and caches the result so that it can be used later
     * when displaying the notification
     */
    private void cacheDtsStateIfNotInitialized() {
        if (mDtsEnabled == null) {
            DtsResult<Boolean> resultBoolean = DtsManager.getInstance().getDtsEnabled();
            if (!resultBoolean.isResultOk()) {
                Log.e(TAG, "Getting DTS state failed: " + resultBoolean.getResultCode());
            }
            mDtsEnabled = resultBoolean.getData();
        }
    }

    /**
     * Gets the DTS stereo preference selection from the SDK and caches the selection so that
     * it can be used later when displaying the notification
     */
    private void cacheStereoPreferenceIfNotInitialized(final Context context, final AudioRoute route) {
        if (mStereoPreferenceSelection == null && DtsFeatureChecker.getInstance().hasFeature(DtsFeature.STEREO_PREFERENCE)) {
            if (mContentModeSelection == null) {
                DtsManager.getInstance().getContentMode(context,new OnCompleteCallback() {
                    @Override
                    public void onComplete(DtsResult dtsResult) {
                        if (!dtsResult.isResultOk()) {
                            Log.e(TAG, "Getting content mode failed.");
                            Log.e(TAG, "DTS returned error code: " + dtsResult.getResultCode() + ". " + dtsResult.getResultMessage());
                            return;
                        }

                        mContentModeSelection = (ContentMode) dtsResult.getData();
                        DtsResult<StereoPreference> resultStereoPreference = DtsManager.getInstance().getStereoPreference(route, mContentModeSelection);
                        if (!resultStereoPreference.isResultOk()) {
                            Log.e(TAG, "Getting stereo preference selection failed: " + resultStereoPreference.getResultCode());
                        } else {
                            mStereoPreferenceSelection = resultStereoPreference.getData();
                        }

                    }
                });
            }else {
                DtsResult<StereoPreference> resultStereoPreference = DtsManager.getInstance().getStereoPreference(route, mContentModeSelection);
                if (!resultStereoPreference.isResultOk()) {
                    Log.e(TAG, "Getting stereo preference selection failed: " + resultStereoPreference.getResultCode());
                } else {
                    mStereoPreferenceSelection = resultStereoPreference.getData();
                }
            }

        }
    }

    /**
     * Show Stereo preference selection menu
     */
    private void expandSoundLayout(Context context) {
        setViewVisibility(R.id.layoutCollapseSound, View.GONE);
        setViewVisibility(R.id.layoutExpand, View.VISIBLE);
        setViewVisibility(R.id.layoutContentMode, View.VISIBLE);
        setViewVisibility(R.id.layoutSpeakerChange, View.VISIBLE);
        setViewVisibility(R.id.layoutSpeakerClose, View.VISIBLE);

        if (mNotification == null) {
            showNotification(context, DtsManager.getInstance().getAudioRoute(), false);
        } else {
            displayNotification(context.getApplicationContext(), false);
        }
    }

    /**
     * Show the current Stereo Preference icon and status
     */
    private void collapseSoundLayout(Context context) {
        setViewVisibility(R.id.layoutExpand, View.GONE);
        setViewVisibility(R.id.layoutCollapseSound, View.VISIBLE);
        setViewVisibility(R.id.layoutContentMode, View.GONE);

        if (mNotification == null) {
            showNotification(context.getApplicationContext(), DtsManager.getInstance().getAudioRoute(), false);
        } else {
            displayNotification(context, false);
        }

        displayNotification(context, false);
    }

    /**
     * Show a list of available headphones and content modes
     */
    private void onClickChange(Context context) {
        // [API-CALL] Read current active route and update UI
        AudioRoute audioRoute = DtsManager.getInstance().getAudioRoute();
        switch (audioRoute) {
            case LINE_OUT:
            case BLUETOOTH:
            case USB:
                launchHeadphoneChangeScreen(context, audioRoute);
                break;
            case INTERNAL_SPEAKERS:
                launchContentModeChangeScreen(context);
        }
    }

    /**
     * Called when the user presses a stereo preference in the notification tray.
     * The function will call setPreference() function in the SDK and update the notification tray
     * to show the currently selected preference.
     */
    private void onClickStereoPreference(Context context, StereoPreference stereoPreference) {
        Log.d(TAG, " onClickStereoPreference()");

        // Cache stereo preference state
        mStereoPreferenceSelection = stereoPreference;


        if(mCurrentAudioRoute == null){
           mCurrentAudioRoute = DtsManager.getInstance().getAudioRoute();
        }
        // Set stereo preference
        DtsResult result = DtsManager.getInstance().setStereoPreference(mCurrentAudioRoute, mContentModeSelection, stereoPreference);
        if (!result.isResultOk()) {
            Log.e(TAG, "Setting stereo preference returned: " + result.getResultCode() + " | " + result.getResultMessage());
        } else {
            // Send a broadcast requesting that UI be updated with DTS stereo preference selection
            Intent toggleIntent = new Intent(UxConstants.NOTIFICATION_UPDATE_EVENT);
            toggleIntent.putExtra(UxConstants.NOTIFICATION_STEREO_PREFERENCE_SELECTION, stereoPreference);
            LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(toggleIntent);

            updateNotificationStereoPreference(context, stereoPreference, true);
        }
    }

    /**
     * Called when the user presses the DTS on/off switch (or ImageButton).
     * The function will call setDtsEnabled() function in the SDK and update the notification
     * tray to show the updated status.
     */
    private void onClickDts(Context context) {
        Log.d(TAG, " onClickDts()");

        // If DTS state was never initialized, get it
        cacheDtsStateIfNotInitialized();

        // Swap state
        mDtsEnabled = !mDtsEnabled;

        String extra = mDtsEnabled ? "enable" : "disable";

        // Enable/disable DTS
        DtsResult result = DtsManager.getInstance().setDtsEnabled(context, mDtsEnabled);
        if (!result.isResultOk()) {
            Log.e(TAG, "Setting DTS enabled/disabled returned: " + result.getResultCode() + " | " + result.getResultMessage());
        } else {
            // Send a broadcast requesting that UI be updated with DTS state
            Intent toggleIntent = new Intent(UxConstants.NOTIFICATION_UPDATE_EVENT);
            toggleIntent.putExtra(UxConstants.NOTIFICATION_HEADPHONE_X_STATUS, extra);
            LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(toggleIntent);

            updateNotificationDts(context, mDtsEnabled, true);
        }
    }

    /**
     * Updates the notification tray with the DTS state.
     *
     * @param enabled true if DTS state should show as enabled
     *                false if DTS state should show as disabled
     * @param notify true if this function should fire off a notify() call, which will pop up the
     *               notification tray if not active
     *               false if this function should simply update the existing state and not pop up
     *               the notification tray if not active
     */
    private void updateNotificationDts(Context context, boolean enabled, boolean notify) {

        if (!enabled) {
            Log.d(TAG, " Switching toggle button off");
            setImageViewResource(R.id.imgToggle, R.drawable.dts_switch_off);
        } else {
            Log.d(TAG, " Switching toggle button on");
            setImageViewResource(R.id.imgToggle, R.drawable.dts_switch_on);
        }

        if (notify) {
            // The notification object may have been lost (new AccessoryNotificationManager instance).
            // This can happen when the user swipe-closes the app while the notification is still active.
            // When this happens, re-create the notification so that we can override it after that.
            if (mNotification == null) {
                showNotification(context.getApplicationContext(), DtsManager.getInstance().getAudioRoute(), false);
            } else {
                displayNotification(context, false);
            }
        }
    }

    /**
     * Updates the notification tray with the DTS stereo preference selection.
     *
     * @param stereoPreference stereo preference to update the notification tray to
     * @param notify true if this function should fire off a notify() call, which will pop up the
     *               notification tray if not active
     *               false if this function should simply update the existing state and not pop up
     *               the notification tray if not active
     */
    private void updateNotificationStereoPreference(Context context, StereoPreference stereoPreference, boolean notify) {

        if (stereoPreference == StereoPreference.WIDE) {
            SpannableString spannableString = new SpannableString(context.getResources().getString(R.string.preference_wide_title));
            StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
            spannableString.setSpan(styleSpan, 0, spannableString.length(), 0);
            setTextViewText(R.id.tvWide, spannableString);
            setTextViewText(R.id.tvFront, context.getResources().getString(R.string.preference_infront_title));
            setTextViewText(R.id.tvTraditional, context.getResources().getString(R.string.preference_traditional_title));
        } else if (stereoPreference == StereoPreference.FRONT) {
            SpannableString spannableString = new SpannableString(context.getResources().getString(R.string.preference_infront_title));
            StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
            spannableString.setSpan(styleSpan, 0, spannableString.length(), 0);
            setTextViewText(R.id.tvFront, spannableString);
            setTextViewText(R.id.tvTraditional, context.getResources().getString(R.string.preference_traditional_title));
            setTextViewText(R.id.tvWide, context.getResources().getString(R.string.preference_wide_title));
        } else {
            SpannableString spannableString = new SpannableString(context.getResources().getString(R.string.preference_traditional_title));
            StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
            spannableString.setSpan(styleSpan, 0, spannableString.length(), 0);
            setTextViewText(R.id.tvTraditional, spannableString);
            setTextViewText(R.id.tvFront, context.getResources().getString(R.string.preference_infront_title));
            setTextViewText(R.id.tvWide, context.getResources().getString(R.string.preference_wide_title));
        }

        if (notify) {
            // The notification object may have been lost (new AccessoryNotificationManager instance).
            // This can happen when the user swipe-closes the app while the notification is still active.
            // When this happens, re-create the notification so that we can override it.
            if (mNotification == null) {
                showNotification(context.getApplicationContext(), DtsManager.getInstance().getAudioRoute(), false);
            } else {
                displayNotification(context, false);
            }
        }
    }

    /**
     * Show a dialog for headphone selection
     */
    private void launchHeadphoneChangeScreen(Context context, AudioRoute audioRoute) {
        // Close notification drawer
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);

        Log.d(TAG, "Launching headphone change screen");

        // Show the accessory selection activity
        Intent intent = new Intent(context, AccessorySelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (audioRoute == AudioRoute.BLUETOOTH) {
            intent.putExtra(AccessorySelectionActivity.AUDIO_ROUTE, AccessorySelectionActivity.ROUTE_BLUETOOTH);
        } else if (audioRoute == AudioRoute.USB) {
            intent.putExtra(AccessorySelectionActivity.AUDIO_ROUTE, AccessorySelectionActivity.ROUTE_USB);
        } else {
            intent.putExtra(AccessorySelectionActivity.AUDIO_ROUTE, AccessorySelectionActivity.ROUTE_LINE_OUT);
        }
        context.startActivity(intent);
    }

    /**
     * Show a dialog for Content Mode selection
     */
    private void launchContentModeChangeScreen(Context context) {
        // Close notification drawer
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);

        Log.d(TAG, "Launching content mode change screen");

        // Show the Content Mode Change activity
        Intent intent = new Intent(context, AccessorySelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AccessorySelectionActivity.AUDIO_ROUTE, AccessorySelectionActivity.ROUTE_SPEAKER);
        context.startActivity(intent);
    }

    /**
     * This method hides the shows the layouts on basis of DTS State.
     */
    public static void showHideNotification(Context context) {

        if(!DtsManager.getInstance().getDtsEnabled().getData())
        {
            getInstance(context).setViewVisibility(R.id.layoutHeadphoneChange, View.GONE);
            getInstance(context).setViewVisibility(R.id.layoutSpeakerChange, View.GONE);
            getInstance(context).setViewVisibility(R.id.layoutSoundChange, View.GONE);
            getInstance(context).setViewVisibility(R.id.layoutWide, View.GONE);
            getInstance(context).setViewVisibility(R.id.layoutFront, View.GONE);
            getInstance(context).setViewVisibility(R.id.layoutTraditional, View.GONE);
            getInstance(context).setViewVisibility(R.id.layoutSpeakerClose, View.GONE);
        }
        else
        {
            getInstance(context).setViewVisibility(R.id.layoutHeadphoneChange, View.VISIBLE);
            getInstance(context).setViewVisibility(R.id.layoutSpeakerChange, View.VISIBLE);
            getInstance(context).setViewVisibility(R.id.layoutSoundChange, View.VISIBLE);
            getInstance(context).setViewVisibility(R.id.layoutWide, View.VISIBLE);
            getInstance(context).setViewVisibility(R.id.layoutFront, View.VISIBLE);
            getInstance(context).setViewVisibility(R.id.layoutTraditional, View.VISIBLE);
            getInstance(context).setViewVisibility(R.id.layoutSpeakerClose, View.VISIBLE);
        }
    }

    /**
     * Displays this instance's mNotification object by calling notify() function using
     * Android's NotificationManager. The caller must ensure that the mNotification object is
     * initialized properly prior to invoking this function. Otherwise, this function will do nothing.
     */
    private void displayNotification(Context context, boolean sound) {
        if (mNotification == null) {
            return;
        }
        /*
         * Function to hide layouts when DTS state is ON or OFF
         */
        showHideNotification(context);

        if(sound) {
            mNotifyBuilder.setDefaults(Notification.DEFAULT_SOUND);
        } else {
            mNotifyBuilder.setDefaults(0);
        }

        if (Build.VERSION.SDK_INT >= 24) {
            mNotifyBuilder.setCustomBigContentView(this);
        }

        mNotification = mNotifyBuilder.build();

        if (Build.VERSION.SDK_INT < 24) {
            mNotification.bigContentView = this;
        }

        NotificationSystemHelper.enableOrUpdateNotification(context, NOTIFICATION_ID, mNotification);


        SharedPreferenceHelper.setNotificationActive(context, true);
        setVisibilityInNotification(context);
    }

    /**
     * Convenient function to get the full name of an accessory.
     * The full name is BRAND_NAME ACCESSORY_NAME
     * If brand is null, ACCESSORY_NAME
     *
     * @param accessory to get the full name for
     * @return full name of the accessory
     */
    private String toFullAccessoryName(Accessory accessory) {
        if (accessory == null) {
            return null;
        }

        if (accessory.getBrandName() == null) {
            return accessory.getName();
        }

        return accessory.getBrandName() + " " + accessory.getName();
    }

    /**
     * A class to limit how frequently a notification is displayed. All request to display notification
     * should be done through this class's requestNotification() function so that the number of
     * requests can be moderated appropriately.
     *
     * The display notification requests need to be moderated because image data needs to be sent
     * to the Android system (so that image can be rendered in notification). If we send too many images
     * very frequently, it may overload the system with too much data, and can even lead to a crash.
     * To avoid flooding the system, this class provides a function to limit the number of calls to
     * the system, while still displaying the requested notification.
     */
    private class NotificationModerator {
        Handler mHandler = new Handler();
        NotificationRequest mLastRequest;
        boolean mIsInProgress;

        // Amount of time it waits (in milliseconds) before the notification is displayed
        private final int WAIT_DURATION_IN_MILLIS = 200;//ms

        /**
         * Class to contain data for a notification request
         */
        private class NotificationRequest {
            Context mContext;
            Object mAccessory; // or Content Mode
            boolean mSoundEnabled;
            boolean mIsHeadphone; // Whether or not this is a headphone or content mode

            NotificationRequest(Context context, ContentMode contentMode, boolean soundEnabled) {
                mContext = context;
                mAccessory = contentMode;
                mSoundEnabled = soundEnabled;
                mIsHeadphone = false;
            }

            NotificationRequest(Context context, Accessory accessory, boolean soundEnabled) {
                mContext = context;
                mAccessory = accessory;
                mSoundEnabled = soundEnabled;
                mIsHeadphone = true;
            }
        }

        /**
         * Requests a notification to be displayed with the specified Accessory info. The function will
         * wait for a set amount of time, and after the wait, the Accessory will be displayed in the
         * notification. If a new request is received while the function is still waiting, the previous
         * request will be dropped and the new request will take its place to be displayed once the wait
         * is over.
         *
         * @param context Android Activity, Application, or service context
         * @param accessory headphone to be displayed in notification
         * @param soundEnabled whether or not the sound should be played when the notification is displayed
         */
        void requestNotification(Context context, Accessory accessory, boolean soundEnabled) {
            // Save this request as the "last request"
            mLastRequest = new NotificationRequest(context, accessory, soundEnabled);
            executeDelayedNotification();
        }

        /**
         * Requests a notification to be displayed with the specified content mode info. The function will
         * wait for a set amount of time, and after the wait, the content mode will be displayed in the
         * notification. If a new request is received while the function is still waiting, the previous
         * request will be dropped and the new request will take its place to be displayed once the wait
         * is over.
         *
         * @param context Android Activity, Application, or service context
         * @param contentMode content mode to be displayed in notification
         * @param soundEnabled whether or not the sound should be played when the notification is displayed
         */
        void requestNotification(Context context, ContentMode contentMode, boolean soundEnabled) {
            // Save this request as the "last request"
            mLastRequest = new NotificationRequest(context, contentMode, soundEnabled);
            executeDelayedNotification();
        }

        /**
         * Initiates a delay for set amount of time (WAIT_DURATION_IN_MILLIS). After the delay is over,
         * the last notification request that was received will be processed and displayed as notification
         */
        void executeDelayedNotification() {
            // Check if runnable for sending notification is already running
            if (!mIsInProgress) {
                // Runnable is not running. Start new and trigger delay/wait
                mIsInProgress = true;
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mIsInProgress = false;
                        NotificationRequest lastRequest = mLastRequest;

                        // Call the respective function to create notification depending on which object
                        // the last notification request contains.
                        if (lastRequest.mIsHeadphone) {
                            createNotification(lastRequest.mContext, (Accessory) lastRequest.mAccessory, lastRequest.mSoundEnabled);
                        } else {
                            createNotification(lastRequest.mContext, (ContentMode) lastRequest.mAccessory, lastRequest.mSoundEnabled);
                        }
                    }
                }, WAIT_DURATION_IN_MILLIS);
            } // Else, the runnable for sending notification is already running. Let the runnable take care
            // of processing the last notification request. Do nothing for now
        }

        /**
         * Creates a notification for the accessory. This will retrieve the accessory information
         * from the AccessoryDatastore and send its info (such as headphone name and image) to Android
         * notification to be displayed immediately.
         *
         * @param context Android Activity, Application, or service context
         * @param accessory headphone to be displayed in notification
         * @param soundEnabled whether or not the sound should be played when the notification is displayed
         */
        private void createNotification(final Context context, final Accessory accessory, final boolean soundEnabled) {
            Log.d(TAG, "Creating notification for accessory");

            cacheDtsStateIfNotInitialized();

            // Build and update the notification with DTS state & stereo preference
            updateNotificationStereoPreference(context, mStereoPreferenceSelection, false);
            updateNotificationDts(context, mDtsEnabled, false);

            // If accessory is null, don't show any image
            if (accessory == null) {
                setViewVisibility(R.id.layoutExpand, View.GONE);
                setViewVisibility(R.id.layoutContentMode, View.GONE);
                setViewVisibility(R.id.layoutSubTitle, View.VISIBLE);
                setViewVisibility(R.id.layoutCollapseSound, View.VISIBLE);
                setImageViewResource(R.id.imgChangeHeadphone,R.drawable.ic_headset_black_24dp);

                // If stereo preference is an enabled feature, show menu to toggle stereo preference
                if (FeatureManager.hasStereoPreference()) {
                    setViewVisibility(R.id.layoutSoundChange, View.VISIBLE);
                } else {
                    // If disabled, hide menu
                    setViewVisibility(R.id.layoutSoundChange, View.GONE);
                }

                if (FeatureManager.hasDts()) {
                    setViewVisibility(R.id.imgToggle, View.VISIBLE);
                } else {
                    setViewVisibility(R.id.imgToggle, View.GONE);
                }

                String accessoryName = "No accessory selected";
                mNotifyBuilder.setContentTitle(accessoryName);
                mNotification = mNotifyBuilder.build();
                setTextViewText(R.id.audioTitle, accessoryName);

                // Build Notification with Notification Manager
                displayNotification(context, soundEnabled);

                return;
            }

            // Load the image that this accessory has
            accessory.getImageBytes(context, AccessoryIcon.SIZE_SMALL, true, new GenericCallback<byte[]>() {
                @Override
                public void onComplete(DtsResult dtsResult, byte[] bytes) {
                    // Check first to see if result is OK
                    if (dtsResult.isResultOk()) {
                        Log.d(TAG, "Showing notification for accessory");

                        Bitmap largeIcon = null;
                        if (bytes != null && accessory.getBrand()!= null) {

                            largeIcon = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        } else if (accessory.getCategory() != null) {

                            switch (accessory.getCategory()) {

                                case OVER_EAR:
                                    largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.dts_standard_overear_small);
                                    break;
                                case EAR_BUDS:
                                    largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.dts_standard_earbuds_small);
                                    break;
                                case EAR_PIECE:
                                    largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.dts_standard_earpiece_small);
                                    break;
                                case CAR_AUDIO:
                                    largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.dts_standard_caraudio_small);
                                    break;
                                default:
                                    largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.dts_standard_otheraudio_small);
                                    break;
                            }
                        }

                        if (largeIcon != null) {
                            mNotifyBuilder.setLargeIcon(largeIcon);
                            setImageViewBitmap(R.id.imgNotification, largeIcon);
                        }

                        setViewVisibility(R.id.layoutExpand, View.GONE);
                        setViewVisibility(R.id.layoutContentMode, View.GONE);
                        setViewVisibility(R.id.layoutSubTitle, View.VISIBLE);
                        setViewVisibility(R.id.layoutCollapseSound, View.VISIBLE);
                        setImageViewResource(R.id.imgChangeHeadphone,R.drawable.ic_headset_black_24dp);

                        // If stereo preference is an enabled feature, show menu to toggle stereo preference
                        if (FeatureManager.hasStereoPreference()) {
                            setViewVisibility(R.id.layoutSoundChange, View.VISIBLE);
                        } else {
                            // If disabled, hide menu
                            setViewVisibility(R.id.layoutSoundChange, View.GONE);
                        }

                        if (FeatureManager.hasDts()) {
                            setViewVisibility(R.id.imgToggle, View.VISIBLE);
                        } else {
                            setViewVisibility(R.id.imgToggle, View.GONE);
                        }

                        String accessoryName = toFullAccessoryName(accessory);
                        mNotifyBuilder.setContentTitle(accessoryName);
                        mNotification = mNotifyBuilder.build();
                        setTextViewText(R.id.audioTitle, accessoryName);

                        // Build Notification with Notification Manager
                        displayNotification(context, soundEnabled);

                    } else {
                        Log.e(TAG, "DTS returned error for loading image: " + dtsResult.getResultCode());
                        Log.e(TAG, dtsResult.getResultMessage());
                    }
                }
            });
        }

        /**
         * Creates a notification for the content mode. This will retrieve the content mode information
         * from the AccessoryDatastore and send its info (such as content mode name and image) to Android
         * notification to be displayed immediately.
         *
         * @param context Android Activity, Application, or service context
         * @param contentMode content mode to be displayed in notification
         * @param soundEnabled whether or not the sound should be played when the notification is displayed
         */
        private void createNotification(final Context context, final ContentMode contentMode, final boolean soundEnabled) {
            Log.d(TAG, "Creating notification for content mode");

            cacheDtsStateIfNotInitialized();

            // Build and update the notification with DTS state & stereo preference
            updateNotificationStereoPreference(context, mStereoPreferenceSelection, false);
            updateNotificationDts(context, mDtsEnabled, false);

            // If content mode is null, dont show any image
            if (contentMode == null) {
                setViewVisibility(R.id.layoutExpand, View.GONE);
                setViewVisibility(R.id.layoutContentMode, View.GONE);
                setViewVisibility(R.id.layoutSpeakerClose, View.GONE);
                setViewVisibility(R.id.layoutSpeakerChange, View.VISIBLE);
                setViewVisibility(R.id.layoutSubTitle, View.VISIBLE);
                setViewVisibility(R.id.layoutCollapseSound, View.VISIBLE);
                setImageViewResource(R.id.imgChangeHeadphone,R.drawable.ic_speaker_phone_black_24dp);

                // If stereo preference is an enabled feature, show menu to toggle stereo preference
                if (FeatureManager.hasStereoPreference()) {
                    setViewVisibility(R.id.layoutSoundChange, View.VISIBLE);
                } else {
                    // If disabled, hide menu
                    setViewVisibility(R.id.layoutSoundChange, View.GONE);
                }

                if (FeatureManager.hasDts()) {
                    setViewVisibility(R.id.imgToggle, View.VISIBLE);
                } else {
                    setViewVisibility(R.id.imgToggle, View.GONE);
                }

                mNotifyBuilder.setContentTitle("No content mode selected");
                mNotification = mNotifyBuilder.build();
                setTextViewText(R.id.audioTitle, "No content mode selected");

                // Build Notification with Notification Manager
                displayNotification(context, soundEnabled);

                return;
            }

            // Load the image that this content mode has
            contentMode.getImageBytes(context, AccessoryIcon.SIZE_SMALL, true, new GenericCallback<byte[]>() {
                @Override
                public void onComplete(DtsResult dtsResult, byte[] bytes) {
                    // Check first to see if result is OK
                    if (dtsResult.isResultOk()) {
                        Log.d(TAG, "Showing notification for content mode");

                        Bitmap largeIcon = null;
                        if (bytes != null) {
                            largeIcon = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        } else {
                            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.dts_standard_internalspeaker_phone_medium);
                        }
                        mNotifyBuilder.setLargeIcon(largeIcon);
                        setImageViewBitmap(R.id.imgNotification, largeIcon);

                        setViewVisibility(R.id.layoutExpand, View.GONE);
                        setViewVisibility(R.id.layoutContentMode, View.GONE);
                        setViewVisibility(R.id.layoutSpeakerClose, View.GONE);
                        setViewVisibility(R.id.layoutSpeakerChange, View.VISIBLE);
                        setViewVisibility(R.id.layoutSubTitle, View.VISIBLE);
                        setViewVisibility(R.id.layoutCollapseSound, View.VISIBLE);
                        setImageViewResource(R.id.imgChangeHeadphone,R.drawable.ic_speaker_phone_black_24dp);

                        // If stereo preference is an enabled feature, show menu to toggle stereo preference
                        if (FeatureManager.hasStereoPreference()) {
                            setViewVisibility(R.id.layoutSoundChange, View.VISIBLE);
                        } else {
                            // If disabled, hide menu
                            setViewVisibility(R.id.layoutSoundChange, View.GONE);
                        }

                        if (FeatureManager.hasDts()) {
                            setViewVisibility(R.id.imgToggle, View.VISIBLE);
                        } else {
                            setViewVisibility(R.id.imgToggle, View.GONE);
                        }

                        //TODO Replace with OEM defined string
                        mNotifyBuilder.setContentTitle(contentMode.getName());
                        mNotification = mNotifyBuilder.build();
                        setTextViewText(R.id.audioTitle, contentMode.getName());

                        // Build Notification with Notification Manager
                        displayNotification(context, soundEnabled);

                    } else {
                        Log.e(TAG, "DTS returned error for loading image: " + dtsResult.getResultCode());
                        Log.e(TAG, dtsResult.getResultMessage());
                    }
                }
            });
        }
    }

    private static Intent createExplicitIntentToNotificationReceiver(Context context, String action) {
        ComponentName componentName = new ComponentName(context.getApplicationContext().getPackageName(),
                AccessoryNotificationManager.NotificationReceiver.class.getName());
        Intent intent = new Intent(action);
        intent.setComponent(componentName);
        return intent;
    }



    /**
     * Broadcast Receiver to handle user actions from Notification tray and update the
     * necessary notification details.
     */
    public static class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equalsIgnoreCase(NotificationIntents.ACTION_DTS_TOGGLE)) {
                // DTS toggle
                getInstance(context.getApplicationContext()).onClickDts(context);
            } else if (action.equalsIgnoreCase(NotificationIntents.ACTION_SOUND_EXPAND)) {
                // Show the stereo preferences menu
                getInstance(context.getApplicationContext()).expandSoundLayout(context);
            } else if (action.equalsIgnoreCase(NotificationIntents.ACTION_SOUND_COLLAPSE)) {
                // Close the stereo preferences menu
                getInstance(context.getApplicationContext()).collapseSoundLayout(context);
            } else if (action.equalsIgnoreCase(NotificationIntents.ACTION_CHANGE_HEADPHONE)) {
                // Show switch headphone dialog
                getInstance(context.getApplicationContext()).onClickChange(context);
            } else if (action.equalsIgnoreCase(NotificationIntents.ACTION_STEREO_WIDE)) {
                // Set stereo preference as "WIDE"
                getInstance(context.getApplicationContext()).onClickStereoPreference(context, StereoPreference.WIDE);
            } else if (action.equalsIgnoreCase(NotificationIntents.ACTION_STEREO_FRONT)) {
                // Set stereo preference as "FRONT"
                getInstance(context.getApplicationContext()).onClickStereoPreference(context, StereoPreference.FRONT);
            } else if (action.equalsIgnoreCase(NotificationIntents.ACTION_STEREO_TRADITIONAL)) {
                // Set stereo preference as "TRADITIONAL"
                getInstance(context.getApplicationContext()).onClickStereoPreference(context, StereoPreference.TRADITIONAL);
            } else if (action.equals(NotificationIntents.ACTION_NOTIFICATION_DISMISS)) {
                // When the notification was dismissed
                SharedPreferenceHelper.setNotificationActive(context, false);
            }
        }
    }
}
