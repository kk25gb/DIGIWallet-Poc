# Issuer AP 系統（發證機關管理平台）

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7-3178c6.svg)](https://www.typescriptlang.org/)

---

## 目錄

- [專案簡介](#專案簡介)
- [系統架構總覽](#系統架構總覽)
- [系統需求](#系統需求)
- [Backend（後端）](#backend後端)
  - [技術堆疊](#backend-技術堆疊)
  - [建置與啟動](#backend-建置與啟動)
  - [設定檔說明](#backend-設定檔說明)
  - [資料庫設定](#資料庫設定)
  - [API 端點一覽](#api-端點一覽)
- [Frontend（前端）](#frontend前端)
  - [技術堆疊](#frontend-技術堆疊)
  - [建置與啟動](#frontend-建置與啟動)
  - [設定檔說明](#frontend-設定檔說明)
  - [頁面與路由](#頁面與路由)
- [Security 安全機制](#security-安全機制)
  - [JWT 認證機制](#jwt-認證機制)
  - [角色與權限](#角色與權限)
  - [CORS 設定](#cors-設定)
  - [Access-Token Callback 驗證](#access-token-callback-驗證)
- [外部服務整合](#外部服務整合)
  - [vc-handler 整合](#vc-handler-整合)
  - [oid4vci-handler 整合](#oid4vci-handler-整合)
  - [核心系統環境變數](#核心系統環境變數)
- [資料庫 Schema](#資料庫-schema)
- [系統流程](#系統流程)

---

## 專案簡介

Issuer AP（發證機關管理平台）為數位憑證皮夾（TWDIW）基礎建設中，提供發證機關操作管理介面的子系統。包含前端管理介面與後端 API 服務，支援：

- **DID 註冊管理** — 發證機關 DID（Decentralized Identifier）的產生與註冊
- **憑證類型管理** — 定義可發行的憑證類型與 JSON Schema
- **憑證發行** — 預載持有人資料、產生 OID4VCI QR Code 供數位皮夾掃碼領證
- **系統設定管理** — 查看與更新 vc-handler 的系統設定

---

## 系統架構總覽

```
┌──────────────────┐       ┌──────────────────────┐
│  Issuer AP       │       │  Core System          │
│  Frontend        │       │                       │
│  (Vue 3 + Vite)  │       │  ┌────────────────┐   │
│  :5173           │       │  │ twdiw-vc-handler│   │
└────────┬─────────┘       │  │ :8082           │   │
         │ /api proxy      │  └────────────────┘   │
         ▼                 │                       │
┌──────────────────┐       │  ┌────────────────┐   │
│  Issuer AP       │◄─────►│  │ twdiw-oid4vci  │   │
│  Backend         │       │  │ -handler :8084  │   │
│  (Spring Boot)   │       │  └────────────────┘   │
│  :8080           │       │                       │
└────────┬─────────┘       └──────────────────────┘
         │
         ▼
┌──────────────────┐
│  PostgreSQL      │
│  DB: modadw_issuer│
│  Schema: issuer_ap│
└──────────────────┘
```

---

## 系統需求

| 項目 | 版本需求 |
|------|---------|
| Java | 17 (Eclipse Temurin) |
| Maven | 3.9+ (內建 Maven Wrapper) |
| Node.js | 18+ |
| npm | 9+ |
| PostgreSQL | 16 |

---

## Backend（後端）

### Backend 技術堆疊

| 技術 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.13 | 應用程式框架 |
| Spring Security | 6.x | 安全框架（JWT + OAuth2 Resource Server） |
| Spring Data JPA | - | 資料存取層 |
| Liquibase | 4.32.0 | 資料庫版本控制與遷移 |
| Nimbus JOSE+JWT | 10.0.2 | JWT 簽發與驗證（HS512） |
| PostgreSQL | 16 | 關聯式資料庫 |
| MapStruct | 1.5.5 | 物件映射 |
| Lombok | 1.18.32 | 程式碼簡化 |
| Spotless | 2.43.0 | 程式碼格式化（Google Java Format） |

### Backend 建置與啟動

```bash
# 進入後端目錄
cd issuer-ap/backend

# 編譯專案（使用 Maven Wrapper，無需本地安裝 Maven）
./mvnw clean package

# 啟動服務（預設 port 8080）
./mvnw spring-boot:run

# 僅執行測試
./mvnw test

# 執行單一測試類別
./mvnw test -Dtest=ClassName

# 執行單一測試方法
./mvnw test -Dtest=ClassName#methodName
```

> **注意：** 啟動前需確保 PostgreSQL 資料庫已建立，Liquibase 會自動執行 schema migration。

### Backend 設定檔說明

設定檔位於 `backend/src/main/resources/config/`：

| 檔案 | 用途 |
|------|------|
| `application.yml` | 主要設定檔（含 JWT、整合服務 URL） |
| `application-dev.yml` | 開發環境設定（DB 連線、CORS、日誌） |

#### 主要環境變數

| 環境變數 | 說明 | 預設值 |
|---------|------|--------|
| `AP_JWT_BASE64_SECRET` | JWT HS512 簽章金鑰（Base64 編碼，64 bytes） | dev 環境有預設值，**正式環境必須設定** |
| `AP_JWT_TOKEN_VALIDITY` | JWT Token 有效期（秒） | `86400`（1 天） |
| `AP_JWT_REMEMBER_ME_VALIDITY` | Remember-me Token 有效期（秒） | `2592000`（30 天） |
| `SPRING_DATASOURCE_URL` | PostgreSQL 連線字串 | `jdbc:postgresql://localhost:5432/modadw_issuer` |
| `SPRING_DATASOURCE_USERNAME` | 資料庫帳號 | `modadw_issuer` |
| `SPRING_DATASOURCE_PASSWORD` | 資料庫密碼 | `modadw` |
| `VC_INTERNAL_URL` | vc-handler 內部服務位址 | `http://192.168.74.25:8082` |
| `OID4VCI_INTERNAL_URL` | oid4vci-handler 內部服務位址 | `http://192.168.74.25:8084` |
| `OID4VCI_WALLET_CLIENT_ID` | OID4VCI 皮夾 Client ID | `moda_dw` |
| `OID4VCI_EXTERNAL_URL` | OID4VCI 對外服務位址（供皮夾存取） | `http://192.168.74.25:8084` |
| `CALLBACK_ACCESS_TOKEN` | 核心系統 callback 驗證用 Access-Token | dev 環境有預設值，**正式環境必須設定** |

#### 產生 JWT Secret

```bash
# 產生 Base64 編碼的 64-byte 隨機金鑰
openssl rand -base64 64
```

### 資料庫設定

- **資料庫名稱：** `modadw_issuer`
- **Schema：** `issuer_ap`
- **遷移工具：** Liquibase（自動執行，變更日誌位於 `src/main/resources/config/liquibase/`）

資料表：

| 資料表 | 說明 |
|--------|------|
| `ap_user` | 系統使用者（預設帳號 `admin` / 密碼 `admin`） |
| `ap_issuer_did` | 發證機關 DID 資料 |
| `ap_credential_type` | 憑證類型定義 |

### API 端點一覽

#### 認證相關

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| POST | `/api/issuer-ap/authenticate` | 公開 | 登入取得 JWT Token |
| GET | `/api/issuer-ap/account` | 已認證 | 取得當前使用者資訊 |

#### DID 管理

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| POST | `/api/issuer-ap/did/register` | ADMIN | 向核心系統註冊發證機關 DID |

#### 憑證類型管理

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| GET | `/api/issuer-ap/credential-types` | 已認證 | 列出所有憑證類型 |
| GET | `/api/issuer-ap/credential-types/{type}` | 已認證 | 取得單一憑證類型 |
| POST | `/api/issuer-ap/credential-types` | ADMIN | 建立憑證類型 |
| DELETE | `/api/issuer-ap/credential-types/{type}` | ADMIN | 刪除憑證類型 |
| PUT | `/api/issuer-ap/credential-types/{type}/func-switch` | ADMIN | 設定功能開關（TX Code / VC Transfer） |

#### 憑證發行

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| POST | `/api/issuer-ap/issue/preload` | ADMIN | 預載持有人憑證資料 |
| POST | `/api/issuer-ap/issue/qr-code` | ADMIN | 產生 OID4VCI QR Code |

#### 系統設定

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| GET | `/api/issuer-ap/settings` | ADMIN | 查詢 vc-handler 設定 |
| PUT | `/api/issuer-ap/settings` | ADMIN | 更新 vc-handler 設定 |

#### Mock DID Server（Callback 端點，供核心系統回呼）

| Method | Path | 權限 | 說明 |
|--------|------|------|------|
| POST | `/api/mock-did-server/generate` | Access-Token | 產生 DID Document（callback） |
| POST | `/api/mock-did-server/create` | Access-Token | 儲存簽章後的 DID（callback） |
| GET | `/api/mock-did-server/{didId}` | 公開 | 查詢發證機關 DID 資訊 |

---

## Frontend（前端）

### Frontend 技術堆疊

| 技術 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.13 | 前端框架 |
| TypeScript | 5.7.2 | 型別安全 |
| Vite | 6.0.5 | 建置工具與開發伺服器 |
| Element Plus | 2.9.1 | UI 元件庫 |
| Pinia | 2.3.0 | 狀態管理 |
| Vue Router | 4.5.0 | 前端路由 |
| Axios | 1.7.9 | HTTP 請求 |

### Frontend 建置與啟動

```bash
# 進入前端目錄
cd issuer-ap/frontend

# 安裝依賴
npm install

# 啟動開發伺服器（預設 port 5173）
npm run dev

# 建置正式版本
npm run build

# 預覽正式版本
npm run preview
```

### Frontend 設定檔說明

#### Vite 設定（`vite.config.ts`）

| 設定項目 | 值 | 說明 |
|---------|-----|------|
| Server Port | `5173` | 開發伺服器埠號 |
| API Proxy | `/api` → `http://localhost:8080` | 將 API 請求代理至後端 |
| Path Alias | `@/` → `./src/` | 方便 import 路徑 |

#### HTTP Client（`src/utils/request.ts`）

| 設定項目 | 值 | 說明 |
|---------|-----|------|
| baseURL | `/api/issuer-ap` | API 基礎路徑 |
| timeout | `15000` ms | 請求逾時時間 |
| Token Key | `ap_token` | localStorage 中的 JWT Token 鍵名 |

- **Request Interceptor：** 自動從 `localStorage` 讀取 `ap_token` 並加入 `Authorization: Bearer <token>` Header
- **Response Interceptor：** 收到 401 回應時自動清除 Token 並導向登入頁

### 頁面與路由

| 路由 | 頁面 | 權限 | 說明 |
|------|------|------|------|
| `/login` | 登入頁 | 公開 | 帳號密碼登入 |
| `/` | Dashboard | 已認證 | 首頁，顯示使用者資訊 |
| `/did/register` | DID 註冊 | ADMIN | 註冊發證機關 DID |
| `/settings` | 系統設定 | ADMIN | 查看/更新系統設定 |
| `/credential-types` | 憑證類型列表 | ADMIN | 管理憑證類型 |
| `/credential-types/create` | 建立憑證類型 | ADMIN | 定義新的憑證類型與欄位 |
| `/issue` | 發行憑證 | ADMIN | 三步驟精靈：選擇類型 → 填入資料 → 產生 QR Code |

---

## Security 安全機制

### JWT 認證機制

本系統採用 **JWT（JSON Web Token）** 搭配 **HS512** 演算法進行身分認證，以 **Stateless** 方式運作。

**認證流程：**

```
1. 使用者 POST /api/issuer-ap/authenticate（帳號 + 密碼）
2. 後端以 BCrypt 驗證密碼
3. 驗證成功後簽發 JWT（HS512）
4. 回傳 { "id_token": "eyJhbGci..." }
5. 前端將 Token 存入 localStorage（key: ap_token）
6. 後續請求自動帶入 Authorization: Bearer <token>
```

**JWT Payload 結構：**

```json
{
  "sub": "admin",
  "iat": 1710000000,
  "exp": 1710086400,
  "auth": "ROLE_ADMIN"
}
```

| 欄位 | 說明 |
|------|------|
| `sub` | 使用者帳號 |
| `iat` | 簽發時間 |
| `exp` | 過期時間 |
| `auth` | 權限角色（逗號分隔） |

**關鍵設定：**

| 設定 | 說明 |
|------|------|
| `app.security.jwt.base64-secret` | HS512 簽章金鑰（Base64 編碼） |
| `app.security.jwt.token-validity-in-seconds` | Token 有效期（預設 86400 秒） |
| `app.security.jwt.token-validity-in-seconds-for-remember-me` | Remember-me 有效期（預設 2592000 秒） |

> **重要：** 正式環境務必更換 JWT Secret，可使用 `openssl rand -base64 64` 產生。

### 角色與權限

| 角色 | 說明 | 可存取功能 |
|------|------|-----------|
| `ROLE_ADMIN` | 管理員 | 全部功能（DID 註冊、憑證類型管理、憑證發行、系統設定） |
| `ROLE_OPERATOR` | 操作員 | Dashboard、查看憑證類型 |

**預設帳號：**

| 帳號 | 密碼 | 角色 |
|------|------|------|
| `admin` | `admin` | `ROLE_ADMIN` |

> **重要：** 正式環境務必修改預設密碼。

### CORS 設定

設定於 `application-dev.yml`：

```yaml
app:
  cors:
    allowed-origins: "http://localhost:5173"
    allowed-methods: "*"
    allowed-headers: "*"
    allow-credentials: true
```

CORS 規則套用範圍為 `/api/**` 路徑。正式環境需將 `allowed-origins` 改為實際的前端部署網址。

### Access-Token Callback 驗證

核心系統（vc-handler）回呼 Mock DID Server 端點時，需在 HTTP Header 帶入 `Access-Token`：

```
Access-Token: <CALLBACK_ACCESS_TOKEN 的值>
```

- 僅對 `POST /api/mock-did-server/**` 驗證
- `GET /api/mock-did-server/**` 不需驗證
- 由 `AccessTokenFilter` 負責攔截驗證

---

## 外部服務整合
Issuer AP 主要整合兩個核心系統服務: VC-handler（發證機關 VC 核心服務）與 OID4VCI-handler（作為 VC-handler 的對外通道服務）

這兩個模組來源於: https://github.com/moda-gov-tw/TWDIW-official-app

本次 poc 開發時所使用的版本 hash: `027cca0`, date: 2025/12/24

### vc-handler 整合

Issuer AP 透過 REST API 呼叫 `twdiw-vc-handler`（發證機關 VC 服務）：

| 呼叫端點 | 方法 | 用途 |
|---------|------|------|
| `/api/did` | POST | 註冊發證機關 DID |
| `/api/checksetting` | GET | 查詢系統設定 |
| `/api/updatesetting` | POST | 更新系統設定 |
| `/api/setseq/{credentialType}` | POST | 建立憑證類型序號 |
| `/api/delseq/{credentialType}` | POST | 刪除憑證類型序號 |
| `/api/funcswitch/{credentialType}` | POST | 設定功能開關 |
| `/api/setdata` | POST | 預載持有人憑證資料 |

**連線設定：**

```yaml
integration:
  vc-handler:
    base-url: ${VC_INTERNAL_URL:http://192.168.74.25:8082}
  connect-timeout: 5000   # 連線逾時（ms）
  read-timeout: 10000     # 讀取逾時（ms）
```

### oid4vci-handler 整合

Issuer AP 透過 REST API 呼叫 `twdiw-oid4vci-handler`（OID4VCI 發行端服務）：

| 呼叫端點 | 方法 | 用途 |
|---------|------|------|
| `/api/issuer/{issuerId}/qr-code` | POST | 產生 OID4VCI QR Code |

**連線設定：**

```yaml
integration:
  oid4vci-handler:
    base-url: ${OID4VCI_INTERNAL_URL:http://192.168.74.25:8084}
  oid4vci:
    wallet-client-id: ${OID4VCI_WALLET_CLIENT_ID:moda_dw}
    external-url: ${OID4VCI_EXTERNAL_URL:http://192.168.74.25:8084}
```

### 核心系統環境變數

`twdiw-vc-handler` 與 `twdiw-oid4vci-handler` 各自有獨立的環境變數設定。

> **詳細環境變數欄位與設定值請參閱：**
>
> - vc-handler：[`docs/env-vc-handler.md`](docs/env-vc-handler.md)
> - oid4vci-handler：[`docs/env-oid4vci-handler.md`](docs/env-oid4vci-handler.md)

---

## 資料庫 Schema

所有資料表位於 PostgreSQL `modadw_issuer` 資料庫的 `issuer_ap` schema 下，由 Liquibase 自動管理遷移。

### ap_user（系統使用者）

| 欄位 | 型別 | 說明 |
|------|------|------|
| id | BIGINT (PK) | 自增主鍵 |
| username | VARCHAR(50) | 帳號（唯一） |
| password_hash | VARCHAR(60) | BCrypt 密碼雜湊 |
| authority | VARCHAR(50) | 角色（預設 `ROLE_OPERATOR`） |
| status | VARCHAR(20) | 狀態（`ACTIVE`） |
| created_date | TIMESTAMP | 建立時間 |
| last_modified_date | TIMESTAMP | 最後修改時間 |

### ap_issuer_did（發證機關 DID）

| 欄位 | 型別 | 說明 |
|------|------|------|
| id | BIGINT (PK) | 自增主鍵 |
| did_id | VARCHAR(500) | DID 識別碼（唯一，如 `did:key:z...`） |
| did_jwt | TEXT | 簽章後的 DID JWT |
| did_document | JSONB | DID Document（JSON 格式） |
| org | JSONB | 組織資訊（JSON 格式） |
| org_type | INTEGER | 組織類型（預設 1） |
| p7data | TEXT | 憑證鏈資料 |
| status | VARCHAR(20) | 狀態（`ACTIVE`） |
| created_date | TIMESTAMP | 建立時間 |

### ap_credential_type（憑證類型定義）

| 欄位 | 型別 | 說明 |
|------|------|------|
| id | BIGINT (PK) | 自增主鍵 |
| credential_type | VARCHAR(200) | 憑證類型名稱（唯一，格式 `businessId_typeName`） |
| business_id | VARCHAR(100) | 發證機關業務 ID |
| schema_id | VARCHAR(500) | Schema 參照 URL |
| vc_schema | TEXT | JSON Schema 定義 |
| effective_time_unit | VARCHAR(20) | 有效期單位（如 `YEAR`、`DAY`） |
| effective_time_value | INTEGER | 有效期數值 |
| metadata | TEXT | OID4VCI Metadata |
| enable_tx_code | BOOLEAN | 是否啟用 TX Code（預設 false） |
| enable_vc_transfer | BOOLEAN | 是否啟用憑證轉移（預設 false） |
| status | VARCHAR(20) | 狀態（`ACTIVE`） |
| created_date | TIMESTAMP | 建立時間 |

---

## 系統流程

### DID 註冊流程

```
管理員 → POST /api/issuer-ap/did/register（組織資訊 + 憑證鏈）
       ↓
Issuer AP Backend → vc-handler POST /api/did
       ↓
vc-handler 產生金鑰對後回呼：
  1. POST /api/mock-did-server/generate（公鑰 JWK）
     → Issuer AP 產生 did:key Document 回傳
  2. POST /api/mock-did-server/create（簽章後 DID JWT）
     → Issuer AP 儲存至 ap_issuer_did
```

### 憑證發行流程

```
管理員 → 選擇憑證類型 → 填入持有人資料 → 預載資料
       ↓
POST /api/issuer-ap/issue/preload
       → vc-handler POST /api/setdata
       ↓
POST /api/issuer-ap/issue/qr-code
       → 建立 id_token JWT（含 credential_configuration_id、nonce 等）
       → oid4vci-handler POST /api/issuer/{issuerId}/qr-code
       ↓
回傳 QR Code（Base64）+ Credential Offer 連結
       ↓
持有人使用數位皮夾掃碼領證
```

---

## 開發注意事項

1. **程式碼格式化：** 後端使用 Spotless（Google Java Format），提交前會自動檢查
2. **資料庫遷移：** 請勿手動修改 DB schema，一律透過 Liquibase changelog 管理
3. **前端代理：** 開發環境下前端 Vite dev server 會自動將 `/api` 請求代理到 `http://localhost:8080`
4. **JWT Secret：** 開發環境已有預設值，正式部署務必更換
5. **預設帳密：** `admin` / `admin`，正式部署務必修改
