package test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

public class GoogleOtpTest {

	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException {
		String code = "708833";
		String encodedKey = "UE2RUIQCCTDEUNJE55O5322OBXF7CGCG";
		
		long codeCheck = Integer.parseInt(code);
		long l = new Date().getTime();
		long ll =  l / 30000;
		 
		boolean check_code = false;
		check_code = check_code(encodedKey, codeCheck, ll);
		
		System.out.println(check_code);
	}
	
	private static boolean check_code(String secret, long code, long t) throws InvalidKeyException, NoSuchAlgorithmException {
		Base32 codec = new Base32();
		byte[] decodedKey = codec.decode(secret);
		int window = 3;
		for (int i = -window; i <= window; ++i) {
			long hash = verify_code(decodedKey, t + i);
		
			if (hash == code) {
				return true;
			}
		}
		
		return false;
	}
	
	private static int verify_code(byte[] key, long t) throws NoSuchAlgorithmException, InvalidKeyException{
		  byte[] data = new byte[8];
		  long value = t;
		  for (int i = 8; i-- > 0; value >>>= 8) {
		      data[i] = (byte) value;
		  }
		 
		  SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
		  Mac mac = Mac.getInstance("HmacSHA1");
		  mac.init(signKey);
		  byte[] hash = mac.doFinal(data);
		 
		  int offset = hash[20 - 1] & 0xF;
		 
		  long truncatedHash = 0;
		  for (int i = 0; i < 4; ++i) {
		      truncatedHash <<= 8;
		      truncatedHash |= (hash[offset + i] & 0xFF);
		  }
		 
		  truncatedHash &= 0x7FFFFFFF;
		  truncatedHash %= 1000000;
		 
		  return (int) truncatedHash;
		}

}
