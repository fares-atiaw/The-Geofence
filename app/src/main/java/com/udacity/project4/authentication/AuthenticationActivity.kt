package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    val TAG = "AuthenticationActivity"
    private val viewModel by viewModels<AuthenticationViewModel>()
    private lateinit var binding: ActivityAuthenticationBinding

    private val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build(),
        AuthUI.IdpConfig.GoogleBuilder().build()
    )

    // Create a sign-in intent
    private val loginIntent = AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setIsSmartLockEnabled(false)
        .setAvailableProviders(providers)
        .build()

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // After the user finishes the transaction/intent
            val response = IdpResponse.fromResultIntent(it.data)
            if (it.resultCode == Activity.RESULT_OK) {
                // User successfully signed in
                Log.i(TAG, "Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}!")
            }
            else {
                // Sign in failed. If response is null the user canceled the sign-in flow using the back button.
                // Otherwise check response.getError().getErrorCode() and handle the error.
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)

        binding.btnLogin.setOnClickListener {
            getResult.launch(loginIntent)
        }

//       TODO Done 1 : Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google
//       TODO Done 2 : If the user was authenticated, send him to RemindersActivity
        viewModel.authenticationState.observe(this) { authenticationState ->
            when (authenticationState) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED ->{
                    startActivity(Intent(this, RemindersActivity::class.java))
                    finish()
                }

                AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED ->
                    Log.w(TAG, "User is UNAUTHENTICATED")

                else -> Log.e(
                    TAG,
                    "New $authenticationState state that doesn't require any UI change"
                )
            }
        }
//          Nope : a bonus is to customize the sign in flow to look nice using :
        //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout
    }

}