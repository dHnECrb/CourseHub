function $(id) {
    return document.getElementById(id);
}

// 登录请求是否已发出：防止按钮连点或反复按回车导致重复提交
let loginSubmitting = false;

function login() {
    console.log("login");
    if (loginSubmitting) {
        return false;
    }
    if($("id").value==""){
        $("msg").textContent="请输入账号";
        $("id").focus();
        return false;
    }
    if($("password").value==""){
        $("msg").textContent="请输入密码";
        $("password").focus();
        return false;
    }

    loginSubmitting = true;
    $("login-button").disabled=true;
    $("msg").textContent=" ";
    $("login-form").submit();
    $("id").value="";
    $("password").value="";
    return true;
}


document.addEventListener("keyup", function(e){
    if(e.keyCode === 13) {
        login();
    }
});
