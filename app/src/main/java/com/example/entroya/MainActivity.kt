package com.example.entroya

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    // NFC
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    // Variables para asignación NFC
    private var nfcIdPendiente: String? = null
    private var modoAsignacion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "Aviso: Dispositivo sin NFC. Usa el acceso manual.", Toast.LENGTH_LONG).show()
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Referencias del diseño actual
        val clockInButton: Button = findViewById(R.id.button_clock_in)
        val clockOutButton: Button = findViewById(R.id.button_clock_out)
        val adminButton: Button = findViewById(R.id.button_admin)
        val loginButton: Button = findViewById(R.id.button_login)
        val emailEditText: EditText = findViewById(R.id.edittext_email)
        val passwordEditText: EditText = findViewById(R.id.edittext_password)
        val selectedUserNameTextView: TextView = findViewById(R.id.text_selected_user_name)
        val nfcStatusTextView: TextView = findViewById(R.id.text_nfc_status)

        // Mostrar estado de NFC
        actualizarEstadoNfc(nfcStatusTextView)

        // Lógica de Login Manual
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val pass = passwordEditText.text.toString()
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                viewModel.loginManual(email, pass)
            } else {
                Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Observar el usuario identificado
        lifecycleScope.launch {
            viewModel.foundUser.collectLatest { user ->
                if (user != null) {
                    selectedUserNameTextView.text = "👤 Usuario: ${user.nombre} (${user.rol})"
                } else {
                    selectedUserNameTextView.text = "👤 Usuario: No identificado"
                }
            }
        }

        // Botones de fichaje (Entrada / Salida)
        clockInButton.setOnClickListener {
            viewModel.onClockInClicked()
        }

        clockOutButton.setOnClickListener {
            viewModel.onClockOutClicked()
        }

        // Botón de administración
        adminButton.setOnClickListener {
            mostrarDialogoAutenticacion()
        }

        // Mostrar mensajes (Toasts) del ViewModel
        lifecycleScope.launch {
            viewModel.uiEvents.collect { message ->
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun actualizarEstadoNfc(textView: TextView) {
        if (nfcAdapter == null) {
            textView.text = "📱 NFC no disponible en este dispositivo"
            return
        }
        if (nfcAdapter?.isEnabled == true) {
            textView.text = if (modoAsignacion) {
                "📱 MODO ASIGNACIÓN: Acerca la tarjeta"
            } else {
                "📱 NFC activado - Acerca tu tarjeta para fichar"
            }
        } else {
            textView.text = "⚠️ NFC desactivado - Actívalo en ajustes"
        }
    }

    private fun mostrarDialogoAutenticacion() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_auth, null)
        val editPassword = dialogView.findViewById<EditText>(R.id.edit_password)

        AlertDialog.Builder(this)
            .setTitle("👑 Autenticación de administrador")
            .setView(dialogView)
            .setPositiveButton("Confirmar", null)
            .setNegativeButton("Cancelar", null)
            .create().apply {
                setOnShowListener { dialogInterface ->
                    val button = (dialogInterface as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
                    button.setOnClickListener {
                        val password = editPassword.text.toString()
                        if (password == "1234") {
                            dismiss()
                            mostrarMenuAsignacion()
                        } else {
                            Toast.makeText(this@MainActivity, "❌ Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                show()
            }
    }

    private fun mostrarMenuAsignacion() {
        modoAsignacion = true
        nfcIdPendiente = null
        actualizarEstadoNfc(findViewById(R.id.text_nfc_status))

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_asignar_nfc, null)
        val textNfcId = dialogView.findViewById<TextView>(R.id.text_nfc_id)
        val spinnerUsuarios = dialogView.findViewById<Spinner>(R.id.spinner_usuarios)
        val btnAsignar = dialogView.findViewById<Button>(R.id.btn_asignar)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btn_cancelar)

        viewModel.userListLiveData.observe(this) { usuarios ->
            spinnerUsuarios.adapter = UsuarioSpinnerAdapter(this, usuarios)
        }

        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

        btnCancelar.setOnClickListener {
            modoAsignacion = false
            actualizarEstadoNfc(findViewById(R.id.text_nfc_status))
            dialog.dismiss()
        }

        btnAsignar.setOnClickListener {
            val usuario = spinnerUsuarios.selectedItem as? Usuario
            val nfcId = nfcIdPendiente
            if (usuario == null) {
                Toast.makeText(this, "❌ Selecciona un usuario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (nfcId == null) {
                Toast.makeText(this, "❌ Acerca una tarjeta NFC primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                if (viewModel.asignarNfcAUsuario(usuario.id, nfcId)) {
                    Toast.makeText(this@MainActivity, "✅ Tarjeta asignada", Toast.LENGTH_LONG).show()
                    modoAsignacion = false
                    actualizarEstadoNfc(findViewById(R.id.text_nfc_status))
                    dialog.dismiss()
                } else {
                    Toast.makeText(this@MainActivity, "❌ Error al asignar", Toast.LENGTH_LONG).show()
                }
            }
        }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let { procesarTarjetaNfc(it) }
        }
    }

    private fun procesarTarjetaNfc(tag: Tag) {
        val tagId = bytesToHex(tag.id)
        if (modoAsignacion) {
            nfcIdPendiente = tagId
            findViewById<TextView>(R.id.text_nfc_id)?.text = "ID: $tagId"
        } else {
            viewModel.procesarNfcTag(tagId)
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
