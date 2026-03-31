# AP System Phase 1: Foundation & Admin Login - Implementation Plan

> **Version:** 1.1 (Revised)
> **Date:** 2026-03-12
> **Status:** Approved

---

## 1. Overview

設計一個與 `twdiw-vc-handler` 以及 `twdiw-oid4vci-handler` 協同運作的單一租戶 (Single-Tenant) 管理後台 AP System。
Phase 1 專注於：基礎 Monorepo 架構、登入與權限控管系統、以及與既有模組的整合預留。

---

## 2. Architecture Decisions

| # | Decision | Detail |
|---|----------|--------|
| 1 | Spring Boot Version | **3.3.13** (與 vc-handler 一致) |
| 2 | Server Port | **8080** (oid4vci-handler 已調整，不衝突) |
| 3 | Database | 共用 `modadw_issuer` DB，獨立 Schema **`issuer_ap`** |
| 4 | JWT | HS512 + `auth` claim，secret 獨立 (`AP_JWT_BASE64_SECRET`) |
| 5 | Roles | `ROLE_ADMIN`, `ROLE_OPERATOR` (獨立於既有模組) |
| 6 | API Prefix | `/api/issuer-ap/...` |
| 7 | Password Encoding | **BCrypt** (Spring Security 標準，不沿用 SHA-512) |
| 8 | Parent POM | **不繼承** `moda-digitalwallet-parent`，選擇性複用 Plugin |
| 9 | Code Reuse | 複用 vc-handler 的 Security 相關結構 (6 個檔案) |

---

## 3. Tech Stack

### 3.1 Backend (`/issuer-ap/backend`)

| Category | Choice |
|----------|--------|
| Framework | Spring Boot 3.3.13, Java 17 |
| Build | Maven (with wrapper) |
| Database | PostgreSQL (共用 `modadw_issuer`, Schema `issuer_ap`) |
| ORM | Spring Data JPA + Hibernate |
| DB Migration | Liquibase 4.32.0 |
| Security | Spring Security 6.x + Stateless JWT (HS512) |
| Password | BCryptPasswordEncoder |
| Mapping | MapStruct 1.5.5 |
| Utility | Lombok 1.18.32 |

### 3.2 Frontend (`/issuer-ap/frontend`)

| Category | Choice |
|----------|--------|
| Framework | Vue 3 + Vite |
| Language | TypeScript |
| UI | Element Plus |
| State | Pinia |
| Routing | Vue Router |
| HTTP | Axios |

---

## 4. POM Configuration (Backend)

### 4.1 Parent

直接繼承 `spring-boot-starter-parent:3.3.13`，不使用 `moda-digitalwallet-parent`。

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.13</version>
    <relativePath/>
</parent>

<groupId>gov.moda.dw.issuer.ap</groupId>
<artifactId>issuer-ap-backend</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

### 4.2 Dependencies

```xml
<!-- Core -->
spring-boot-starter-web
spring-boot-starter-security
spring-boot-starter-data-jpa
spring-boot-starter-validation
spring-boot-starter-oauth2-resource-server

<!-- Database -->
postgresql (runtime)
liquibase-core (4.32.0)

<!-- JWT -->
nimbus-jose-jwt (10.0.2)  <!-- 與 vc-handler 一致 -->

<!-- Utility -->
lombok (1.18.32)
mapstruct (1.5.5.Final)

<!-- Test -->
spring-boot-starter-test
spring-security-test
```

### 4.3 Plugins (從 parent POM 選擇性複用)

| Plugin | Version | 複用原因 |
|--------|---------|---------|
| **Spotless** (Google Java Format) | 2.43.0 | 統一程式碼風格，與既有模組一致 |
| **Maven Compiler** | 3.13.0 | 含 Lombok + MapStruct annotation processor 設定 |
| **Maven Surefire** | 3.2.5 | 排除 `*IT*`/`*IntTest*`，與既有測試命名慣例一致 |
| **Maven Failsafe** | 3.2.5 | 整合測試 `*IntTest*` pattern，與既有一致 |
| **Spring Boot Plugin** | (inherited) | `build-info` + `repackage` 標準打包 |

**不複用：** Checkstyle (JHipster 專用)、Git Commit ID (暫不需要)、JaCoCo (POC 不需覆蓋率)、Maven Enforcer (簡化為 Java 17 only)

---

## 5. Backend File Plan

### 5.1 Project Structure

```
issuer-ap/backend/
├── pom.xml
├── src/main/java/gov/moda/dw/issuer/ap/
│   ├── IssuerApApplication.java                          [NEW]
│   ├── config/
│   │   ├── SecurityConfiguration.java                    [REUSE] from vc-handler
│   │   ├── SecurityJwtConfiguration.java                 [REUSE] from vc-handler
│   │   ├── CustomJwtGrantedAuthoritiesConverter.java      [REUSE] from vc-handler
│   │   ├── ApplicationProperties.java                    [NEW]
│   │   ├── IntegrationProperties.java                    [NEW] 整合預留
│   │   ├── RestClientConfiguration.java                  [NEW] 整合預留
│   │   └── CorsConfiguration.java                        [NEW]
│   ├── domain/
│   │   ├── ApUser.java                                   [REUSE+MODIFY] from User.java
│   │   └── ApAuthority.java                              [REUSE+MODIFY] from Authority.java
│   ├── repository/
│   │   ├── ApUserRepository.java                         [NEW]
│   │   └── ApAuthorityRepository.java                    [NEW]
│   ├── security/
│   │   ├── SecurityUtils.java                            [REUSE] from vc-handler
│   │   ├── AuthoritiesConstants.java                     [REUSE+MODIFY]
│   │   └── DomainUserDetailsService.java                 [REUSE+MODIFY]
│   ├── service/
│   │   ├── AccountService.java                           [NEW]
│   │   └── integration/
│   │       ├── VcHandlerClient.java                      [NEW] 整合預留 (interface)
│   │       └── Oid4vciHandlerClient.java                 [NEW] 整合預留 (interface)
│   └── web/rest/
│       ├── AuthenticateController.java                   [NEW]
│       └── AccountController.java                        [NEW]
├── src/main/resources/
│   ├── config/
│   │   ├── application.yml                               [NEW]
│   │   └── application-dev.yml                           [NEW]
│   └── config/liquibase/
│       ├── master.xml                                    [NEW]
│       └── changelog/
│           └── 00000000000001_issuer_ap_initial_schema.xml [NEW]
└── src/test/java/gov/moda/dw/issuer/ap/
    └── web/rest/
        └── AuthenticateControllerIntTest.java            [NEW]
```

### 5.2 File Details

---

#### `[NEW] pom.xml`
- Parent: `spring-boot-starter-parent:3.3.13`
- GroupId: `gov.moda.dw.issuer.ap`
- 含 Section 4 所列之 dependencies 與 plugins

---

#### `[NEW] IssuerApApplication.java`
- `@SpringBootApplication`
- 標準 Spring Boot main class

---

#### `[REUSE+MODIFY] config/SecurityConfiguration.java`
**來源：** `vc-handler: SecurityConfiguration.java`

**修改項：**
- Package: `gov.moda.dw.issuer.ap.config`
- PasswordEncoder: 改為 `BCryptPasswordEncoder` (移除 `ModadwPasswordEncoder`)
- 移除 `AmsAuthenticationProvider` (AP 不需要 ExtendedUser/orgId 邏輯)
- `permitAll` endpoints 調整為：
  - `POST /api/issuer-ap/authenticate` → permitAll
  - 其餘 `/api/issuer-ap/**` → authenticated
- 移除 vc-handler 特有的 `/api/credential`, `/api/did`, `/api/status-list` 等 endpoint 設定
- 保留：CORS, CSRF disabled, stateless session, Bearer token, `jwtAuthenticationConverter()`

---

#### `[REUSE+MODIFY] config/SecurityJwtConfiguration.java`
**來源：** `vc-handler: SecurityJwtConfiguration.java`

**修改項：**
- Package: `gov.moda.dw.issuer.ap.config`
- `@Value` 來源改為 `${app.security.jwt.base64-secret}` (不使用 `jhipster.*` namespace)
- 保留：HS512 algorithm, NimbusJwtEncoder/Decoder, SecretKey 解析邏輯

---

#### `[REUSE] config/CustomJwtGrantedAuthoritiesConverter.java`
**來源：** `vc-handler: CustomJwtGrantedAuthoritiesConverter.java`

**修改項：** 僅改 package name，邏輯完全複用
- 解析 JWT `"auth"` claim (comma-separated)
- 轉換為 `SimpleGrantedAuthority` collection

---

#### `[NEW] config/ApplicationProperties.java`
- `@ConfigurationProperties(prefix = "app")`
- 包含 `security.jwt.*` 相關屬性的 type-safe binding

---

#### `[NEW] config/IntegrationProperties.java`
**用途：** 整合預留，Phase 1 只建立設定類別

```java
@ConfigurationProperties(prefix = "integration")
public class IntegrationProperties {
    private ServiceConfig vcHandler = new ServiceConfig();
    private ServiceConfig oid4vciHandler = new ServiceConfig();
    private int connectTimeout = 5000;
    private int readTimeout = 10000;

    public static class ServiceConfig {
        private String baseUrl;
    }
}
```

---

#### `[NEW] config/RestClientConfiguration.java`
**用途：** 整合預留，Phase 1 建立 RestTemplate Bean

```java
@Configuration
public class RestClientConfiguration {
    @Bean
    public RestTemplate restTemplate(IntegrationProperties props) {
        // 設定 connectTimeout, readTimeout
        // Phase 2+ 將注入到 VcHandlerClient / Oid4vciHandlerClient
    }
}
```

---

#### `[NEW] config/CorsConfiguration.java`
- 讀取 `app.cors.*` 設定
- Dev profile 允許 `http://localhost:5173` (Vite dev server)

---

#### `[REUSE+MODIFY] domain/ApUser.java`
**來源：** `vc-handler: User.java`

**修改項：**
- Table: `@Table(name = "ap_user", schema = "issuer_ap")`
- 移除 JHipster 特有欄位：`activationKey`, `resetKey`, `resetDate`, `imageUrl`, `langKey`
- 移除 `AbstractAuditingEntity` 繼承 (AP 不依賴 JHipster audit)
- 密碼欄位名改為 `passwordHash` (而非 `passw0rd`)
- 新增 `status` 欄位 (VARCHAR 20, 如 `ACTIVE`, `DISABLED`)
- 保留 `id`, `username` (原 login), `passwordHash`, `status`
- ManyToMany join table: `ap_user_authority` (schema: `issuer_ap`)

**簡化後欄位：**

| Column | Type | Note |
|--------|------|------|
| `id` | BIGINT (PK, sequence) | |
| `username` | VARCHAR(50), unique, not null | |
| `password_hash` | VARCHAR(60), not null | BCrypt output = 60 chars |
| `status` | VARCHAR(20), not null | `ACTIVE` / `DISABLED` |
| `created_date` | TIMESTAMP | |
| `last_modified_date` | TIMESTAMP | |

---

#### `[REUSE+MODIFY] domain/ApAuthority.java`
**來源：** `vc-handler: Authority.java`

**修改項：**
- Table: `@Table(name = "ap_authority", schema = "issuer_ap")`
- 保留 `Persistable<String>` 實作模式
- 其餘邏輯不變

---

#### `[NEW] repository/ApUserRepository.java`
```java
public interface ApUserRepository extends JpaRepository<ApUser, Long> {
    Optional<ApUser> findByUsername(String username);
}
```

---

#### `[NEW] repository/ApAuthorityRepository.java`
```java
public interface ApAuthorityRepository extends JpaRepository<ApAuthority, String> {
}
```

---

#### `[REUSE] security/SecurityUtils.java`
**來源：** `vc-handler: SecurityUtils.java`

**修改項：** 僅改 package name
- 保留 `JWT_ALGORITHM = MacAlgorithm.HS512`
- 保留 `AUTHORITIES_KEY = "auth"`
- 保留所有 static utility methods

---

#### `[REUSE+MODIFY] security/AuthoritiesConstants.java`
**來源：** `vc-handler: AuthoritiesConstants.java`

**修改為：**
```java
public final class AuthoritiesConstants {
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String OPERATOR = "ROLE_OPERATOR";
    private AuthoritiesConstants() {}
}
```

---

#### `[REUSE+MODIFY] security/DomainUserDetailsService.java`
**來源：** `vc-handler: DomainUserDetailsService.java`

**修改項：**
- 改用 `ApUserRepository.findByUsername()`
- 移除 email 登入判斷邏輯 (AP 後台只用 username)
- 移除 `UserNotActivatedException`，改檢查 `status == "ACTIVE"`
- `createSpringSecurityUser()` 中 activation 檢查改為 status 檢查

---

#### `[NEW] service/AccountService.java`
- `getAccountInfo()`: 從 SecurityContext 取得當前使用者，回傳 DTO (username, authorities)

---

#### `[NEW] service/integration/VcHandlerClient.java`
**用途：** 整合預留 (Phase 1 僅建立介面)

```java
public interface VcHandlerClient {
    // Phase 2: credential 查詢、狀態變更等
}
```

---

#### `[NEW] service/integration/Oid4vciHandlerClient.java`
**用途：** 整合預留 (Phase 1 僅建立介面)

```java
public interface Oid4vciHandlerClient {
    // Phase 2: issuer config 查詢、credential offer 管理等
}
```

---

#### `[NEW] web/rest/AuthenticateController.java`
- `POST /api/issuer-ap/authenticate`
- Request body: `{ "username": "...", "password": "..." }`
- 驗證成功回傳 JWT token
- 使用 `AuthenticationManager.authenticate()` + `JwtEncoder`

---

#### `[NEW] web/rest/AccountController.java`
- `GET /api/issuer-ap/account`
- 需 authenticated，回傳當前使用者資訊 (username, authorities)

---

### 5.3 Application Configuration

#### `application.yml`

```yaml
spring:
  application:
    name: issuerApBackend
  profiles:
    active: dev
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        default_schema: issuer_ap
    naming:
      physical-strategy: org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
  liquibase:
    change-log: classpath:config/liquibase/master.xml
    default-schema: issuer_ap

server:
  port: 8080

# JWT Configuration (aligned with vc-handler: HS512, auth claim)
app:
  security:
    jwt:
      base64-secret: ${AP_JWT_BASE64_SECRET:}
      token-validity-in-seconds: 86400
      token-validity-in-seconds-for-remember-me: 2592000

# Integration endpoints (Phase 2+)
integration:
  vc-handler:
    base-url: ${VC_INTERNAL_URL:http://localhost:8082}
  oid4vci-handler:
    base-url: ${OID4VCI_INTERNAL_URL:http://localhost:8081}
  connect-timeout: 5000
  read-timeout: 10000
```

#### `application-dev.yml`

```yaml
logging:
  level:
    ROOT: INFO
    gov.moda.dw.issuer.ap: DEBUG
    org.hibernate.SQL: DEBUG

spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/modadw_issuer}
    username: ${SPRING_DATASOURCE_USERNAME:modadw_issuer}
    password: ${SPRING_DATASOURCE_PASSWORD:modadw}
    hikari:
      poolName: Hikari
      auto-commit: false
  liquibase:
    contexts: dev

app:
  cors:
    allowed-origins: "http://localhost:5173"
    allowed-methods: "*"
    allowed-headers: "*"
    allow-credentials: true
  security:
    jwt:
      # Dev only - generate with: openssl rand -base64 64
      base64-secret: ${AP_JWT_BASE64_SECRET:YTJkNGYxMjM0NTY3ODkwYWJjZGVmMDEyMzQ1Njc4OTBhYmNkZWYwMTIzNDU2Nzg5MGFiY2RlZjAxMjM0NTY3}
```

---

### 5.4 Liquibase Schema

#### `master.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <property name="now" value="now()" dbms="postgresql"/>
    <property name="floatType" value="float4" dbms="postgresql"/>
    <property name="uuidType" value="uuid" dbms="postgresql"/>

    <include file="config/liquibase/changelog/00000000000001_issuer_ap_initial_schema.xml"
             relativeToChangelogFile="false"/>
</databaseChangeLog>
```

#### `00000000000001_issuer_ap_initial_schema.xml`

建立以下 tables (所有 table 指定 `schemaName="issuer_ap"`):

**Table: `ap_authority`**

| Column | Type | Constraints |
|--------|------|-------------|
| `name` | VARCHAR(50) | PK, NOT NULL |

**Table: `ap_user`**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PK, auto-increment (sequence: `issuer_ap.ap_user_id_seq`) |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL |
| `password_hash` | VARCHAR(60) | NOT NULL |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' |
| `created_date` | TIMESTAMP | DEFAULT now() |
| `last_modified_date` | TIMESTAMP | DEFAULT now() |

**Table: `ap_user_authority`**

| Column | Type | Constraints |
|--------|------|-------------|
| `user_id` | BIGINT | FK → ap_user.id, NOT NULL |
| `authority_name` | VARCHAR(50) | FK → ap_authority.name, NOT NULL |
| | | PK(user_id, authority_name) |

**Initial Data (loadData):**

`ap_authority`:
| name |
|------|
| ROLE_ADMIN |
| ROLE_OPERATOR |

`ap_user`:
| id | username | password_hash | status |
|----|----------|---------------|--------|
| 1 | admin | (BCrypt of "admin") | ACTIVE |

`ap_user_authority`:
| user_id | authority_name |
|---------|---------------|
| 1 | ROLE_ADMIN |

---

## 6. Frontend File Plan

### 6.1 Project Structure

```
issuer-ap/frontend/
├── package.json                                [NEW]
├── vite.config.ts                              [NEW]
├── tsconfig.json                               [NEW]
├── index.html                                  [NEW]
└── src/
    ├── main.ts                                 [NEW]
    ├── App.vue                                 [NEW]
    ├── router/
    │   └── index.ts                            [NEW]
    ├── stores/
    │   └── auth.ts                             [NEW]
    ├── utils/
    │   └── request.ts                          [NEW]
    ├── views/
    │   ├── login/
    │   │   └── index.vue                       [NEW]
    │   └── dashboard/
    │       └── index.vue                       [NEW]
    └── layout/
        └── index.vue                           [NEW]
```

### 6.2 File Details

#### `package.json`
- `vue@3`, `vue-router@4`, `pinia@2`
- `element-plus`, `axios`
- `typescript`, `vite`

#### `vite.config.ts`
- Dev server proxy: `/api` → `http://localhost:8080` (backend)

#### `src/router/index.ts`
- Routes: `/login` → Login view, `/` → Layout with Dashboard child
- `beforeEach` guard: 無 token 時導向 `/login`

#### `src/stores/auth.ts` (Pinia)
- State: `token`, `user` (username, authorities)
- Getters: `isAdmin`, `isOperator`, `isAuthenticated`
- Actions: `login()`, `fetchAccount()`, `logout()`

#### `src/utils/request.ts`
- Axios instance, baseURL: `/api/issuer-ap`
- Request interceptor: 自動加 `Authorization: Bearer {token}`
- Response interceptor: 401 → 清除 token → 導向 `/login`

#### `src/views/login/index.vue`
- Element Plus `el-form`: username + password
- 呼叫 `POST /api/issuer-ap/authenticate`
- 成功後存 token 至 Pinia store + localStorage

#### `src/layout/index.vue`
- 左側 `el-menu` sidebar
- 頂部 navbar (顯示 username, logout button)
- `<router-view/>` 主內容區

#### `src/views/dashboard/index.vue`
- 歡迎頁面，顯示當前使用者資訊與角色

---

## 7. Integration Pre-work (Phase 1 Scope)

Phase 1 建立整合基礎設施，Phase 2+ 實作具體呼叫：

| Item | Phase 1 Deliverable | Phase 2+ Usage |
|------|---------------------|----------------|
| `IntegrationProperties.java` | 設定類別 + yml 參數 | 讀取 vc/oid4vci service URL |
| `RestClientConfiguration.java` | RestTemplate Bean (含 timeout) | 注入至 Client 實作 |
| `VcHandlerClient.java` | 空介面 | 呼叫 credential CRUD、status-list 等 |
| `Oid4vciHandlerClient.java` | 空介面 | 呼叫 issuer config、credential offer 管理 |
| `application.yml` integration block | URL + timeout 參數 | 環境變數覆蓋 |
| CORS configuration | 允許前端 dev server | 允許部署環境的前端 origin |

---

## 8. Verification Plan

### 8.1 Automated Tests

**`AuthenticateControllerIntTest.java`** (使用 `@SpringBootTest` + `MockMvc`)

| Test Case | Expected |
|-----------|----------|
| 正確帳密登入 | HTTP 200 + JWT token in response body |
| 錯誤密碼 | HTTP 401 Unauthorized |
| 不存在的帳號 | HTTP 401 Unauthorized |
| 無 token 存取 `/api/issuer-ap/account` | HTTP 401 |
| 有效 token 存取 `/api/issuer-ap/account` | HTTP 200 + user info (username, authorities) |
| ROLE_OPERATOR 存取 ADMIN-only endpoint (預留) | HTTP 403 Forbidden |

### 8.2 Manual Verification

1. **啟動 Backend:** `cd issuer-ap/backend && ./mvnw spring-boot:run`
2. **啟動 Frontend:** `cd issuer-ap/frontend && npm run dev`
3. **測試流程：**
   - 開啟 `http://localhost:5173` → 應被導向 `/login`
   - 輸入 `admin / admin` → 登入成功 → 導向 Dashboard
   - Chrome DevTools → Application → LocalStorage → 確認有 token
   - Network tab → `GET /api/issuer-ap/account` → Header 含 `Authorization: Bearer <token>`
   - 登出 → token 清除 → 導回 `/login`

---

## 9. Reuse Mapping Summary

| Source (vc-handler) | Target (issuer-ap) | Modification |
|---------------------|---------------------|-------------|
| `SecurityConfiguration.java` | `config/SecurityConfiguration.java` | BCrypt, 移除 AmsAuthProvider, 調整 endpoints |
| `SecurityJwtConfiguration.java` | `config/SecurityJwtConfiguration.java` | `@Value` 改讀 `app.security.jwt.*` |
| `CustomJwtGrantedAuthoritiesConverter.java` | `config/CustomJwtGrantedAuthoritiesConverter.java` | Package only |
| `SecurityUtils.java` | `security/SecurityUtils.java` | Package only |
| `AuthoritiesConstants.java` | `security/AuthoritiesConstants.java` | ADMIN + OPERATOR |
| `DomainUserDetailsService.java` | `security/DomainUserDetailsService.java` | ApUserRepo, 移除 email, status check |

---

## 10. Out of Scope (Phase 2+)

- Credential 管理 UI (CRUD via vc-handler API)
- Credential Offer 管理 (via oid4vci-handler API)
- Issuer Config 管理介面
- 使用者管理 (CRUD ap_user)
- Audit logging
- Verify AP System (`/api/verify-ap/...`)
