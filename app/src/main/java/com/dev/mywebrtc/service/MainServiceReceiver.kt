package com.dev.mywebrtc.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dev.mywebrtc.ui.CloseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainServiceReceiver:BroadcastReceiver() {

    @Inject lateinit var serviceRepository:MainServiceRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == MainServiceActions.ACTION_EXIT.name){
            //we want to exit the whole application
            serviceRepository.stopService()
            context?.startActivity(Intent(context,CloseActivity::class.java))
        }
    }
}