import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import org.apache.commons.codec.binary.*;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;


public class CommonUltis{
	/** Do not move it to ResourceConstants **/
	static final String SYM_KEY_HEX = "000102030405060708090A0B0C0D0E0F"
	public static String decrypt(final String ivAndEncryptedMessageBase64) {
        final byte[] symKeyData = DatatypeConverter.parseHexBinary(CommonUltis.SYM_KEY_HEX);
        final byte[] ivAndEncryptedMessage = DatatypeConverter.parseBase64Binary(ivAndEncryptedMessageBase64);
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final int blockSize = cipher.getBlockSize();

            // create the key
            final SecretKeySpec symKey = new SecretKeySpec(symKeyData, "AES");

            // retrieve random IV from start of the received message
            final byte[] ivData = new byte[blockSize];
            System.arraycopy(ivAndEncryptedMessage, 0, ivData, 0, blockSize);
            final IvParameterSpec iv = new IvParameterSpec(ivData);

            // retrieve the encrypted message itself
            final byte[] encryptedMessage = new byte[ivAndEncryptedMessage.length - blockSize];
            System.arraycopy(ivAndEncryptedMessage, blockSize, encryptedMessage, 0, encryptedMessage.length);

            cipher.init(Cipher.DECRYPT_MODE, symKey, iv);

            final byte[] encodedMessage = cipher.doFinal(encryptedMessage);

            // concatenate IV and encrypted message
            final String message = new String(encodedMessage, Charset.forName("UTF-8"));
            return message;
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("key argument does not contain a valid AES key");
        } catch (BadPaddingException e) {
            return null;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unexpected exception during decryption", e);
        }
    }

	public static String encrypt(final String plainMessage) {
        final byte[] symKeyData = DatatypeConverter.parseHexBinary(CommonUltis.SYM_KEY_HEX);
        final byte[] encodedMessage = plainMessage.getBytes(Charset.forName("UTF-8"));
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            int blockSize = cipher.getBlockSize();

            // create the key
            SecretKeySpec symKey = new SecretKeySpec(symKeyData, "AES");

            // generate random IV using block size
            byte[] ivData = new byte[blockSize];
            SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
            rnd.nextBytes(ivData);
            IvParameterSpec iv = new IvParameterSpec(ivData);

            cipher.init(Cipher.ENCRYPT_MODE, symKey, iv);

            byte[] encryptedMessage = cipher.doFinal(encodedMessage);

            // concatenate IV and encrypted message
            byte[] ivAndEncryptedMessage = new byte[ivData.length + encryptedMessage.length];
            System.arraycopy(ivData, 0, ivAndEncryptedMessage, 0, blockSize);
            System.arraycopy(encryptedMessage, 0, ivAndEncryptedMessage, blockSize, encryptedMessage.length);

            String ivAndEncryptedMessageBase64 = DatatypeConverter.printBase64Binary(ivAndEncryptedMessage);

            return ivAndEncryptedMessageBase64;
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("key argument does not contain a valid AES key");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unexpected exception during encryption", e);
        }
    }

    /**
	* getPasswdFileName Get password file name
	* @param dbType
	* @param hostId
	* @param sId
	* @return pwdFileName
	*/
    public static String getPasswdFileName(dbType, hostId, sId) {
    	def pwdFileName = ".dbpasswd"
		if(hostId != null && hostId != "") {
			pwdFileName += "_" + hostId
		}
		pwdFileName += "_" + dbType
		if(sId != null && sId != "") {
			pwdFileName += "_" + sId
		}
		return pwdFileName
    }
}
