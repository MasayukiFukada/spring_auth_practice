# 生成 AI による開発

## 開発の流れ

1. Spring Initilizr で手動で作成
1. <AI> 環境構築。Gradleのビルドが通らなかった部分を修正
1. main.html を手動で追加(ただし、テンプレートだけ)
1. mkcert (https)対応
1. <AI>ボタンや入力欄をchatGPTで作成
    - タイマー使用による疑似APIで対応
1. SQLiteに対応
1. <AI>ちゃんとしたAPIを用意
    - H2で作ろうとしていた
    - AIが直前に作成したはずのSQLiteを見落していた
1. <AI>main.htmlと接続

