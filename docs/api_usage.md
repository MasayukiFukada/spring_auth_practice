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

## 3. 次のステップ (フロントエンド実装)

バックエンドAPIの準備が整いました。次のステップは、`src/main/resources/static/main.js` を編集して、これらのAPIを呼び出すフロントエンドのロジックを実装することです。

**実装の指針:**
1.  HTMLのフォーム（登録、ログイン）のsubmitイベントを捕捉します。
2.  `fetch` APIを使用して、対応するバックエンドAPIにリクエストを送信します。
3.  APIからのレスポンス（成功または失敗）に応じて、UIを動的に更新します（例: 「ようこそ、testuserさん」と表示する、エラーメッセージを表示する、など）。
4.  ページ読み込み時に `/api/me` を呼び出し、既にログイン済みかどうかを確認してUIの状態を初期化します。
