package gov.modadw.issuer.service.did;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DidKeyUtilsTest {

  // Sample P-256 JWK public key
  private static final Map<String, Object> SAMPLE_P256_JWK =
      Map.of(
          "kty", "EC",
          "crv", "P-256",
          "x", "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
          "y", "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0");

  @Test
  void testGenerateDidKey() {
    String didKey = DidKeyUtils.generateDidKey(SAMPLE_P256_JWK);
    assertNotNull(didKey);
    assertTrue(didKey.startsWith("did:key:z"));
  }

  @Test
  void testBuildDidDocument() {
    Map<String, Object> doc = DidKeyUtils.buildDidDocument(SAMPLE_P256_JWK);

    assertNotNull(doc);
    assertTrue(doc.containsKey("@context"));
    assertTrue(doc.containsKey("id"));
    assertTrue(doc.containsKey("verificationMethod"));
    assertTrue(doc.containsKey("authentication"));
    assertTrue(doc.containsKey("assertionMethod"));

    String id = (String) doc.get("id");
    assertTrue(id.startsWith("did:key:z"));
  }

  @Test
  void testCompressP256PublicKey() {
    byte[] compressed = DidKeyUtils.compressP256PublicKey(SAMPLE_P256_JWK);
    assertEquals(33, compressed.length);
    assertTrue(compressed[0] == 0x02 || compressed[0] == 0x03);
  }

  @Test
  void testMissingCoordinatesThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DidKeyUtils.generateDidKey(Map.of("kty", "EC", "crv", "P-256")));
  }
}
