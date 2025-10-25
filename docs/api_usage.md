# API仕様と次のステップ

このドキュメントは、実装された認証APIの仕様と、フロントエンドでの利用方法について説明します。

## 1. アプリケーションの起動

以下のコマンドでアプリケーションを起動します。

```bash
./gradlew bootRun
```

- デフォルトでは `https://localhost:8443` で起動します。
    - HTTPS 対応済み

## 2. APIエンドポイント

### ユーザー登録

新しいユーザーを作成します。

- **URL**: `POST /api/register`
- **Request Body**: `application/json`
  ```json
  {
    "name": "yourname",
    "password": "yourpassword"
  }
  ```
- **Success Response**: `201 Created`
- **Failure Response (ユーザー名重複)**: `409 Conflict`

**curlコマンド例:**
```bash
curl -X POST -H "Content-Type: application/json" -d '{"name":"testuser", "password":"password"}' https://localhost:8443/api/register
```

### ログイン

ユーザーを認証し、セッションを開始します。成功すると、クライアントにセッションIDを含むCookieが設定されます。

- **URL**: `POST /api/login`
- **Request Body**: `application/x-www-form-urlencoded`
  - `username`: ユーザー名
  - `password`: パスワード
- **Success Response**: `200 OK` (セッションCookieがセットされる)
- **Failure Response**: `401 Unauthorized`

**curlコマンド例 (Cookieを `cookie.txt` に保存):**
```bash
curl -X POST -c cookie.txt -d 'username=testuser&password=password' https://localhost:8443/api/login
```

### ログインユーザー情報の取得

現在ログインしているユーザーの情報を取得します。認証が必要です。

- **URL**: `GET /api/me`
- **Authentication**: セッションCookieが必要
- **Success Response**: `200 OK` とユーザー情報
  ```json
  {
    "name": "testuser"
  }
  ```
- **Failure Response**: `401 Unauthorized`

**curlコマンド例 (保存したCookieを使用):**
```bash
# -b : クッキーの送信オプション
curl -b cookie.txt https://localhost:8443/api/me
```

### ログアウト

現在のセッションを無効化します。

- **URL**: `POST /api/logout`
- **Authentication**: セッションCookieが必要
- **Success Response**: `200 OK`

**curlコマンド例:**
```bash
curl -X POST -b cookie.txt https://localhost:8443/api/logout
```

### OTPセットアップ

ユーザーのOTP (ワンタイムパスワード) をセットアップします。

- **URL**: `POST /api/otp/setup`
- **Authentication**: セッションCookieが必要
- **Request Body**: なし
- **Success Response**: `200 OK` とシークレットキーおよびQRコード画像データ (Base64エンコード)
  ```json
  {
    "secret": "YOUR_SECRET_KEY",
    "qrUrl": "data:image/png;base64,..."
  }
  ```
- **Failure Response**: `500 Internal Server Error` (QRコード生成失敗時)

### OTP検証

ユーザーが入力したOTPコードを検証し、OTP機能を有効にします。

- **URL**: `POST /api/otp/verify`
- **Authentication**: セッションCookieが必要
- **Request Body**: `application/json`
  ```json
  {
    "code": "YOUR_OTP_CODE"
  }
  ```
- **Success Response**: `200 OK` と "OTP enabled successfully"
- **Failure Response**: `401 Unauthorized` (コードが無効な場合)

### パスキー登録開始

WebAuthn (パスキー) 登録プロセスを開始します。

- **URL**: `POST /passkey/register/start`
- **Authentication**: セッションCookieが必要 (既存ユーザーとしてログイン済みであること)
- **Request Body**: なし
- **Success Response**: `200 OK` と `PublicKeyCredentialCreationOptions` (JSON形式)
- **Failure Response**: `400 Bad Request` または `500 Internal Server Error`

### パスキー登録完了

WebAuthn (パスキー) 登録プロセスを完了します。

- **URL**: `POST /passkey/register/finish`
- **Authentication**: セッションCookieが必要
- **Request Body**: `application/json` (クライアントから受け取った `PublicKeyCredential` オブジェクト)
- **Success Response**: `200 OK` と "Registration successful"
- **Failure Response**: `400 Bad Request` (登録失敗または進行中の登録がない場合)

### パスキー認証開始

WebAuthn (パスキー) 認証プロセスを開始します。

- **URL**: `POST /passkey/login/start`
- **Request Body**: `application/json` (オプションでユーザー名を含む `StartLoginRequest`)
  ```json
  {
    "username": "optional_username"
  }
  ```
- **Success Response**: `200 OK` と `PublicKeyCredentialRequestOptions` (JSON形式)
- **Failure Response**: `400 Bad Request` または `500 Internal Server Error`

### パスキー認証完了

WebAuthn (パスキー) 認証プロセスを完了します。

- **URL**: `POST /passkey/login/finish`
- **Request Body**: `application/json` (クライアントから受け取った `PublicKeyCredential` オブジェクト)
- **Success Response**: `200 OK` と "Login successful for user: [username]"
- **Failure Response**: `400 Bad Request` (認証失敗または進行中の認証がない場合)

