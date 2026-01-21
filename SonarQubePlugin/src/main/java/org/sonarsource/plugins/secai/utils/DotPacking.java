package org.sonarsource.plugins.secai.utils;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

public final class DotPacking {
  private DotPacking() {}

  public static String toGzipBase64(String s) {
    if (s == null || s.isEmpty()) return "";
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
        gz.write(s.getBytes(StandardCharsets.UTF_8));
      }
      return Base64.getEncoder().encodeToString(bos.toByteArray());
    } catch (Exception e) {
      // fallback: uncompressed base64 (still avoids JSON escaping issues)
      return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
  }
}

