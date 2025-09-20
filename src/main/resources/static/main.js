// RxJS を CDN から読み込む前提
// <script src="https://unpkg.com/rxjs@7/dist/bundles/rxjs.umd.min.js"></script>

/* 初回プロンプト
main.jsにはまだ何も書かれていない状態ですのでゼロから作成して大丈夫です。
可能ならRxJSなどのリアクティブな仕組みを取り入れたいと考えています。
*/
const { fromEvent, merge } = rxjs;
const { map, tap } = rxjs.operators;

// 要素参照
const userIdInput = document.getElementById("userId");
const passwordInput = document.getElementById("password");
const loginBtn = document.getElementById("btnLogin");
const logoutBtn = document.getElementById("btnLogout");
const statusLabel = document.getElementById("status");
const logArea = document.getElementById("logArea");

// --- ストリーム定義 ---

// ログインイベント
const login$ = fromEvent(loginBtn, "click").pipe(
    map(() => ({
        type: "login",
        userId: userIdInput.value,
        password: passwordInput.value,
    }))
);

// ログアウトイベント
const logout$ = fromEvent(logoutBtn, "click").pipe(
    map(() => ({ type: "logout" }))
);

// 入力イベント（オプション：リアルタイムで監視したい場合）
const userIdChange$ = fromEvent(userIdInput, "input").pipe(
    map((e) => ({ type: "userIdChange", value: e.target.value }))
);

// ストリーム統合
const app$ = merge(login$, logout$, userIdChange$);

// --- ストリーム購読処理 ---
app$.subscribe((event) => {
    switch (event.type) {
        case "login":
            if (event.userId && event.password) {
                setStatus("ログイン中...");
                appendLog(`ログイン試行: ${event.userId}`, "user");

                // 疑似APIレスポンス
                setTimeout(() => {
                    setStatus("ログイン成功");
                    appendLog("APIレスポンス: { status: 200, message: 'OK' }", "info");
                }, 1000);
            } else {
                setStatus("ログイン失敗: 入力不足");
                appendLog("エラー: ユーザーIDまたはパスワードが未入力です", "error");
            }
            break;

        case "logout":
            setStatus("ログアウトしました");
            appendLog("ログアウト処理が実行されました", "user");
            break;

        case "userIdChange":
            appendLog(`入力中のユーザーID: ${event.value}`, "user");
            break;
    }
});

// --- ユーティリティ関数 ---

function setStatus(text) {
    statusLabel.textContent = "ステータス: " + text;
}

const MAX_LOGS = 300; // 保持件数の上限

function appendLog(message, type = "user") {
    const now = new Date().toLocaleTimeString();
    const logLine = document.createElement("div");

    // メッセージタイプごとに色を切り替え
    switch (type) {
        case "user":
            logLine.className = "text-green-300"; // ユーザー操作
            break;
        case "info":
            logLine.className = "text-blue-400"; // 正常ログ
            break;
        case "error":
            logLine.className = "text-red-400"; // 異常ログ
            break;
    }

    logLine.textContent = `[${now}] ${message}`;

    // 最新を上に追加
    if (logArea.firstChild) {
        logArea.insertBefore(logLine, logArea.firstChild);
    } else {
        logArea.appendChild(logLine);
    }

    // 件数制限を超えた場合は古いものを削除
    while (logArea.childNodes.length > MAX_LOGS) {
        logArea.removeChild(logArea.lastChild);
    }
}