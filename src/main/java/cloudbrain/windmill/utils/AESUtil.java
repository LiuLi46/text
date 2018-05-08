package cloudbrain.windmill.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class AESUtil {  //��ʼ����
  public static final String VIPARA = "aabtjkddeeffgghh";   //AES Ϊ16bytes. DES Ϊ8bytes

  //���뷽ʽ
  public static final String bm = "UTF-8";

  //˽Կ
  private static final String ASE_KEY = "aabbchjgeeffgghh";   //AES�̶���ʽΪ128/192/256 bits.����16/24/32bytes��DES�̶���ʽΪ128bits����8bytes��

  /**
   * ����
   *
   * @param cleartext
   * @return
   */
  public static String encrypt(String cleartext) {
    //���ܷ�ʽ�� AES128(CBC/PKCS5Padding) + Base64, ˽Կ��aabbccddeeffgghh
    try {
      IvParameterSpec zeroIv = new IvParameterSpec(VIPARA.getBytes());
      //������������һ��Ϊ˽Կ�ֽ����飬 �ڶ���Ϊ���ܷ�ʽ AES����DES
      SecretKeySpec key = new SecretKeySpec(ASE_KEY.getBytes(), "AES");
      //ʵ���������࣬����Ϊ���ܷ�ʽ��Ҫдȫ
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); //PKCS5Padding��PKCS7PaddingЧ�ʸߣ�PKCS7Padding��֧��IOS�ӽ���
      //��ʼ�����˷������Բ������ַ�ʽ���������㷨Ҫ������ӡ���1���޵�����������2������������ΪSecureRandom random = new SecureRandom();��random�����������(AES���ɲ������ַ���)��3�����ô˴����е�IVParameterSpec
      cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
      //���ܲ���,���ؼ��ܺ���ֽ����飬Ȼ����Ҫ���롣��Ҫ����뷽ʽ��Base64, HEX, UUE,7bit�ȵȡ��˴�����������Ҫʲô���뷽ʽ
      byte[] encryptedData = cipher.doFinal(cleartext.getBytes(bm));

      return new BASE64Encoder().encode(encryptedData);
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  /**
   * ����
   *
   * @param encrypted
   * @return
   */
  public static String decrypt(String encrypted) {
    try {
      byte[] byteMi = new BASE64Decoder().decodeBuffer(encrypted);
      IvParameterSpec zeroIv = new IvParameterSpec(VIPARA.getBytes());
      SecretKeySpec key = new SecretKeySpec(
              ASE_KEY.getBytes(), "AES");
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      //�����ʱ��ͬMODE:Cipher.DECRYPT_MODE
      cipher.init(Cipher.DECRYPT_MODE, key, zeroIv);
      byte[] decryptedData = cipher.doFinal(byteMi);
      return new String(decryptedData, bm);
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }
}
