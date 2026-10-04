package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

final class CifradoPerfil {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final String PREFIJO_VERSION = "v1:";
    private static final int TAMANO_IV = 12;
    private static final int TAMANO_TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile SecretKeySpec clave;

    private CifradoPerfil() {
    }

    static void configurarClave(String claveBase64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(claveBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("app.crypto.perfil-key debe ser una clave Base64 válida de 256 bits", ex);
        }
        if (bytes.length != 32) {
            throw new IllegalStateException("app.crypto.perfil-key debe contener exactamente 32 bytes");
        }
        clave = new SecretKeySpec(bytes, "AES");
        Arrays.fill(bytes, (byte) 0);
    }

    static String cifrar(String valor) {
        byte[] iv = new byte[TAMANO_IV];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, claveRequerida(), new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            byte[] resultado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(cifrado, 0, resultado, iv.length, cifrado.length);
            return PREFIJO_VERSION + Base64.getEncoder().encodeToString(resultado);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No fue posible cifrar un dato del perfil infantil", ex);
        }
    }

    static String descifrar(String valor) {
        String contenido = valor.startsWith(PREFIJO_VERSION)
                ? valor.substring(PREFIJO_VERSION.length()) : valor;
        byte[] datos = Base64.getDecoder().decode(contenido);
        if (datos.length <= TAMANO_IV) {
            throw new IllegalStateException("El dato cifrado del perfil infantil está incompleto");
        }
        byte[] iv = Arrays.copyOfRange(datos, 0, TAMANO_IV);
        byte[] cifrado = Arrays.copyOfRange(datos, TAMANO_IV, datos.length);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, claveRequerida(), new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No fue posible descifrar un dato del perfil infantil", ex);
        }
    }

    static boolean estaCifradoEnFormatoActual(String valor) {
        return valor != null && valor.startsWith(PREFIJO_VERSION);
    }

    private static SecretKeySpec claveRequerida() {
        SecretKeySpec configurada = clave;
        if (configurada == null) {
            throw new IllegalStateException("No se configuró app.crypto.perfil-key");
        }
        return configurada;
    }
}
