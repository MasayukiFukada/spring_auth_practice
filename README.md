# Spring フレームワークにおける認証などの実験

- [変更内容](./CHANGELOG.md)
- [HTTPS対応](./docs/mkcert.md)
    - ローカル環境でも HTTPS に対応させる方法
    - ※ Spring の場合
- [API仕様と次のステップ](./docs/api_usage.md)
- [生成AIによる開発](./docs/ai.md)

## 機能

- ユーザー登録
    - ID・パスワードを入力して検証用のユーザーを登録することができます
    - ユーザー登録後、登録したユーザーでログインできるようになります
- ID・パスワードによるログイン/ログアウト
    - ごく一般的なユーザー認証
- 時間ベースの OTP(TOTP: Time-based One-Time Password) による二要素認証
    - スマートフォンを使用するなどして TOTP を登録してセキュリティを強化できます
    - ログインした後の画面で登録できます
- パスキー
    - パスキーによる認証作業の軽減 ※ 対応ブラウザが必要( Google Chrome で検証 )
    - ログインした後の画面で登録できます
    - **<制限>** パスキーは例外的に **localhost** を許可する形で検証しています
      スマートフォンなど他端末からアクセスした場合にはドメインの関係上パスキーの動作を確認できない可能性があります

## 環境

- Java
- Spring
- DB: SQLite

## ビルド と起動

- `./gradlew build` でビルド

- `./gradlew bootrun` で起動

## CHANGELOG.md の更新

- `git cliff > CHANGELOG.md`

