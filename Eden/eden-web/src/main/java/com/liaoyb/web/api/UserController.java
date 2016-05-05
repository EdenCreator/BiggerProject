package com.liaoyb.web.api;

import com.liaoyb.base.annotation.AuthPassport;
import com.liaoyb.base.domain.Page;
import com.liaoyb.base.support.exception.CustomException;
import com.liaoyb.filestore.model.FileCloudInfo;
import com.liaoyb.filestore.service.FileStoreService;
import com.liaoyb.persistence.domain.dto.*;
import com.liaoyb.persistence.domain.vo.base.Artist;
import com.liaoyb.persistence.domain.vo.base.Collect;
import com.liaoyb.persistence.domain.vo.base.User;
import com.liaoyb.persistence.domain.vo.custom.SongCustom;
import com.liaoyb.persistence.service.CollectService;
import com.liaoyb.persistence.service.UserService;
import com.liaoyb.persistence.serviceImpl.UserServiceImpl;
import com.liaoyb.base.support.exception.SourcesNotFoundException;
import com.liaoyb.support.utils.MyResultUtil;
import com.liaoyb.support.utils.WebUtils;
import com.liaoyb.util.BaseWebUtils;
import com.liaoyb.util.MailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

/**
 *
 * @author liao
 * @create 2016-01-30-19:19
 **/
@Controller
@RequestMapping("/api/user")
public class UserController {
    private static Logger logger= LoggerFactory.getLogger(UserServiceImpl.class);


    @Autowired
    private UserService userService;

    @Autowired
    private CollectService collectService;

    @Autowired
    private FileStoreService fileStoreService;


    /**
     * 我的mv,当前用户收藏的mv
     * 需要先登录
     * @param response
     * @param page
     */
    @RequestMapping("/myMV")
    @AuthPassport
    public void myMV(HttpServletRequest request, HttpServletResponse response, Page<SongCustom>page){

        User currentUser= WebUtils.getCurrentUser(request);
        page=userService.findUserMV(page,currentUser!=null?currentUser.getId():null);
        MyResultUtil.sendPage(response,page);
    }




    /**
     * 我的歌手,当前用户
     * @param request
     * @param response
     * @param page
     */
    @RequestMapping("/myArtist")
    @AuthPassport
    public void myArtist(HttpServletRequest request, HttpServletResponse response, Page<ArtistDto>page){
        User currentUser= WebUtils.getCurrentUser(request);
        page=userService.findUserArtist(page,currentUser!=null?currentUser.getId():null);
        MyResultUtil.sendPage(response,page);
    }




    /**
     * 喜欢某首歌的用户
     * @param response
     * @param page
     * @param songId
     */
    @RequestMapping("/findUsersLikeThisSong")
    public void findUsersLikeThisSong(HttpServletResponse response, Page<UserPlay>page, Long songId){
        page=userService.findUsersLikeThisSong(page,songId);
        MyResultUtil.sendPage(response,page);
    }


    /**
     * 添加歌曲到播放列表，可以多首歌曲
     * @param request
     * @param response
     * @param songids
     * @param cover 是否覆盖之前的播放列表,为空，为false表示不覆盖
     */
    @RequestMapping("/addSongToPlaylist")
    public void addSongToPlaylist(HttpServletRequest request,HttpServletResponse response, Boolean cover,@RequestParam(value = "typeId[]",required = false)Long[] songids){
        //如果当前用户没有登录，就什么都不做,给出提示信息
        User currentUser= WebUtils.getCurrentUser(request);
        if(currentUser==null){
            MyResultUtil.sendFail(response,"还未登录，不能同步播放列表哦!");
        }
        userService.addSongsToUserPlay(currentUser.getId(),songids,cover);
    }




    /**
     * 登录
     * 返回登陆信息
     * @param email
     * @param password
     * @return
     */
    @RequestMapping("/login")
    public void login(HttpServletRequest request,HttpServletResponse response,String email,String password){


        UserDto userDto=userService.userLogin(email,password, BaseWebUtils.getIP(request));
        if(userDto!=null){
            //登录成功
            MyResultUtil.sendSuccess(response,"登录成功");
            WebUtils.setCurrentUser(request,userDto);
        }else{
            //登录失败
            MyResultUtil.sendFail(response,"登录失败,请重新登陆");
        }

    }

    /**
     * 注销
     * @param response
     */
    @RequestMapping("/logout")
    public void logout(HttpSession session,HttpServletRequest request  ,HttpServletResponse response){
        //添加历史
        UserDto userDto=WebUtils.getCurrentUser(request);
        if(userDto!=null){
            userService.userLogout(userDto,BaseWebUtils.getIP(request));
        }
        //session失效
        session.invalidate();
        MyResultUtil.sendSuccess(response,"注销成功");
    }


    // TODO: 2016/4/2  qq授权登录注册


    //注册,只能使用邮箱注册
    @RequestMapping("/register")
    public void register(HttpServletRequest request,HttpServletResponse response,String email,String password,String code){
        //验证码
        if(StringUtils.isEmpty(code)||!code.equals(WebUtils.getVerifyCode(request))){
            MyResultUtil.sendFail(response,"验证码有误");
            return;
        }
        User user=new User();
        user.setEmail(email);
        user.setPassword(password);

        Response re=userService.register(user);
        if(re.isFlag()){
            //发送激活邮件
            try {
                String content="<h3>欢迎加入Eden云音乐！</h3><h3> 现在，您可以随心下载海量高品质音乐！</h3><p>在这里不仅可以听歌，更欢迎您把好音乐与别人分享哦！ 一亿爱音乐的人都在这里，碰到同样音乐偏好的朋友，私信say hi吧~ 如果找不到你想听的歌曲，可以邮件给我1600174150@qq.com,王小云很乐意为您服务！</p><h3>点击一下链接激活你的账号(此链接将在30分钟后失效):</h3>" +
                        "<a href='"+ WebUtils.baseUrl(request)+"/api/user/activateAccount?email="+user.getEmail()+"&registerCode="+user.getRegisterCode()+"'>点我激活</a>";
                MailUtil.send(user.getEmail(),"激活账户",content);
            } catch (MessagingException e) {
                logger.error("注册邮件发送失败",e);
                re= new Response().failure("注册邮件发送失败,请检查邮箱是否正确");
            }
        }

        MyResultUtil.sendResponse(response,re);
    }



    /**
     * 激活,跳转到激活成功页面
     * @param request
     * @param email
     * @param registerCode
     * @return
     */
    @RequestMapping("/activateAccount")
    public String activateAccount(HttpServletRequest request,String email,String registerCode){
        if(userService.activateAccount(email,registerCode)){
            //激活成功页面
            return "hint/registerSuccess";
        }else{
            return "hint/registerError";
        }
    }



    /**
     * 用户创建的歌单
     * @param request
     * @param response
     * @param userId
     */
    @RequestMapping("/findSonglistUserCreated")
    public void findSonglistUserCreated(HttpServletRequest request,HttpServletResponse response,Long userId){
        List<SonglistCountDto>songlistCountDtos= userService.findSonglistsUserCreated(userId);
        MyResultUtil.sendList(response,songlistCountDtos);
    }

    /**
     * 用户收藏的歌单
     * @param request
     * @param response
     * @param userId
     */
    @RequestMapping("/findSonglistUserCollected")
    public void findSonglistUserCollected(HttpServletRequest request,HttpServletResponse response,Long userId){
        List<SonglistCountDto>songlistCountDtos=userService.findSonglistsUserCollected(userId);
        MyResultUtil.sendList(response,songlistCountDtos);
    }


    /**
     * 用户关注的用户
     * @param request
     * @param response
     * @param userId
     */
    @RequestMapping("/findUserFocus")
    public void findUserFocus(HttpServletRequest request,HttpServletResponse response,Long userId){

        List<User>users=userService.findUserFocus(userId);
        MyResultUtil.sendList(response,users);
    }

    /**
     * 用户粉丝
     * @param request
     * @param response
     * @param userId
     */
    @RequestMapping("/findUserFans")
    public void findUserFans(HttpServletRequest request,HttpServletResponse response,Long userId){
        List<User>users=userService.findUserFans(userId);
        MyResultUtil.sendList(response,users);

    }


    /**
     * 我的上传
     * @param request
     * @param response
     */
    @RequestMapping("/myUpload")
    @AuthPassport
    public void myUpload(HttpServletRequest request,HttpServletResponse response,Page<SongDto>page) throws SourcesNotFoundException {
        UserDto userDto=WebUtils.getCurrentUser(request);
        page=userService.findUserUpload(page,userDto.getId());
        MyResultUtil.sendPage(response,page);

    }


    /**
     * 用户的好友（即用户关注的用户）
     * @param request
     * @param response
     */
    @RequestMapping("/myFriend")
    @AuthPassport
    public void myFriend(HttpServletRequest request,HttpServletResponse response,User condition){
        UserDto userDto=WebUtils.getCurrentUser(request);
        List<UserFriend>userFriends=userService.findUserFriend(userDto.getId(),condition);
        MyResultUtil.sendList(response,userFriends);
    }


    /**
     * 搜索用户
     * @param request
     * @param response
     * @param page
     * @param searchText
     */
    @RequestMapping("/findUser")
    public void findUser(HttpServletRequest request,HttpServletResponse response,Page<User>page,String searchText){
        WebUtils.setPage(page,request);
        page=userService.findUser(page,searchText);
        MyResultUtil.sendPage(response,page);
    }


    /**
     * 我的歌单
     * @param request
     * @param response
     */
    @RequestMapping("/findMySonglist")
    @AuthPassport
    public void findMySonglist(HttpServletRequest request,HttpServletResponse response) throws SourcesNotFoundException {
        UserDto userDto=WebUtils.getCurrentUser(request);
        List<SonglistCountDto>songlistDtos=userService.findSonglistsUserCreated(userDto.getId());
        MyResultUtil.sendList(response,songlistDtos);

    }

    /**
     * 添加歌曲到歌单
     * @param request
     * @param response
     * @param songlistId
     * @param songId
     */
    @RequestMapping("/addSongToSonglist")
    @AuthPassport
    public void addSongToSonglist(HttpServletRequest request,HttpServletResponse response,Long songlistId,Long songId) {
        UserDto userDto=WebUtils.getCurrentUser(request);
        //歌单必须要自己创建
        Response res=userService.addSongToSonglist(userDto.getId(),songlistId,songId);
        MyResultUtil.sendResponse(response,res);
    }


    /**
     * 添加到我喜欢歌单
     * @param request
     * @param response
     * @param songId
     */
    @RequestMapping("/toggleSongFromLovelist")
    @AuthPassport
    public void toggleSongFromLovelist(HttpServletRequest request,HttpServletResponse response,Long songId) {
        UserDto userDto=WebUtils.getCurrentUser(request);
        Response res=userService.toggleSongFromLovelist(userDto.getId(),songId);
        MyResultUtil.sendResponse(response,res);
    }



    /**
     * 添加到收藏
     * @param request
     * @param response
     * @param collect
     * @throws CustomException
     */
    @RequestMapping("/collect")
    @AuthPassport
    public void collect(HttpServletRequest request, HttpServletResponse response, Collect collect) throws CustomException {
        UserDto userDto=WebUtils.getCurrentUser(request);
        collect.setUserId(userDto.getId());
        collect.setDate(new Date().getTime());
        Response res=collectService.andToCollect(collect);
        MyResultUtil.sendResponse(response,res);
    }


    /**
     * 更新用户信息
     * @param response
     * @param request
     * @param user
     * @param coverKey
     * @throws Exception
     */
    @RequestMapping("/editUserInfo")
    @AuthPassport
    public void editUserInfo(HttpServletResponse response, HttpServletRequest request, User user, String coverKey) throws Exception {
        if(!StringUtils.isEmpty(coverKey)){
            FileCloudInfo coverFileInfo=fileStoreService.getFileCLoudInfo(coverKey);
            if(coverFileInfo==null){
                MyResultUtil.sendFail(response,"修改个人信息失败");
                return;
            }
            user.setAvatarUrl(coverFileInfo.getUrl());
        }

        //重新设置session中当前用户
        //当前用户
        UserDto userDto=WebUtils.getCurrentUser(request);
        user.setId(userDto.getId());
        boolean isSuccess=userService.updateUser(user);



        UserDto lastUserDto=userService.findUserDto(userDto.getId());
        WebUtils.setCurrentUser(request,lastUserDto);
        if(isSuccess){
            MyResultUtil.sendSuccess(response,"个人信息修改成功");
        }else{
            MyResultUtil.sendFail(response,"个人信息修改失败");
        }

    }


}
