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
    public String PrivateKeyPath;

    /**
     * Local da chave pública no sistema de arquivos.
     */
    public String PublicKeyPath;

    public RSAEncryptDecrypt(String keyName)
    {
        PrivateKeyPath = "keys/" + keyName + "_private.key";
        PublicKeyPath = "keys/" + keyName + "_public.key";
    }

    public static final String ALGORITHM = "RSA";


    public void createKeys()
    {
        createKeysS(PrivateKeyPath, PublicKeyPath);
    }

    /**
     * Gera a chave que contém um par de chave Privada e Pública usando 1025 bytes.
     * Armazena o conjunto de chaves nos arquivos private.key e public.key
     */
    private static void createKeysS(String PrivateKeyPath, String PublicKeyPath) {
        try {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(1024);
            final KeyPair key = keyGen.generateKeyPair();

            File chavePrivadaFile = new File(PrivateKeyPath);
            File chavePublicaFile = new File(PublicKeyPath);

            // Cria os arquivos para armazenar a chave Privada e a chave Publica
            if (chavePrivadaFile.getParentFile() != null) {
                chavePrivadaFile.getParentFile().mkdirs();
            }

            chavePrivadaFile.createNewFile();

            if (chavePublicaFile.getParentFile() != null) {
                chavePublicaFile.getParentFile().mkdirs();
            }

            chavePublicaFile.createNewFile();

            // Salva a Chave Pública no arquivo
            ObjectOutputStream chavePublicaOS = new ObjectOutputStream(
                    new FileOutputStream(chavePublicaFile));
            chavePublicaOS.writeObject(key.getPublic());
            chavePublicaOS.close();

            // Salva a Chave Privada no arquivo
            ObjectOutputStream chavePrivadaOS = new ObjectOutputStream(
                    new FileOutputStream(chavePrivadaFile));
            chavePrivadaOS.writeObject(key.getPrivate());
            chavePrivadaOS.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean verifyKeysAlreadyInSO()
    {
        return verifyKeysAlreadyInSOS(PrivateKeyPath, PublicKeyPath);
    }

    /**
     * Verifica se o par de chaves Pública e Privada já foram geradas.
     */
    private static boolean verifyKeysAlreadyInSOS(String PrivateKeyPath, String PublicKeyPath) {

        File chavePrivada = new File(PrivateKeyPath);
        File chavePublica = new File(PublicKeyPath);

        if (chavePrivada.exists() && chavePublica.exists()) {
            return true;
        }

        return false;
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

    public PublicKey getPublicKey()
    {
        ObjectInputStream inputStream = null;

        try
        {
            inputStream = new ObjectInputStream(new FileInputStream(PublicKeyPath));
            return (PublicKey) inputStream.readObject();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public PrivateKey getPrivateKey()
    {
        ObjectInputStream inputStream = null;

        try
        {
            inputStream = new ObjectInputStream(new FileInputStream(PrivateKeyPath));
            return (PrivateKey) inputStream.readObject();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
