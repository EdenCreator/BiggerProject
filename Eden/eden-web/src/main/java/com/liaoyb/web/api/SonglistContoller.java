package com.liaoyb.web.api;

import com.liaoyb.base.annotation.AuthPassport;
import com.liaoyb.base.domain.Page;
import com.liaoyb.base.support.exception.PermissionDeniedException;
import com.liaoyb.filestore.model.FileCloudInfo;
import com.liaoyb.filestore.service.FileStoreService;
import com.liaoyb.persistence.domain.dto.*;
import com.liaoyb.persistence.domain.vo.base.Songlist;
import com.liaoyb.persistence.domain.vo.base.SonglistWithSong;
import com.liaoyb.persistence.domain.vo.base.User;
import com.liaoyb.persistence.domain.vo.custom.SongCustom;
import com.liaoyb.persistence.service.SongOfListService;
import com.liaoyb.persistence.service.SongTypeService;
import com.liaoyb.persistence.service.SonglistService;
import com.liaoyb.support.utils.MyResultUtil;
import com.liaoyb.support.utils.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * @author ybliao2
 */
@Controller
@RequestMapping("/api/songlist")
public class SonglistContoller {
    @Autowired
    private SonglistService songlistService;

    @Autowired
    private SongTypeService songTypeService;

    @Autowired
    private SongOfListService songOfListService;

    @Autowired
    private FileStoreService fileStoreService;

    //包含某首歌的歌单
    @RequestMapping("/songListIncludeSong")
    public void songListIncludeSong(HttpServletResponse response, Page<Songlist> page, Long songId){
        page=songlistService.findSonglistCustomsIncludeSong(page,songId);
        MyResultUtil.sendPage(response,page);
    }


    /**
     * 歌单中的歌曲,不用分页
     * @param request
     * @param response
     * @param songlistId
     */
    @RequestMapping("/findSongCustomInSonglist")
    public void findSongCustomInSonglist(HttpServletRequest request, HttpServletResponse response,Long songlistId){
        List<SongCustom>songCustoms=songlistService.findSongCustomsInSonglist(songlistId);
        MyResultUtil.sendList(response,songCustoms);
    }


    //歌单中的音乐SongDto
    @RequestMapping("/findSongDtoInSonglist")
    public void findSongDtoInSonglist(HttpServletRequest request, HttpServletResponse response, Page<SongDto>page, Long songlistId){

        WebUtils.setPage(page,request);
        //当前用户
        User currentUser= WebUtils.getCurrentUser(request);
        page=songlistService.findSongDtoInSonglist(page,songlistId,currentUser!=null?currentUser.getId():null);
        MyResultUtil.sendPage(response,page);

    }


    //榜单中的音乐SongOflist
    @RequestMapping("/findSongOflist")
    public void findSongOflist(HttpServletRequest request, HttpServletResponse response, Page<SongOfList>page, Long songlistid){
        page=songOfListService.findSongInList(page,songlistid);
        MyResultUtil.sendPage(response,page);
    }

    /**
     * 全球榜单
     * @param request
     * @param response
     */
    @RequestMapping("/globalRank")
    public void globalRank(HttpServletRequest request, HttpServletResponse response){
        List<Songlist>songlists=songlistService.findGlobalSonglist();
        MyResultUtil.sendList(response,songlists);
    }




    /**
     * discover,个性推荐里面
     * 歌单推荐
     * @param request
     * @param response
     * @param page
     */
    @RequestMapping("/songlistRecommend")
    public void songlistRecommend(HttpServletRequest request, HttpServletResponse response, Page<Songlist>page){
        //当前用户,如果用户未登录，综合推荐
        User currentUser= WebUtils.getCurrentUser(request);
        if(currentUser!=null){
            page=songlistService.findSonglistUserLike(page,currentUser.getId());
        }else{
            page=songlistService.findSonglistRecommend(page);
        }
        MyResultUtil.sendPage(response,page);
    }


    /**
     * 歌单分类查找,分页
     * @param response
     * @param page
     */
    @RequestMapping("/findSonglistBySongType")
    public void findSonglistBySongType(HttpServletResponse response, Page<Songlist>page, @RequestParam(value = "typeId[]",required = false)Long[] typeId){
        //数组转为集合

        List<Long>typeIds=null;
        if(typeId!=null){
            typeIds= Arrays.asList(typeId);
        }
        page=songTypeService.findSonglistBySongType(page,typeIds);
        MyResultUtil.sendPage(response,page);
    }


    /**
     * 歌单搜索
     * @param request
     * @param response
     * @param page
     * @param searchText
     */
    @RequestMapping("/findSonglist")
    public void findSonglist(HttpServletRequest request, HttpServletResponse response, Page<SonglistCountDto>page,String searchText){
        WebUtils.setPage(page,request);
        page=songlistService.findSonglist(page,searchText);
        MyResultUtil.sendPage(response,page);
    }


    /**
     * 新建歌单
     * @param request
     * @param response
     * @param listName
     */
    @RequestMapping("/newSonglist")
    @AuthPassport
    public void newSonglist(HttpServletRequest request, HttpServletResponse response,String listName) throws Exception {
        User currentUser= WebUtils.getCurrentUser(request);
        Response re=songlistService.createSonglist(currentUser.getId(),listName);
        MyResultUtil.sendResponse(response,re);

    }



    /**
     * 修改歌单信息
     * @param request
     * @param response
     * @param songlist
     * @param coverKey
     * @throws Exception
     */
    @RequestMapping("/updateSonglist")
    @AuthPassport
    public void updateSonglist(HttpServletRequest request, HttpServletResponse response,Songlist songlist,String coverKey) throws Exception {
        //歌单是否自己的
        UserDto userDto=WebUtils.getCurrentUser(request);
        boolean isusers=songlistService.userOwn(songlist.getId(),userDto.getId());
        if(isusers){
            //允许修改

            if(!StringUtils.isEmpty(coverKey)){
                FileCloudInfo fileCloudInfo=fileStoreService.getFileCLoudInfo(coverKey);
                songlist.setCoverUrl(fileCloudInfo.getUrl());
            }

            songlistService.updateSonglist(songlist);
            MyResultUtil.sendSuccess(response,"歌单信息更新成功");
        }else{
            //权限不足
            throw new PermissionDeniedException();
        }
    }


    /**
     * 删除歌单
     * @param request
     * @param response
     * @param songlistId
     * @throws Exception
     */
    @RequestMapping("/dealMySonglist")
    @AuthPassport
    public void dealMySonglist(HttpServletRequest request, HttpServletResponse response,Long  songlistId) throws Exception {
        UserDto userDto=WebUtils.getCurrentUser(request);
        boolean ismy=songlistService.userOwn(songlistId,userDto.getId());
        if(ismy){
            //允许删除
            boolean isDelSuccess=songlistService.dealSonglistById(songlistId);
            if(isDelSuccess){
                MyResultUtil.sendSuccess(response,"删除歌单成功");
            }else{
                MyResultUtil.sendFail(response,"删除歌单失败");
            }
        }else{
            throw new PermissionDeniedException();
        }
    }

    /**
     * 从歌单中移除歌曲
     * 要是用户自己的歌单
     * @param request
     * @param response
     * @param songlistWithSong
     * @throws Exception
     */
    @RequestMapping("/removeSongFromSonglist")
    @AuthPassport
    public void removeSongFromSonglist(HttpServletRequest request, HttpServletResponse response, SonglistWithSong songlistWithSong) throws Exception {

        UserDto userDto=WebUtils.getCurrentUser(request);
        boolean ismy=songlistService.userOwn(songlistWithSong.getSonglistId(),userDto.getId());
        if(ismy){
            //允许移除
            boolean remSucc=songlistService.removeSongFromSonglist(songlistWithSong);
            if(remSucc){
                MyResultUtil.sendSuccess(response,"从歌单中移除歌曲成功");
            }else{
                MyResultUtil.sendFail(response,"从歌单中移除歌曲失败");
            }
        }else{
            throw new PermissionDeniedException();
        }
    }

}
