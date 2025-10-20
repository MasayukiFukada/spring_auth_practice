// RxJS を CDN から読み込む前提
const { fromEvent, merge, of, from } = rxjs;
const { map, switchMap, catchError, tap } = rxjs.operators;

// --- 要素参照 ---
const userIdInput = document.getElementById("userId");
const passwordInput = document.getElementById("password");
const loginBtn = document.getElementById("btnLogin");
const logoutBtn = document.getElementById("btnLogout");
const statusLabel = document.getElementById("status");
const logArea = document.getElementById("logArea");

const loginForm = document.getElementById("loginForm");
const otpLoginForm = document.getElementById("otpLoginForm");
const otpCodeInput = document.getElementById("otpCode");
const btnOtpLogin = document.getElementById("btnOtpLogin");

const otpSetupArea = document.getElementById("otpSetupArea");
const btnEnableOtpSetup = document.getElementById("btnEnableOtpSetup");
const otpQrCodeArea = document.getElementById("otpQrCodeArea");
const otpQrCodeImage = document.getElementById("otpQrCodeImage");
const otpVerifyCodeInput = document.getElementById("otpVerifyCode");
const btnVerifyOtp = document.getElementById("btnVerifyOtp");

const btnRegister = document.getElementById("btnRegister");

// --- 状態管理 ---
const initialState = {
    loggedIn: false,
    otpPending: false,
    user: null,
};
let state = { ...initialState };

// --- ストリーム定義 ---

// ログインイベント
const login$ = fromEvent(loginBtn, "click").pipe(
    map(() => ({
        type: "LOGIN",
        payload: {
            username: userIdInput.value,
            password: passwordInput.value,
        },
    }))
);

// 登録イベント
const register$ = fromEvent(btnRegister, "click").pipe(
    map(() => ({
        type: "REGISTER",
        payload: {
            name: userIdInput.value,
            password: passwordInput.value,
        },
    }))
);

// ログアウトイベント
const logout$ = fromEvent(logoutBtn, "click").pipe(map(() => ({ type: "LOGOUT" })));

// OTPでのログインイベント
const otpLogin$ = fromEvent(btnOtpLogin, "click").pipe(
    map(() => ({ type: "OTP_LOGIN", payload: { code: otpCodeInput.value } }))
);

// OTPセットアップ開始イベント
const setupOtp$ = fromEvent(btnEnableOtpSetup, "click").pipe(map(() => ({ type: "SETUP_OTP" })));

// OTP検証イベント
const verifyOtp$ = fromEvent(btnVerifyOtp, "click").pipe(
    map(() => ({ type: "VERIFY_OTP", payload: { code: otpVerifyCodeInput.value } }))
);

// --- APIコール --- 

const api = {
    login: (username, password) => 
        fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ username, password })
        }).then(res => res.ok ? res.json() : Promise.reject(res)),

    register: (name, password) =>
        fetch('/api/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, password })
        }).then(res => {
            if (res.ok) return res.text();
            return res.text().then(text => Promise.reject({ status: res.status, message: text }));
        }),

    logout: () => fetch('/api/logout', { method: 'POST' }),

    otpLogin: (code) => 
        fetch('/api/otp/login', { 
            method: 'POST', 
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ code })
        }).then(res => res.ok ? res.json() : Promise.reject(res)),

    setupOtp: () => 
        fetch('/api/otp/setup', { method: 'POST' })
            .then(res => res.ok ? res.json() : Promise.reject(res)),

    verifyOtp: (code) => 
        fetch('/api/otp/verify', { 
            method: 'POST', 
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ code })
        }).then(res => res.ok ? res.text() : Promise.reject(res)),
};

// --- イベント処理 ---

const app$ = merge(login$, register$, logout$, otpLogin$, setupOtp$, verifyOtp$).pipe(
    switchMap(event => {
        setStatus("処理中...");
        appendLog(`イベント: ${event.type}`, "user");

        switch (event.type) {
            case "LOGIN":
                return from(api.login(event.payload.username, event.payload.password)).pipe(
                    map(response => ({ type: "LOGIN_SUCCESS", payload: response, user: event.payload.username })),
                    catchError(err => of({ type: "API_ERROR", payload: `ログイン失敗: ${err.status}` }))
                );
            
            case "REGISTER":
                return from(api.register(event.payload.name, event.payload.password)).pipe(
                    map(response => ({ type: "REGISTER_SUCCESS", payload: response })),
                    catchError(err => of({ type: "API_ERROR", payload: `登録失敗: ${err.status} - ${err.message}` }))
                );

            case "LOGOUT":
                return from(api.logout()).pipe(
                    map(() => ({ type: "LOGOUT_SUCCESS" })),
                    catchError(err => of({ type: "API_ERROR", payload: `ログアウト失敗: ${err.status}` }))
                );

            case "OTP_LOGIN":
                return from(api.otpLogin(event.payload.code)).pipe(
                    map(response => ({ type: "OTP_LOGIN_SUCCESS", payload: response })),
                    catchError(err => of({ type: "API_ERROR", payload: `OTPログイン失敗: ${err.status}` }))
                );

            case "SETUP_OTP":
                return from(api.setupOtp()).pipe(
                    map(response => ({ type: "SETUP_OTP_SUCCESS", payload: response })),
                    catchError(err => of({ type: "API_ERROR", payload: `OTPセットアップ失敗: ${err.status}` }))
                );

            case "VERIFY_OTP":
                return from(api.verifyOtp(event.payload.code)).pipe(
                    map(response => ({ type: "VERIFY_OTP_SUCCESS", payload: response })),
                    catchError(err => of({ type: "API_ERROR", payload: `OTP検証失敗: ${err.status}` }))
                );

            default:
                return of({ type: "NO_OP" });
        }
    })
);

// --- 状態更新とUI描画 ---

app$.subscribe(action => {
    appendLog(`アクション: ${action.type}`, "info");

    switch (action.type) {
        case "LOGIN_SUCCESS":
            if (action.payload.otpRequired) {
                state = { ...state, otpPending: true, user: action.user };
                setStatus(`ようこそ ${state.user} さん、OTPコードを入力してください`);
                showOtpForm();
            } else {
                state = { ...state, loggedIn: true, otpPending: false, user: action.user };
                setStatus(`ようこそ ${state.user} さん`);
                showOtpSetup();
                logoutBtn.classList.remove("hidden");
            }
            break;

        case "REGISTER_SUCCESS":
            setStatus("ユーザー登録成功");
            appendLog(`サーバーメッセージ: ${action.payload}`, "info");
            alert("ユーザー登録に成功しました。ログインしてください。");
            userIdInput.value = "";
            passwordInput.value = "";
            break;

        case "LOGOUT_SUCCESS":
            state = { ...initialState };
            setStatus("未ログイン");
            showLoginForm();
            logoutBtn.classList.add("hidden");
            break;

        case "OTP_LOGIN_SUCCESS":
            state = { ...state, loggedIn: true, otpPending: false };
            setStatus(`ようこそ ${state.user} さん`);
            showOtpSetup();
            logoutBtn.classList.remove("hidden");
            break;

        case "SETUP_OTP_SUCCESS":
            otpQrCodeImage.src = action.payload.qrUrl; // Directly use the Data URI
            otpQrCodeArea.classList.remove("hidden");
            btnEnableOtpSetup.classList.add("hidden");
            setStatus("QRコードをスキャンして確認コードを入力してください");
            break;

        case "VERIFY_OTP_SUCCESS":
            otpQrCodeArea.classList.add("hidden");
            btnEnableOtpSetup.classList.remove("hidden");
            setStatus("OTPが正常に有効化されました");
            alert("OTPが有効になりました！");
            break;

        case "API_ERROR":
            setStatus("エラー");
            appendLog(action.payload, "error");
            break;
    }
});

// --- UI制御関数 ---

function showLoginForm() {
    loginForm.classList.remove("hidden");
    otpLoginForm.classList.add("hidden");
    otpSetupArea.classList.add("hidden");
    otpQrCodeArea.classList.add("hidden");
    btnEnableOtpSetup.classList.remove("hidden");
    userIdInput.value = '';
    passwordInput.value = '';
    otpCodeInput.value = '';
    otpVerifyCodeInput.value = '';
}

function showOtpForm() {
    loginForm.classList.add("hidden");
    otpLoginForm.classList.remove("hidden");
    otpSetupArea.classList.add("hidden");
}

function showOtpSetup() {
    loginForm.classList.add("hidden");
    otpLoginForm.classList.add("hidden");
    otpSetupArea.classList.remove("hidden");
}

// --- ユーティリティ関数 ---

function setStatus(text) {
    statusLabel.textContent = "ステータス: " + text;
}

const MAX_LOGS = 300;

function appendLog(message, type = "user") {
    const now = new Date().toLocaleTimeString();
    const logLine = document.createElement("div");

    const colors = {
        user: "text-green-300",
        info: "text-blue-400",
        error: "text-red-400",
    };
    logLine.className = colors[type] || "text-gray-400";
    logLine.textContent = `[${now}] ${message}`;

    if (logArea.firstChild) {
        logArea.insertBefore(logLine, logArea.firstChild);
    } else {
        logArea.appendChild(logLine);
    }

    while (logArea.childNodes.length > MAX_LOGS) {
        logArea.removeChild(logArea.lastChild);
    }
}

// 初期化
showLoginForm();
setStatus("未ログイン");
