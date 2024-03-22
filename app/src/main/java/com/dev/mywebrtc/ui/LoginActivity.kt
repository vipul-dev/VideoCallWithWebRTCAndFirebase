package com.dev.mywebrtc.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dev.mywebrtc.databinding.ActivityLoginBinding
import com.dev.mywebrtc.repository.MainRepository
import com.dev.mywebrtc.utils.FirebaseFieldNames.USERNAME
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    @Inject
    lateinit var mainRepository: MainRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initBlock()
    }

    private fun initBlock() {
        binding.apply {
            btn.setOnClickListener {
                mainRepository.login(
                    usernameEt.text.toString(),
                    passwordEt.text.toString()
                ) { isDone, reason ->
                    if (!isDone) {
                        Toast.makeText(this@LoginActivity, reason, Toast.LENGTH_SHORT).show()
                    } else {
                        /*
                        * start to moving our main activity
                        * */
                        Toast.makeText(this@LoginActivity, reason, Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra(USERNAME, usernameEt.text.toString())
                        })
                    }
                }
            }
        }
    }
}