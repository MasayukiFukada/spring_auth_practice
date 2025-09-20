# Spring フレームワークにおける認証などの実験

- [変更内容](./CHANGELOG.md)

## ビルド と起動

- `./gradlew`
- `./gradlew bootrun` で起動
    - Spring Security の BASIC 認証が入るので注意

## mkcert によるローカル証明書

今回行なった手順

1. ホームディレクトリに移動

1. ローカル証明局の作成
    - `mkcert -install` でルート証明書が作成できる
    - 自分のホームの .local 配下
    - ~/.local/share/mkcert/rootCA.pem
        - 公開鍵
        - こちらを Kleopatra などでインポート
    - ~/.local/share/mkcert/rootCA-key.pem
        - 秘密鍵

1. (これは不要な操作かも？)
    - `mkcert localhost 127.0.0.1 ::1` を実行
    - ~/localhost+2.pem
        - 公開鍵
    - ~/localhost+2-key.pem
        - 秘密鍵

1. ローカル証明書の作成
    - `mkcert -pkcs12 localhost 127.0.0.1 ::1` を実行
    - ~/localhost+2.p12

1. src/main/resources に証明書を配置する
    - cp ~/localhost+2.p12 .

1. application.yml に下記を追記
    - 8443 ポートは重要
        - 開発環境でのセキュアなウェブサービスの代替アクセス
    - 8080
        - 開発環境でのウェブサービスアクセス

```
server:
  port: 8443
  ssl:
    key-store: classpath:localhost+2.p12  # 証明書ファイル
    key-store-type: PKCS12                # 証明書作成時のキーストアタイプ
    key-password: changeit                # mkcertデフォルト
    key-store-password: changeit          # mkcertデフォルト
```

1. サーバーを起動すれば `https://localhost:8443` でアクセスができる
    - ブラウザのアドレス欄から https であることが確認できる

## CHANGELOG.md の更新

- `git cliff > CHANGELOG.md`

