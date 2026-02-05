package com.example.entroya

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.entroya.ui.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val clockInButton: Button = findViewById(R.id.button_clock_in)
        val clockOutButton: Button = findViewById(R.id.button_clock_out)
        val userIdEditText: EditText = findViewById(R.id.edittext_user_id)
        val selectedUserNameTextView: TextView = findViewById(R.id.text_selected_user_name)

        // 1. Escuchar los cambios en el campo de texto del ID
        userIdEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Llamamos al ViewModel para que busque el usuario
                viewModel.findUserById(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 2. Observar el usuario encontrado y actualizar la UI
        lifecycleScope.launch {
            viewModel.foundUser.collectLatest { user ->
                if (user != null) {
                    selectedUserNameTextView.text = "Usuario: ${user.nombre}"
                } else {
                    selectedUserNameTextView.text = "Usuario: -"
                }
            }
        }

        clockInButton.setOnClickListener {
            viewModel.onClockInClicked()
        }

        clockOutButton.setOnClickListener {
            viewModel.onClockOutClicked()
        }

        lifecycleScope.launch {
            viewModel.uiEvents.collect { message ->
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
