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

                fetch('/api/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: new URLSearchParams({
                        'username': event.userId,
                        'password': event.password
                    })
                })
                .then(response => {
                    if (response.ok) {
                        setStatus("ログイン成功");
                        appendLog("APIレスポンス: ログイン成功 (200 OK)", "info");
                        // TODO: ログイン後の処理（例：ユーザー情報の表示など）
                    } else {
                        setStatus("ログイン失敗");
                        appendLog(`APIレスポンス: ログイン失敗 (${response.status})`, "error");
                    }
                })
                .catch(error => {
                    setStatus("ログインエラー");
                    appendLog(`ネットワークエラー: ${error}`, "error");
                });

            } else {
                setStatus("ログイン失敗: 入力不足");
                appendLog("エラー: ユーザーIDまたはパスワードが未入力です", "error");
            }
            break;

        case "logout":
            setStatus("ログアウト中...");
            appendLog("ログアウト処理を実行します", "user");

            fetch('/api/logout', {
                method: 'POST'
            })
            .then(response => {
                if (response.ok) {
                    setStatus("ログアウト成功");
                    appendLog("APIレスポンス: ログアウト成功 (200 OK)", "info");
                    userIdInput.value = '';
                    passwordInput.value = '';
                } else {
                    setStatus("ログアウト失敗");
                    appendLog(`APIレスポンス: ログアウト失敗 (${response.status})`, "error");
                }
            })
            .catch(error => {
                setStatus("ログアウトエラー");
                appendLog(`ネットワークエラー: ${error}`, "error");
            });
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
