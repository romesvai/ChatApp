package com.romes.chatapp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.romes.chatapp.R
import com.romes.chatapp.databinding.ActivitySignInBinding
import com.romes.chatapp.utils.Constants




class SignInActivity : AppCompatActivity() {
    private var binding: ActivitySignInBinding?=null
    private lateinit var auth: FirebaseAuth
    private lateinit var signInClient : GoogleSignInClient
    private var isSigningOut: Boolean = false

    private val googleSignInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task= GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try{
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            }catch (e: ApiException){
                // Google Sign in Failed
                Log.w("failed","Google Sign In faield ",e)
            }

        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        if(intent.hasExtra(Constants.EXTRA_SIGN_OUT)){
            isSigningOut = intent.getBooleanExtra(Constants.EXTRA_SIGN_OUT,false)
        }
        if(isSigningOut){
            FirebaseAuth.getInstance().signOut()
        }

        binding?.ivGoogleLogo?.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build()
            signInClient = GoogleSignIn.getClient(this,gso)
            auth = Firebase.auth
            val signInIntent = signInClient.signInIntent
//            if(signInClient.asGoogleApiClient().isConnected){
//                signInClient.asGoogleApiClient().clearDefaultAccountAndReconnect()
//            }
            googleSignInLauncher.launch(signInIntent)

        }
    }
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?) {
        Log.d("signIn", "firebaseAuthWithGoogle:" + acct?.id)
        val credential = GoogleAuthProvider.getCredential(acct?.idToken,null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener(this) {
                Log.d("success", "signInWithCredential:success")
                startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener(this) { e -> // If sign in fails, display a message to the user.
                Log.w("failure", "signInWithCredential", e)
                Toast.makeText(
                    this@SignInActivity, "Authentication failed.",
                    Toast.LENGTH_SHORT
                ).show()
            }

    }
}