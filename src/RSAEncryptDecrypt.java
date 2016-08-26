/**
 * Created by rodrigoguimaraes on 2016-08-25.
 */
import java.io.*;
import java.security.*;
import javax.crypto.Cipher;

//BASED ON: http://www.devmedia.com.br/criptografia-assimetrica-criptografando-e-descriptografando-dados-em-java/31213
public class RSAEncryptDecrypt {

    /**
     * Local da chave privada no sistema de arquivos.
     */
    public static String PrivateKeyPath;

    /**
     * Local da chave pública no sistema de arquivos.
     */
    public static String PublicKeyPath;

    public static final String ALGORITHM = "RSA";

    /**
     * Gera a chave que contém um par de chave Privada e Pública usando 1025 bytes.
     * Armazena o conjunto de chaves nos arquivos private.key e public.key
     */
    public static KeyPair createKeys() 
    {
        KeyPair key = null;
        try
        {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(1024);
            key = keyGen.generateKeyPair();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return key;

    }

    /**
     * Criptografa o texto puro usando chave.
     */
    public static byte[] encrypt(String texto, Key chave) {
        byte[] cipherText = null;

        try {
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            // Criptografa o texto puro usando a chave
            cipher.init(Cipher.ENCRYPT_MODE, chave);
            cipherText = cipher.doFinal(texto.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cipherText;
    }

    /**
     * Decriptografa o texto puro usando chave.
     */
    public static String decrypt(byte[] texto, Key chave) {
        byte[] dectyptedText = null;

        try {
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            // Decriptografa o texto puro usando a chave
            cipher.init(Cipher.DECRYPT_MODE, chave);
            dectyptedText = cipher.doFinal(texto);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new String(dectyptedText);
    }
}
