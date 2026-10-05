package com.example.springdemo.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springdemo.entity.UserPosition;
import com.example.springdemo.entity.UserTask;
import com.example.springdemo.mapper.UserTaskMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserTaskService {
    @Resource
    private UserTaskMapper userTaskMapper;

    @Resource
    private MessageSource msgSource;

    public List<UserTask> getAllTask(){
        return userTaskMapper.selectList(null);
    }

    public List<UserTask> getActiveTask(){
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<UserTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(UserTask::getStart_time, now)
               .ge(UserTask::getDeadline_time, now);

        return userTaskMapper.selectList(wrapper);
    }

    public List<UserTask> getTaskByPosition(HttpSession httpSession){
        String position=BasicFunctions.getPositionFromHttpSession(httpSession);

        LambdaQueryWrapper<UserTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(UserTask::getPosition, position, "all");
        return userTaskMapper.selectList(wrapper);
    }

    public List<UserTask> getActiveTaskByPosition(@NonNull HttpSession httpSession){
        LocalDateTime now = LocalDateTime.now();
        String position=BasicFunctions.getPositionFromHttpSession(httpSession);

        LambdaQueryWrapper<UserTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(UserTask::getStart_time, now)
               .ge(UserTask::getDeadline_time, now)
                .in(UserTask::getPosition, position, "all");
        return userTaskMapper.selectList(wrapper);
    }

    /**判断能否进入选课界面。
     * 若用户未登录(401)、时间未到(1)、时间截止(2)、无权限(403)，则不能进入；否则可以进入。
     * */
    public RequestResult tryGoTask(@NonNull HttpSession httpSession, UserTask userTask){
        if(httpSession.getAttribute("userId")==null){
            return new RequestResult(false, 401, null);
        }
        UserPosition userPosition=(UserPosition) httpSession.getAttribute("userPosition");
        if (!userTask.getPosition().equals(userPosition.getString()) &&
                !userTask.getPosition().equals("all")) {
                    return new RequestResult(false, 403, null);
        }
        LocalDateTime localDateTime=LocalDateTime.now();
        if(userTask.getStart_time().isAfter(localDateTime)){ // 太早
            return new RequestResult(false, 1, null);
        }
        if(userTask.getDeadline_time().isBefore(localDateTime)){ // 太晚
            return new RequestResult(false, 2, null);
        }
        return new RequestResult(true, 0, null);
    }

    /**判断能否进入选课界面。
     * 若用户未登录(401)、时间未到(1)、时间截止(2)、无权限(403)、找不到(404)，则不能进入；否则可以进入。
     * 若同一个网址在UserTask表中有多行，且每个任务都已经截止，则返回时间截止；若存在任务没有开始，则返回时间未到。
     * */
    public RequestResult tryGoTask(@NonNull HttpSession httpSession, String site){
        LambdaQueryWrapper<UserTask> wrapper = new LambdaQueryWrapper<>();
        boolean toLate=true;
        wrapper.eq(UserTask::getSite, site);
//        wrapper.in(UserTask::getPosition, )
        List<UserTask> userTaskList=userTaskMapper.selectList(wrapper);
        if(userTaskList.isEmpty()){ // 找不到这个网址
            return new RequestResult(false, 404, null);
        }
        List<UserTask> userTaskList1 = userTaskList.stream()
                .filter(e->e.belongTo((UserPosition)httpSession.getAttribute("userPosition")))
                .toList();
        if(userTaskList1.isEmpty()){ // 有网址，但身份错误
            return new RequestResult(false, 403, null);
        }
        LocalDateTime localDateTime=LocalDateTime.now();
        for(UserTask task : userTaskList1){
            if(task.getStart_time().isAfter(localDateTime)){
                toLate=false;
                continue;
            }
            if(task.getDeadline_time().isBefore(localDateTime)){
                continue;
            }
            return new RequestResult(true, 200, null);
        }
        if(toLate){
            return new RequestResult(false, 2, null);
        }
        return new RequestResult(false, 1, null);
    }

    public RequestResult tryGoTask(@NonNull HttpSession httpSession, Long userTaskId){
        UserTask userTask=userTaskMapper.selectById(userTaskId);
        if(userTask==null){
            return new RequestResult(false, 404, null);
        }
        return tryGoTask(httpSession, userTask);
    }

    public RequestResult goTask(@NonNull HttpSession httpSession, String taskName, String site, Locale locale){
        RequestResult requestResult=tryGoTask(httpSession, site);
        if(requestResult.isSucceed()){
            BasicFunctions.printTimeHttpSessionAnd(httpSession, (String) httpSession.getAttribute("userId"),
                    msgSource.getMessage("logMessage.goTask", new Object[]{taskName}, Locale.CHINA));
            return requestResult;
        }
        if(requestResult.code()==1){
            return new RequestResult(false, 403,
                    msgSource.getMessage("responseMessage.beforeTaskStartTime", null, locale));
        }
        if(requestResult.code()==2){
            return new RequestResult(false, 403,
                    msgSource.getMessage("responseMessage.afterTaskEndTime", null, locale));
        }
        if(requestResult.code()==401){
            BasicFunctions.printTimeHttpSessionAnd(httpSession,
                    msgSource.getMessage("logMessage.goTaskWithoutLogin", null, Locale.CHINA));
            return new RequestResult(false, 401,
                    msgSource.getMessage("responseMessage.didNotLogin", null, locale));
        }
        if(requestResult.code()==403){
            BasicFunctions.printTimeHttpSessionAnd(httpSession, (String) httpSession.getAttribute("userId"),
                    msgSource.getMessage("logMessage.goTaskFailDueToAccessDenied", new Object[]{taskName}, Locale.CHINA));
            return new RequestResult(false, 403,
                    msgSource.getMessage("responseMessage.accessDenied", null, locale));
        }
        if(requestResult.code()==404){
            BasicFunctions.printTimeHttpSessionAnd(httpSession, (String) httpSession.getAttribute("userId"),
                    msgSource.getMessage("logMessage.taskNotExists", new Object[]{taskName}, Locale.CHINA));
            return new RequestResult(false, 404,
                    msgSource.getMessage("responseMessage.taskNotExists", null, locale));
        }
        throw new IllegalArgumentException("未知的code: "+requestResult.code());
    }



    //wrapper.and(w -> w
    //        .eq(User::getUsername, "Tom")
    //        .or()
    //        .eq(User::getUsername, "Jack")
    //    )
    //    .eq(User::getAge, 18);

//    public int addUserTask(UserTask userTask){
//        userTask.setId(null);
//        return userTaskMapper.insert(userTask);
//    }
//
//    public int updateUserTask(UserTask userTask){
//        return userTaskMapper.updateById(userTask);
//    }
//
//    public int delUserTask(Long id){
//        return userTaskMapper.deleteById(id);
//    }
}
