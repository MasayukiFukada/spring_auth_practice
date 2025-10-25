# Spring フレームワークにおける認証などの実験

- [変更内容](./CHANGELOG.md)
- [HTTPS対応](./docs/mkcert.md)
    - ローカル環境でも HTTPS に対応させる方法
    - ※ Spring の場合
- [API仕様と次のステップ](./docs/api_usage.md)
- [生成AIによる開発](./docs/ai.md)

## ビルド と起動

- `./gradlew`
- `./gradlew bootrun` で起動
    - Spring Security の BASIC 認証が入るので注意

## CHANGELOG.md の更新

- `git cliff > CHANGELOG.md`

