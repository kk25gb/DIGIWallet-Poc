package gov.modadw.issuer.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the Access-Token header for all callback endpoints from core-system. This filter
 * handles /api/issuer-ap/callback/** requests that use Access-Token authentication instead of JWT
 * Bearer.
 */
@Slf4j
@Component
public class AccessTokenFilter extends OncePerRequestFilter {

  private static final String ACCESS_TOKEN_HEADER = "Access-Token";
  private static final String MOCK_DID_SERVER_PATH_PREFIX = "/api/mock-did-server/";

  // TODO 應可以透過 API 通知 VC 設定此約定值，poc 先用預設
  @Value("${integration.callback.access-token:}")
  private String expectedAccessToken;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String requestUri = request.getRequestURI();

    if (!requestUri.startsWith(MOCK_DID_SERVER_PATH_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    // GET requests (get_issuer_info_did) from vc-handler CredentialService do NOT send
    // Access-Token, only POST requests (generate_did, create_did) from IssuerDidService do.
    if ("GET".equalsIgnoreCase(request.getMethod())) {
      log.debug("Allowing GET request without Access-Token: {}", requestUri);
      filterChain.doFilter(request, response);
      return;
    }

    String accessToken = request.getHeader(ACCESS_TOKEN_HEADER);

    if (expectedAccessToken == null || expectedAccessToken.isBlank()) {
      log.error("Access-token is not configured on server side");
      response.sendError(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Access token not configured");
      return;
    }

    if (accessToken == null || accessToken.isBlank()) {
      log.warn("Missing Access-Token header for callback: {}", requestUri);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Access-Token");
      return;
    }

    if (!expectedAccessToken.equals(accessToken.trim())) {
      log.warn("Invalid Access-Token for callback: {}", requestUri);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Access-Token");
      return;
    }

    log.debug("Access-Token validated for callback: {}", requestUri);
    filterChain.doFilter(request, response);
  }
}
