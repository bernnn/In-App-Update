package com.inapp.update

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

class SplashScreenActivity : AppCompatActivity() {

    lateinit var appUpdateManager: AppUpdateManager
    val MY_REQUEST_CODE = 10001
    var forceUpdateVersion = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 60
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config)
        remoteConfig.fetchAndActivate()

        forceUpdateVersion = remoteConfig.getLong("force_update_version")
        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installListener)

        checkUpdate()
    }


    override fun onResume() {
        super.onResume()
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        MY_REQUEST_CODE
                    );
                }
            }
    }

    override fun onStop() {
        super.onStop()
        removeUpdateListener()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_REQUEST_CODE){
            when {
                resultCode == RESULT_OK -> {
                    Toast.makeText(this,"Update flow success! Result code: "+ resultCode,
                        Toast.LENGTH_SHORT).show()
                    Log.e("onActivityResult","Update flow success! Result code: " + resultCode)
                }
                requestCode == RESULT_CANCELED -> {
                    Toast.makeText(this,"Result Cancelled",
                        Toast.LENGTH_SHORT).show()
                    Log.e("onActivityResult", "" + "Result Cancelled")
                }
                requestCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    Toast.makeText(this,"UPDATE FAILED",
                        Toast.LENGTH_SHORT).show()
                    Log.e("onActivityResult", "" + "UPDATE FAILED")
                }
            }
        }
    }

    private fun checkUpdate(){
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        // Checks that the platform will allow the specified type of update.
        Log.e("checkUpdate", "Checking for updates")
        Toast.makeText(this,"Checking for updates", Toast.LENGTH_SHORT).show()
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                forceUpdateVersion > BuildConfig.VERSION_CODE){

                showGenericDialog("Update", "New version is available") {

                    if(appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        Log.e("checkUpdate", "Update available --AppUpdateType.FLEXIBLE")
                        requestUpdate(appUpdateInfo)
                    }else {
                        Log.e("checkUpdate", "Update available --AppUpdateType.IMMEDIATE")
                        requestUpdateImmediate(appUpdateInfo)
                    }
                }
            } else {

                // update not available

                if(forceUpdateVersion > BuildConfig.VERSION_CODE  ){
                    showGenericDialog("Update", "No new version available") {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                }else{
                    Toast.makeText(this,"Normal app flow",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }




    private fun requestUpdate(appUpdateInfo: AppUpdateInfo){
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.FLEXIBLE,
            this,
            MY_REQUEST_CODE)
    }

    private fun requestUpdateImmediate(appUpdateInfo: AppUpdateInfo){
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.IMMEDIATE,
            this,
            MY_REQUEST_CODE)
    }



    private val installListener: InstallStateUpdatedListener = InstallStateUpdatedListener { installState ->
        if (installState.installStatus() == InstallStatus.DOWNLOADED) {
            Log.e("installListener", "An update has been downloaded")

            //install update
            appUpdateManager.completeUpdate()

        } else if(installState.installStatus() == InstallStatus.INSTALLED){

            removeUpdateListener()
            Log.e("installListener", "An update has been installed")
            Toast.makeText(this,"An update has been installed",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun removeUpdateListener(){
        appUpdateManager.unregisterListener(installListener)
    }

    private fun showGenericDialog(title: String, textMessage: String, action: () -> Unit?) {
        AlertDialog.Builder(this, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
            .setTitle(title)
            .setMessage(textMessage)
            .setCancelable(false)
            .setPositiveButton("Okay") { dialog, _ ->
                action.invoke()
                dialog.dismiss()
            }
            .create()
            .show()
    }

}