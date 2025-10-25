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
    - 固定IP で DNS で名前割り当てたので作り直し
        - `mkcert localhost 127.0.0.1 ::1 192.168.11.39 arch-portable.local`

1. ローカル証明書の作成
    - `mkcert -pkcs12 localhost 127.0.0.1 ::1` を実行
    - ~/localhost+2.p12
        - 固定IP で DNS で名前割り当てたので作り直し
        - `mkcert -pkcs12 localhost 127.0.0.1 ::1 192.168.11.39 arch-portable.local` を実行
        - ~/localhost+3.p12
        - 他の端末(スマホなど)からサーバーに接続する時用にサーバーの IP も加えておく
        - 必要そうなら DNS ( もしくは mDNS )の設定を行う

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

# 他端末からアクセスするために証明書を使用する

1. ~/.local/share/mkcert/rootCA.pem
    - mkcert におけるルート CA 証明書はこちらにある

1. Android に pem ファイルを送信する
    - LocalSend や Pairdrop などで転送

1. Android の標準の「設定」から"証明書"で探すと CA 証明書をインストールする画面が見つかるのでファイルを使用してインストールする
    - インストール済みの中から「ユーザー」カテゴリでインストールされているので必要に応じてアンインストールも可能

