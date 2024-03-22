package com.dev.mywebrtc.firebaseclient

import com.dev.mywebrtc.utils.DataModel
import com.dev.mywebrtc.utils.FirebaseFieldNames.LATEST_EVENT
import com.dev.mywebrtc.utils.FirebaseFieldNames.PASSWORD
import com.dev.mywebrtc.utils.FirebaseFieldNames.STATUS
import com.dev.mywebrtc.utils.MyEventListener
import com.dev.mywebrtc.utils.UserStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor(
    private val dbRef: DatabaseReference,
    private val gson: Gson
) {
    private var currentUserName: String? = null
    private fun setUserName(username: String) {
        this.currentUserName = username
    }

    fun login(username: String, password: String, done: (Boolean, String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                /*
                * if the current user exist
                * */

                if (snapshot.hasChild(username)) {
                    /*
                    * check password
                    * */
                    val dbPassword = snapshot.child(username).child(PASSWORD).value
                    if (password == dbPassword) {
                        /*
                        * password is correct and sign in
                        * */

                        dbRef.child(username).child(STATUS).setValue(UserStatus.ONLINE)
                            .addOnCompleteListener {
                                setUserName(username)
                                done(true, "Sign in successfully")
                            }
                            .addOnFailureListener {
                                done(false, it.message)
                            }

                    } else {
                        /*
                        * password is wrong, notify user
                        * */
                        done(false, "Password is wrong")
                    }

                } else {
                    /*
                    *  user doesn't exist, register user
                    * */

                    dbRef.child(username).child(PASSWORD).setValue(password)
                        .addOnCompleteListener {
                            dbRef.child(username).child(STATUS).setValue(UserStatus.ONLINE)
                                .addOnCompleteListener {
                                    setUserName(username)
                                    done(true, "User created successfully.")
                                }
                                .addOnFailureListener {
                                    done(false, it.message)
                                }
                        }
                        .addOnFailureListener {
                            done(false, it.message)
                        }
                }
            }

        })

    }

    fun observeUsersStatus(status: (List<Pair<String, String>>) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                var list = snapshot.children.filter { it.key != currentUserName }.map {
                    it.key!! to it.child(STATUS).value.toString()
                }
                status(list)
            }
        })
    }


    fun subscribeForLatestEvent(listener: Listener) {
        try {
            dbRef.child(currentUserName!!).child(LATEST_EVENT)
                .addValueEventListener(object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)
                        val event = try {
                            gson.fromJson(snapshot.value.toString(), DataModel::class.java)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }

                        event?.let {
                            listener.onLatestEventReceived(it)
                        }
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    fun sendMessageToOtherClient(message: DataModel, success: (Boolean) -> Unit) {
        val convertedMessage = gson.toJson(message.copy(sender = currentUserName))
        dbRef.child(message.target!!).child(LATEST_EVENT).setValue(convertedMessage)
            .addOnCompleteListener { success(true) }
            .addOnFailureListener { success(false) }
    }

    fun changeMyStatus(status: UserStatus) {
        dbRef.child(currentUserName!!).child(STATUS).setValue(status.name)
    }

    fun clearLatestEvent() {
        dbRef.child(currentUserName!!).child(LATEST_EVENT).setValue(null)
    }

    fun logOff(function: () -> Unit) {
        dbRef.child(currentUserName!!).child(STATUS).setValue(UserStatus.OFFLINE)
            .addOnCompleteListener { function() }
    }


    interface Listener {
        fun onLatestEventReceived(event: DataModel)
    }
}