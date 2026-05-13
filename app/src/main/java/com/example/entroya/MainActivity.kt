package com.example.entroya

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.entroya.data.model.Usuario
import com.example.entroya.ui.MainViewModel
import com.example.entroya.ui.UsuarioSpinnerAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    // NFC
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    // Receiver para detectar cambios de estado de NFC en tiempo real
    private val nfcStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
                actualizarEstadoNfc()
                if (state == NfcAdapter.STATE_ON) {
                    // Si se activa, rehabilitamos el despacho en primer plano inmediatamente
                    nfcAdapter?.enableForegroundDispatch(this@MainActivity, pendingIntent, null, null)
                }
            }
        }
    }

    // UI Screens
    private lateinit var layoutNfc: LinearLayout
    private lateinit var layoutManual: LinearLayout
    private lateinit var layoutFeedback: LinearLayout
    private lateinit var textFeedback: TextView

    // Variables para asignación NFC
    private var nfcIdPendiente: String? = null
    private var modoAsignacion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencias de pantallas y feedback
        layoutNfc = findViewById(R.id.layout_nfc_screen)
        layoutManual = findViewById(R.id.layout_manual_screen)
        layoutFeedback = findViewById(R.id.layout_feedback_overlay)
        textFeedback = findViewById(R.id.text_feedback_message)

        // Navegación
        findViewById<TextView>(R.id.text_goto_manual).setOnClickListener {
            mostrarPantallaManual()
        }
        findViewById<TextView>(R.id.text_back_to_nfc).setOnClickListener {
            mostrarPantallaNfc()
        }
        findViewById<TextView>(R.id.text_goto_admin).setOnClickListener {
            mostrarDialogoAutenticacion()
        }

        layoutFeedback.setOnClickListener {
            layoutFeedback.visibility = View.GONE
        }

        // Inicializar NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        setupManualFichajeWatcher()

        lifecycleScope.launch {
            viewModel.uiEvents.collect { message ->
                mostrarFeedbackGigante(message)
                if (message.startsWith("✅")) {
                    limpiarCamposManual()
                    mostrarPantallaNfc()
                }
            }
        }
    }

    private fun mostrarFeedbackGigante(message: String) {
        textFeedback.text = message
        if (message.startsWith("✅")) {
            textFeedback.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        } else {
            textFeedback.setTextColor(android.graphics.Color.parseColor("#C62828"))
        }
        layoutFeedback.visibility = View.VISIBLE
        lifecycleScope.launch {
            delay(3000)
            layoutFeedback.visibility = View.GONE
        }
    }

    private fun mostrarPantallaManual() {
        layoutNfc.visibility = View.GONE
        layoutManual.visibility = View.VISIBLE
    }

    private fun mostrarPantallaNfc() {
        layoutManual.visibility = View.GONE
        layoutNfc.visibility = View.VISIBLE
    }

    private fun setupManualFichajeWatcher() {
        val emailEdit = findViewById<EditText>(R.id.edittext_email_manual)
        val passEdit = findViewById<EditText>(R.id.edittext_password_manual)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = emailEdit.text.toString()
                val pass = passEdit.text.toString()
                if (email.contains("@") && pass.length >= 4) {
                    viewModel.loginManualAndClock(email, pass)
                }
            }
        }
        emailEdit.addTextChangedListener(watcher)
        passEdit.addTextChangedListener(watcher)
    }

    private fun limpiarCamposManual() {
        findViewById<EditText>(R.id.edittext_email_manual).text.clear()
        findViewById<EditText>(R.id.edittext_password_manual).text.clear()
    }

    private fun actualizarEstadoNfc() {
        val textView = findViewById<TextView>(R.id.text_nfc_status)
        if (nfcAdapter == null) {
            textView.text = "📱 NFC no disponible"
            return
        }
        if (nfcAdapter?.isEnabled == true) {
            textView.text = if (modoAsignacion) "📱 MODO ASIGNACIÓN: Acerque tarjeta" else "📱 Acerque su tarjeta para fichar"
            textView.setTextColor(android.graphics.Color.parseColor("#1976D2"))
        } else {
            textView.text = "⚠️ Active el NFC para fichar"
            textView.setTextColor(android.graphics.Color.parseColor("#C62828"))
        }
    }

    private fun mostrarDialogoAutenticacion() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_auth, null)
        val editPassword = dialogView.findViewById<EditText>(R.id.edit_password)

        AlertDialog.Builder(this)
            .setTitle("Autenticación Admin")
            .setView(dialogView)
            .setPositiveButton("Entrar") { _, _ ->
                if (editPassword.text.toString() == "1234") {
                    mostrarMenuAsignacion()
                } else {
                    Toast.makeText(this, "❌ PIN Incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarMenuAsignacion() {
        modoAsignacion = true
        nfcIdPendiente = null
        actualizarEstadoNfc()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_asignar_nfc, null)
        val spinnerUsuarios = dialogView.findViewById<Spinner>(R.id.spinner_usuarios)
        val btnAsignar = dialogView.findViewById<Button>(R.id.btn_asignar)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btn_cancelar)

        viewModel.userListLiveData.observe(this) { usuarios ->
            spinnerUsuarios.adapter = UsuarioSpinnerAdapter(this, usuarios)
        }

        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

        btnCancelar.setOnClickListener {
            modoAsignacion = false
            actualizarEstadoNfc()
            dialog.dismiss()
        }

        btnAsignar.setOnClickListener {
            val usuario = spinnerUsuarios.selectedItem as? Usuario
            val nfcId = nfcIdPendiente
            if (usuario != null && nfcId != null) {
                lifecycleScope.launch {
                    if (viewModel.asignarNfcAUsuario(usuario.id, nfcId)) {
                        Toast.makeText(this@MainActivity, "✅ Vinculado", Toast.LENGTH_SHORT).show()
                        modoAsignacion = false
                        actualizarEstadoNfc()
                        dialog.dismiss()
                    }
                }
            } else {
                Toast.makeText(this, "Falta seleccionar usuario o leer tarjeta", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        // Registrar el receptor de cambios de estado NFC
        val intentFilter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        registerReceiver(nfcStateReceiver, intentFilter)
        
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
        actualizarEstadoNfc()
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar el receptor para evitar fugas de memoria
        unregisterReceiver(nfcStateReceiver)
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                val tagId = bytesToHex(it.id)
                if (modoAsignacion) {
                    nfcIdPendiente = tagId
                    Toast.makeText(this, "ID Detectado: $tagId", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.procesarNfcTag(tagId)
                }
            }
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }
}
