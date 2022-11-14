package com.udacity.project4

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.LiveData

class FirebaseUserLiveData : LiveData<FirebaseUser?>()              /** As Repository **/
{
    // You can utilize the FirebaseAuth.AuthStateListener callback to get updates on the current Firebase user logged into the app.
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        value = firebaseAuth.currentUser
    }

    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onActive() {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onInactive() {
        firebaseAuth.removeAuthStateListener(authStateListener)
    }
}