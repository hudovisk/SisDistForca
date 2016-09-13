/**
 * Created by rodrigoguimaraes on 2016-08-25.
 */
import java.security.*;
import javax.crypto.Cipher;

//BASED ON: http://www.devmedia.com.br/criptografia-assimetrica-criptografando-e-descriptografando-dados-em-java/31213

/**
 * Class that encrypts and decrypts information by generating and using symmetric keys and the RSA algorithm.
 */
public class RSAEncryptDecrypt {

    public static final String ALGORITHM = "RSA";

    /**
     * Returns a KeyPair, which contains both the public and private key.
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
     * Uses a key to encrypt a text string. The key could be either
     * public or private.
     */
    public static byte[] encrypt(String texto, Key chave) {
        byte[] cipherText = null;

        try {
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            //Encrypts the pure text using a cipher
            cipher.init(Cipher.ENCRYPT_MODE, chave);
            cipherText = cipher.doFinal(texto.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cipherText;
    }

    /**
     * Decrypts an encrypted byte array and returns the
     * pure text string.
     */
    public static String decrypt(byte[] texto, Key chave) {
        byte[] dectyptedText = null;

        try {
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            //Decrypts the pure text using the key
            cipher.init(Cipher.DECRYPT_MODE, chave);
            dectyptedText = cipher.doFinal(texto);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new String(dectyptedText);
    }
}
