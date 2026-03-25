# EntroYa 🕒

EntroYa es una aplicación Android diseñada para simplificar el control de asistencia y fichaje de empleados. Utiliza tecnología NFC para un acceso rápido y seguro, además de permitir el fichaje manual y la gestión de usuarios por parte de administradores.

## ✨ Características

- **Fichaje con NFC**: Los empleados pueden fichar su entrada y salida simplemente acercando su tarjeta o dispositivo NFC.
- **Acceso Manual**: Opción de inicio de sesión mediante correo electrónico y contraseña para dispositivos sin NFC o en casos de olvido de la tarjeta.
- **Gestión de Administrador**: Panel protegido por contraseña para vincular nuevas tarjetas NFC a los empleados.
- **Sincronización en Tiempo Real**: Integración con **Supabase** para el almacenamiento seguro de datos de usuarios y registros de jornada.
- **Interfaz Intuitiva**: Diseño limpio y fácil de usar, adaptado con Edge-to-Edge para dispositivos modernos.

## 🛠️ Tecnologías Utilizadas

- **Lenguaje**: [Kotlin](https://kotlinlang.org/)
- **Arquitectura**: MVVM (Model-View-ViewModel)
- **Base de Datos y Autenticación**: [Supabase](https://supabase.com/)
- **Hardware**: Soporte para tecnología NFC (NfcAdapter)
- **Serialización**: Kotlinx Serialization
- **Corrutinas**: Para operaciones asíncronas de red.

## 🚀 Instalación y Configuración

1. **Clonar el repositorio**:
   ```bash
   git clone https://github.com/tu-usuario/EntroYa.git
   ```

2. **Configuración de Supabase**:
   Asegúrate de tener un proyecto en Supabase y crea las tablas necesarias (`usuarios` y `fichajes`). Configura las variables de entorno en tu archivo `local.properties` o `build.gradle.kts`:
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`

3. **Compilación**:
   Abre el proyecto en Android Studio y sincroniza con Gradle. Pulsa "Run" para instalar en tu dispositivo o emulador.

## 📱 Requisitos

- Android 5.0 (API 21) o superior.
- Dispositivo con lector NFC (opcional para el modo manual).

## 📝 Licencia

Este proyecto está bajo la Licencia MIT.
