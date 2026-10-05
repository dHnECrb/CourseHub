function $(id) {
    return document.getElementById(id);
}

function login() {
    console.log("login");
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

    $("msg").textContent=" ";
    $("login-form").submit();
    $("id").value="";
    $("password").value="";
    $("login-button").disabled=true;
    return true;
}


document.addEventListener("keyup", function(e){
    if(e.keyCode === 13) {
        login();
    }
});