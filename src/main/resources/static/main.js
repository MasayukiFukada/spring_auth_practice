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

const mfaSetupArea = document.getElementById("mfaSetupArea");
const btnEnableOtpSetup = document.getElementById("btnEnableOtpSetup");
const otpQrCodeArea = document.getElementById("otpQrCodeArea");
const otpQrCodeImage = document.getElementById("otpQrCodeImage");
const otpVerifyCodeInput = document.getElementById("otpVerifyCode");
const btnVerifyOtp = document.getElementById("btnVerifyOtp");

const btnRegister = document.getElementById("btnRegister");
const btnPasskeyLogin = document.getElementById("btnPasskeyLogin");
const btnRegisterPasskey = document.getElementById("btnRegisterPasskey");


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

// パスキー登録開始イベント
const registerPasskey$ = fromEvent(btnRegisterPasskey, "click").pipe(map(() => ({ type: "PASSKEY_REGISTER_START" })));

// パスキーログイン開始イベント
const loginPasskey$ = fromEvent(btnPasskeyLogin, "click").pipe(map(() => ({ type: "PASSKEY_LOGIN_START" })));


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

    startPasskeyRegister: () => 
        fetch('/passkey/register/start', { method: 'POST' })
            .then(res => res.ok ? res.json() : Promise.reject(res)),

    finishPasskeyRegister: (credential) =>
        fetch('/passkey/register/finish', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credential)
        }).then(res => res.ok ? res.text() : Promise.reject(res)),

    startPasskeyLogin: (username) =>
        fetch('/passkey/login/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: username ? JSON.stringify({ username }) : ''
        }).then(res => res.ok ? res.json() : Promise.reject(res)),

    finishPasskeyLogin: (credential) =>
        fetch('/passkey/login/finish', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credential)
        }).then(res => res.ok ? res.text() : Promise.reject(res)),
};

// --- イベント処理 ---

const app$ = merge(login$, register$, logout$, otpLogin$, setupOtp$, verifyOtp$, registerPasskey$, loginPasskey$).pipe(
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

            case "PASSKEY_REGISTER_START":
                return from(handlePasskeyRegisterStart());

            case "PASSKEY_LOGIN_START":
                return from(handlePasskeyLoginStart());

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
                showMfaSetup();
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
            showMfaSetup();
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

        case "PASSKEY_REGISTER_SUCCESS":
            setStatus("パスキー登録成功");
            alert("新しいパスキーを登録しました。");
            break;

        case "PASSKEY_LOGIN_SUCCESS":
            state = { ...state, loggedIn: true, otpPending: false, user: "Passkey User" }; // TODO: Get actual username
            setStatus(`ようこそ ${state.user} さん`);
            showMfaSetup();
            logoutBtn.classList.remove("hidden");
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
    mfaSetupArea.classList.add("hidden");
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
    mfaSetupArea.classList.add("hidden");
}

function showMfaSetup() {
    loginForm.classList.add("hidden");
    otpLoginForm.classList.add("hidden");
    mfaSetupArea.classList.remove("hidden");
}

// --- WebAuthn ハンドラ ---

async function handlePasskeyRegisterStart() {
    try {
        const creationOptions = await api.startPasskeyRegister();
        appendLog("登録オプション受信", "info");
        console.log("サーバーから受信した登録オプション:", JSON.stringify(creationOptions, null, 2));
        const credential = await navigator.credentials.create({
            publicKey: decodeRegistrationOptions(creationOptions)
        });
        appendLog("クレデンシャル作成成功", "info");
        const credentialForServer = encodeRegistrationCredential(credential);
        console.log("サーバーへ送信するクレデンシャル:", JSON.stringify(credentialForServer, null, 2));
        const response = await api.finishPasskeyRegister(credentialForServer);
        return { type: "PASSKEY_REGISTER_SUCCESS", payload: response };
    } catch (err) {
        console.error("パスキー登録中にエラーが発生しました:", err);
        return { type: "API_ERROR", payload: `パスキー登録失敗: ${err}` };
    }
}

async function handlePasskeyLoginStart() {
    try {
        const requestOptions = await api.startPasskeyLogin(userIdInput.value || null);
        appendLog("認証オプション受信", "info");
        const credential = await navigator.credentials.get({
            publicKey: decodeLoginOptions(requestOptions)
        });
        appendLog("認証成功", "info");
        const credentialForServer = encodeLoginCredential(credential);
        const response = await api.finishPasskeyLogin(credentialForServer);
        return { type: "PASSKEY_LOGIN_SUCCESS", payload: response };
    } catch (err) {
        return { type: "API_ERROR", payload: `パスキーログイン失敗: ${err}` };
    }
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

// --- WebAuthn ヘルパー ---

function bufferToBase64url(buffer) {
    return btoa(String.fromCharCode.apply(null, new Uint8Array(buffer)))
        .replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

function base64urlToBuffer(base64url) {
    const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
    const binStr = atob(base64);
    const bin = new Uint8Array(binStr.length);
    for (let i = 0; i < binStr.length; i++) {
        bin[i] = binStr.charCodeAt(i);
    }
    return bin.buffer;
}

const decodeRegistrationOptions = (options) => {
    const decoded = { ...options };
    decoded.challenge = base64urlToBuffer(decoded.challenge);
    decoded.user.id = base64urlToBuffer(decoded.user.id);
    if (decoded.excludeCredentials) {
        decoded.excludeCredentials = decoded.excludeCredentials.map(cred => {
            const newCred = {
                ...cred,
                id: base64urlToBuffer(cred.id)
            };
            if (newCred.transports === null || newCred.transports === undefined) {
                // transports が null または undefined の場合はプロパティ自体を削除
                delete newCred.transports;
            } else if (!Array.isArray(newCred.transports)) {
                // transports が配列でない場合に、単一の文字列であれば配列に変換する
                console.warn("WebAuthn: excludeCredentials.transports is not an array. Attempting to convert to array.", newCred.transports);
                newCred.transports = [newCred.transports];
            }
            return newCred;
        });
    }
    // appidExclude 拡張が null の場合にエラーとなるブラウザがあるため、プロパティごと削除する
    if (decoded.extensions && decoded.extensions.appidExclude === null) {
        delete decoded.extensions.appidExclude;
    }
    return decoded;
};

const decodeLoginOptions = (options) => {
    const decoded = { ...options };
    decoded.challenge = base64urlToBuffer(decoded.challenge);
    if (decoded.allowCredentials === null || decoded.allowCredentials === undefined) {
        // allowCredentials が null または undefined の場合はプロパティ自体を削除
        delete decoded.allowCredentials;
    } else if (!Array.isArray(decoded.allowCredentials)) {
        // allowCredentials が配列でない場合に、単一の文字列であれば配列に変換する
        console.warn("WebAuthn: allowCredentials is not an array. Attempting to convert to array.", decoded.allowCredentials);
        decoded.allowCredentials = [decoded.allowCredentials];
    } else {
        decoded.allowCredentials = decoded.allowCredentials.map(cred => ({
            ...cred,
            id: base64urlToBuffer(cred.id)
        }));
    }
    // appid 拡張が null の場合にエラーとなるブラウザがあるため、プロパティごと削除する
    if (decoded.extensions && decoded.extensions.appid === null) {
        delete decoded.extensions.appid;
    }
    return decoded;
};

function encodeRegistrationCredential(credential) {
    return {
        id: credential.id,
        rawId: bufferToBase64url(credential.rawId),
        type: credential.type,
        response: {
            clientDataJSON: bufferToBase64url(credential.response.clientDataJSON),
            attestationObject: bufferToBase64url(credential.response.attestationObject),
        },
        clientExtensionResults: {}
    };
}

function encodeLoginCredential(credential) {
    return {
        id: credential.id,
        rawId: bufferToBase64url(credential.rawId),
        type: credential.type,
        response: {
            clientDataJSON: bufferToBase64url(credential.response.clientDataJSON),
            authenticatorData: bufferToBase64url(credential.response.authenticatorData),
            signature: bufferToBase64url(credential.response.signature),
            userHandle: credential.response.userHandle ? bufferToBase64url(credential.response.userHandle) : null,
        },
        clientExtensionResults: {}
    };
}


// 初期化
showLoginForm();
setStatus("未ログイン");
