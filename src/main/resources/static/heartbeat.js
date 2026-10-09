(function () {
    const IDLE_LIMIT = 5 * 60 * 1000;   // 5 分钟无操作视为离开
    const WARN_BEFORE = 60 * 1000;      // 超时前 60 秒弹出警告，可续期
    const HEART_BEAT_TIME = 30 * 1000

    let lastActiveTime = Date.now();
    let warningTimer = null;
    let idleTimer = null;

    let warningShowing = false;
    let visibilityChangedBeforeHeartBeat=false;

    // ===== 自定义弹窗相关 =====
    const mask = document.getElementById('idle-mask');
    const countdownEl = document.getElementById('idle-countdown');
    let countdownInterval = null;

    // 心跳机制，登录成功后启动，每 30 秒上报一次
    let heartbeatTimer = setInterval(heartBeat, HEART_BEAT_TIME);

    function heartBeat(){
        if(visibilityChangedBeforeHeartBeat){
            lastActiveTime=Date.now()-HEART_BEAT_TIME;
            visibilityChangedBeforeHeartBeat=false;
        }
        fetch('/app_1/main/heartbeat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({lastActiveTime:lastActiveTime})
        })
            // .then(response => response.text())
            // .then(data => {
            //     if(data){
            //         window.location.href="/app_1/login"
            //     }
            // })
            .then(response => {
                if (response.status === 401) {
                    alert("登录过期，点击确定以返回登录界面。");
                    clearInterval(heartbeatTimer);
                    heartbeatTimer=null;
                    window.location.href = "/app_1/login";
                }
                else{
                    return response.json();
                }
            })
            // .then(response => response.json())
            .then(data=>{
                console.log("HeartBeat返回数值:"+data);
                if(!data){
                    clearInterval(heartbeatTimer);
                    heartbeatTimer=null;
                }
            })
            .catch(e => {

                alert("网络错误");
                console.error(e);
            })
        ;
    }

    // 1. 节流：mousemove 每 200ms 最多执行一次
    function throttle(fn, delay) {
        let last = 0;
        return function (...args) {
            const now = Date.now();
            if (now - last >= delay) {
                last = now;
                fn(...args);
            }
        };
    }

    // 2. 用户活跃时调用：重置所有计时
    function onUserActive() {
        if(warningShowing){

        }
        else {
            lastActiveTime = Date.now();
            clearTimeout(warningTimer);
            clearTimeout(idleTimer);
            scheduleTimers();
            // hideWarning();
        }
    }

    // 3. 监听各类用户操作事件
    const events = ['mousemove', 'mousedown', 'keydown', 'wheel', 'touchstart', 'scroll'];
    events.forEach(evt => {
        document.addEventListener(evt, throttle(onUserActive, 200), { passive: true });
    });

    // 4. 定时检查（比 setTimeout 更可靠，因为事件可能被节流漏掉）
    function scheduleTimers() {
        warningTimer = setTimeout(showWarning, IDLE_LIMIT - WARN_BEFORE);
        idleTimer = setTimeout(onIdle, IDLE_LIMIT);
    }

    // 5. 超时前警告：倒计时，用户动一下鼠标即可取消
    // function showWarning() {
    //     // 这里可以用你自己的 UI 组件，简单用 confirm 演示
    //     const remain = WARN_BEFORE / 1000;
    //     if (confirm(`检测到您长时间未操作，${remain}秒后将自动退出登录。点击"确定"继续会话。`)) {
    //         onUserActive();       // 用户确认，续期
    //     }
    //     // 如果用户不点，idleTimer 到点后会执行 onIdle
    // }


    function showWarning() {
        // 显示遮罩与弹窗
        mask.style.display = 'flex';

        warningShowing=true;

        // 倒计时
        let remain = WARN_BEFORE / 1000;   // 30
        countdownEl.textContent = remain+"";
        countdownInterval = setInterval(() => {
            remain--;
            if (remain <= 0) {
                clearInterval(countdownInterval);
                hideWarning();
                onIdle();                  // 倒计时结束，执行登出
            } else {
                countdownEl.textContent = remain+"";
            }
        }, 1000);

        // 绑定按钮（用 once 避免重复绑定）
        document.getElementById('idle-stay')
            .addEventListener('click', onStayClicked, { once: true });
        document.getElementById('idle-logout')
            .addEventListener('click', onLogoutClicked, { once: true });
    }

    /** 关闭弹窗、停止倒计时、恢复界面亮度 */
    function hideWarning() {
        warningShowing=false;
        mask.style.display = 'none';
        clearInterval(countdownInterval);
    }

    function onStayClicked() {
        hideWarning();
        lastActiveTime = Date.now();
        clearTimeout(warningTimer);
        clearTimeout(idleTimer);
        scheduleTimers();
    }

    function onLogoutClicked() {
        hideWarning();
        window.location.href='/app_1/main/logout';   // 主动登出
    }

    // 6. 真正超时：调用登出接口并跳回登录页
    async function onIdle() {
        // clearInterval(heartbeatTimer);          // 停止心跳
        window.location.href='/app_1/main/logoutAuto';

    }

    // ========== 页面隐藏/恢复处理 ==========
    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') {
            console.log('页面可见：用户切回了当前标签页或恢复了窗口');
            // if (Date.now() - lastActiveTime > IDLE_LIMIT) {
            //     onIdle();
            // } else {
            //     onUserActive();
            // }
            visibilityChangedBeforeHeartBeat=true;
            if(heartbeatTimer==null){
                heartbeatTimer = setInterval(heartBeat, HEART_BEAT_TIME);
            }
        } else {
            visibilityChangedBeforeHeartBeat=false;
            console.log('页面隐藏：用户切换到了其他标签页、最小化了窗口或离开了浏览器');
            clearTimeout(warningTimer);
            clearTimeout(idleTimer);
        }
    });

    // 启动
    onUserActive();
})();