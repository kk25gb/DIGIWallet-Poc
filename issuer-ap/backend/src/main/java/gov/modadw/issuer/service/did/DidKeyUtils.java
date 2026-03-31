package gov.modadw.issuer.service.did;

import java.math.BigInteger;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility for generating did:key identifiers and DID Documents from P-256 public keys.
 *
 * <p>did:key method spec: the DID is derived from the public key itself. For P-256 (secp256r1):
 * multicodec prefix = 0x1200, then Base58btc encode with 'z' prefix.
 */
@Slf4j
public final class DidKeyUtils {

  private static final String DID_KEY_PREFIX = "did:key:";

  // Multicodec for P-256 public key (compressed): 0x1200
  private static final byte[] P256_MULTICODEC_PREFIX = new byte[] {(byte) 0x80, (byte) 0x24};

  // Base58 Bitcoin alphabet
  private static final char[] BASE58_ALPHABET =
      "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

  private DidKeyUtils() {}

  /**
   * Generate a did:key identifier from a P-256 JWK public key.
   *
   * @param publicKeyJwk JWK containing "x" and "y" coordinates (Base64url encoded)
   * @return did:key:z... identifier
   */
  public static String generateDidKey(Map<String, Object> publicKeyJwk) {
    byte[] compressedKey = compressP256PublicKey(publicKeyJwk);

    // Prepend multicodec prefix
    byte[] multicodecKey = new byte[P256_MULTICODEC_PREFIX.length + compressedKey.length];
    System.arraycopy(P256_MULTICODEC_PREFIX, 0, multicodecKey, 0, P256_MULTICODEC_PREFIX.length);
    System.arraycopy(
        compressedKey, 0, multicodecKey, P256_MULTICODEC_PREFIX.length, compressedKey.length);

    // Base58btc encode with 'z' prefix
    String multibaseEncoded = "z" + base58Encode(multicodecKey);

    return DID_KEY_PREFIX + multibaseEncoded;
  }

  /**
   * Build a complete DID Document from a P-256 JWK public key.
   *
   * @param publicKeyJwk JWK containing "kty", "crv", "x", "y"
   * @return DID Document as a Map
   */
  public static Map<String, Object> buildDidDocument(Map<String, Object> publicKeyJwk) {
    String didId = generateDidKey(publicKeyJwk);
    String verificationMethodId = didId + "#" + didId.substring(DID_KEY_PREFIX.length());

    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put(
        "@context",
        List.of("https://www.w3.org/ns/did/v1", "https://w3id.org/security/suites/jws-2020/v1"));
    doc.put("id", didId);

    // Verification method
    Map<String, Object> verificationMethod = new LinkedHashMap<>();
    verificationMethod.put("id", verificationMethodId);
    verificationMethod.put("type", "JsonWebKey2020");
    verificationMethod.put("controller", didId);
    verificationMethod.put("publicKeyJwk", publicKeyJwk);

    doc.put("verificationMethod", List.of(verificationMethod));
    doc.put("authentication", List.of(verificationMethodId));
    doc.put("assertionMethod", List.of(verificationMethodId));

    return doc;
  }

  /** Compress a P-256 public key from JWK x,y coordinates to 33-byte compressed form. */
  static byte[] compressP256PublicKey(Map<String, Object> jwk) {
    String xB64 = (String) jwk.get("x");
    String yB64 = (String) jwk.get("y");

    if (xB64 == null || yB64 == null) {
      throw new IllegalArgumentException("JWK must contain 'x' and 'y' coordinates");
    }

    byte[] x = Base64.getUrlDecoder().decode(xB64);
    byte[] y = Base64.getUrlDecoder().decode(yB64);

    // Determine prefix: 0x02 if y is even, 0x03 if y is odd
    byte prefix = (y[y.length - 1] & 1) == 0 ? (byte) 0x02 : (byte) 0x03;

    // Ensure x is exactly 32 bytes (pad with leading zeros if needed)
    byte[] compressed = new byte[33];
    compressed[0] = prefix;
    int xOffset = 32 - x.length;
    System.arraycopy(x, 0, compressed, 1 + xOffset, x.length);

    return compressed;
  }

  /** Base58 encode (Bitcoin variant). */
  static String base58Encode(byte[] input) {
    if (input.length == 0) return "";

    // Count leading zeros
    int leadingZeros = 0;
    for (byte b : input) {
      if (b == 0) leadingZeros++;
      else break;
    }

    BigInteger value = new BigInteger(1, input);
    StringBuilder sb = new StringBuilder();
    BigInteger base = BigInteger.valueOf(58);

    while (value.compareTo(BigInteger.ZERO) > 0) {
      BigInteger[] divmod = value.divideAndRemainder(base);
      value = divmod[0];
      sb.append(BASE58_ALPHABET[divmod[1].intValue()]);
    }

    // Add leading '1's for leading zero bytes
    for (int i = 0; i < leadingZeros; i++) {
      sb.append(BASE58_ALPHABET[0]);
    }

    return sb.reverse().toString();
  }
}
