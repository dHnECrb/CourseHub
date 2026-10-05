package com.example.springdemo.manager;

//https://zhuanlan.zhihu.com/p/1904081307874625155
//枚举天然线程安全且防反射攻击，是实现单例的最佳方式：

/**记录系统登录过的次数，包括一个用户前后多次登录*/
public enum LoginCounter {
    FAIL,    // 登录失败
    ADMIN,   // 管理员登录
    STUDENT, // 学生登录
    TEACHER; // 教师登录

    private int counter = 0;

    public void increment() {
        counter++;
    }

    public int getCount() {
        return counter;
    }

    public static int getTotalCount(){
        return  LoginCounter.ADMIN.getCount()+
                LoginCounter.STUDENT.getCount()+
                LoginCounter.TEACHER.getCount();
    }
}

// 使用
//Singleton.INSTANCE.increment();
//System.out.println(Singleton.INSTANCE.getCount());