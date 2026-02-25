package com.example.entroya

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import androidx.lifecycle.LifecycleOwner
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
            Toast.makeText(this, "Este dispositivo no tiene NFC", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val clockInButton: Button = findViewById(R.id.button_clock_in)
        val clockOutButton: Button = findViewById(R.id.button_clock_out)
        val adminButton: Button = findViewById(R.id.button_admin)
        val userIdEditText: EditText = findViewById(R.id.edittext_user_id)
        val selectedUserNameTextView: TextView = findViewById(R.id.text_selected_user_name)
        val nfcStatusTextView: TextView = findViewById(R.id.text_nfc_status)

        // Mostrar estado de NFC
        actualizarEstadoNfc(nfcStatusTextView)

        // Escuchar los cambios en el campo de texto del ID (para pruebas manuales)
        userIdEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.findUserById(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Observar el usuario encontrado
        lifecycleScope.launch {
            viewModel.foundUser.collectLatest { user ->
                if (user != null) {
                    selectedUserNameTextView.text = "👤 Usuario: ${user.nombre} (${user.rol})"
                } else {
                    selectedUserNameTextView.text = "👤 Usuario: -"
                }
            }
        }

        // Botones manuales
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

        // Observar eventos de UI
        lifecycleScope.launch {
            viewModel.uiEvents.collect { message ->
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun actualizarEstadoNfc(textView: TextView) {
        if (nfcAdapter?.isEnabled == true) {
            textView.text = if (modoAsignacion) {
                "📱 MODO ASIGNACIÓN: Acerca la tarjeta a asignar"
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
        // Activar modo asignación
        modoAsignacion = true
        nfcIdPendiente = null

        val nfcStatusTextView = findViewById<TextView>(R.id.text_nfc_status)
        actualizarEstadoNfc(nfcStatusTextView)

        // Mostrar diálogo de asignación
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_asignar_nfc, null)
        val textNfcId = dialogView.findViewById<TextView>(R.id.text_nfc_id)
        val spinnerUsuarios = dialogView.findViewById<Spinner>(R.id.spinner_usuarios)
        val btnAsignar = dialogView.findViewById<Button>(R.id.btn_asignar)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btn_cancelar)

        // Observar la lista de usuarios desde el ViewModel
        viewModel.userListLiveData.observe(this) { usuarios ->
            val adapter = UsuarioSpinnerAdapter(this, usuarios)
            spinnerUsuarios.adapter = adapter
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancelar.setOnClickListener {
            modoAsignacion = false
            nfcIdPendiente = null
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

            // Asignar NFC al usuario
            lifecycleScope.launch {
                val result = viewModel.asignarNfcAUsuario(usuario.id, nfcId)
                if (result) {
                    Toast.makeText(this@MainActivity, "✅ Tarjeta asignada a ${usuario.nombre}", Toast.LENGTH_LONG).show()
                    modoAsignacion = false
                    nfcIdPendiente = null
                    actualizarEstadoNfc(findViewById(R.id.text_nfc_status))
                    dialog.dismiss()
                } else {
                    Toast.makeText(this@MainActivity, "❌ Error al asignar tarjeta", Toast.LENGTH_LONG).show()
                }
            }
        }

        dialog.setOnDismissListener {
            modoAsignacion = false
            nfcIdPendiente = null
            actualizarEstadoNfc(findViewById(R.id.text_nfc_status))
        }

        dialog.show()
    }

    // Manejar NFC cuando la app está en primer plano
    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    // Procesar la tarjeta NFC cuando se detecta
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                procesarTarjetaNfc(it)
            }
        }
    }

    private fun procesarTarjetaNfc(tag: Tag) {
        val tagId = bytesToHex(tag.id)

        if (modoAsignacion) {
            // Estamos en modo asignación
            nfcIdPendiente = tagId
            Toast.makeText(this, "📱 Tarjeta detectada: $tagId", Toast.LENGTH_SHORT).show()

            // Actualizar el texto en el diálogo si está abierto
            val textNfcId = findViewById<TextView>(R.id.text_nfc_id)
            textNfcId?.text = "ID: $tagId"
        } else {
            // Modo normal de fichaje
            Toast.makeText(this, "📱 Tarjeta detectada: $tagId", Toast.LENGTH_SHORT).show()
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