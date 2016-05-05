package com.liaoyb.web.api;

import com.liaoyb.base.SysCode;
import com.liaoyb.base.domain.Page;
import com.liaoyb.base.support.exception.SourcesNotFoundException;
import com.liaoyb.filestore.service.FileStoreService;
import com.liaoyb.persistence.domain.dto.SongDto;
import com.liaoyb.persistence.domain.dto.UserDto;
import com.liaoyb.persistence.domain.vo.base.Songlist;
import com.liaoyb.persistence.domain.vo.base.User;
import com.liaoyb.persistence.domain.vo.custom.SongCustom;
import com.liaoyb.persistence.service.SongService;
import com.liaoyb.persistence.service.SongTypeService;
import com.liaoyb.support.utils.MyResultUtil;
import com.liaoyb.support.utils.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 音乐Controller
 * @author ybliao2
 */
@Controller
@RequestMapping("/api/song")
public class SongController {

    @Autowired
    private SongService songService;

    @Autowired
    private FileStoreService fileStoreService;

    @Autowired
    private SongTypeService songTypeService;



    @RequestMapping("/getSong")
    public void getSong(HttpServletResponse response,Long songId) throws SourcesNotFoundException {
        SongCustom songCustom=songService.findSongCustomById(songId);
        if(songCustom==null){
            throw  new SourcesNotFoundException();
        }
        MyResultUtil.sendObject(response,songCustom);
    }

    //index页面

    /**
     * 发现音乐---随机推荐音乐
     * @param response
     * @param page
     */
    @RequestMapping("/discover")
    public void discover(HttpServletRequest request,HttpServletResponse response, Page<SongDto> page){
        User currentUser= WebUtils.getCurrentUser(request);
        page=songService.findSongDtoRandom(page,currentUser!=null?currentUser.getId():null,SysCode.SONG_TYPE.SONG);
        MyResultUtil.sendPage(response,page);
    }



    /**
     * Top songs
     * 热播mv、歌曲
     * @param songType 歌曲类型（歌曲或者mv）
     * @param area 表示地区
     * @param response
     * @param page
     */
    @RequestMapping("/topSongs")
    public void topSongs(HttpServletResponse response, Page<SongCustom> page, Long songType, Long area){
        page=songService.findSongsRankByPlayCount(page,songType,area);
        MyResultUtil.sendPage(response,page);
    }



    /**
     * new songs
     * 最新歌曲，（歌曲、mv）
     * @param songType 歌曲类型（歌曲或者mv）
     * @param area 表示地区
     * @param response
     * @param page
     */
    @RequestMapping("/newSongs")
    public void newSongs(HttpServletResponse response, Page<SongCustom> page, Long songType, Long area){
        page=songService.findlastSongs(page,songType,area);
        MyResultUtil.sendPage(response,page);
    }




    /**
     * 歌曲(MV)排行榜，综合
     * @param response
     * @param page 表示地区
     * @param songType 歌曲类型（歌曲或者mv）
     * @param area
     */
    @RequestMapping("/songRankComprehensive")
    public void songRankComprehensive(HttpServletResponse response, Page<SongCustom> page, Long songType, Long area){
        page=songService.findlastSongs(page,songType,area);
        MyResultUtil.sendPage(response,page);
    }


    //song页面

    /**
     * 歌曲详细
     * @param response
     * @param songId
     */
    public void songInfo(HttpServletResponse response,Long songId){
        SongCustom songCustom=songService.findSongCustomById(songId);
        if(songCustom!=null){
            MyResultUtil.sendObject(response,songCustom);
        }else{
            MyResultUtil.sendFail(response,"歌曲不存在");
        }
    }


    /**
     * 相似歌曲
     * @param response
     * @param page
     * @param songId
     */
    @RequestMapping("/songSimilar")
    public void songSimilar(HttpServletResponse response, Page<SongCustom>page, Long songId){
        page=songService.findSongCustomsSimilar(page,songId);
        MyResultUtil.sendPage(response,page);
    }


    /**
     * discover,个性推荐里面
     * 单曲推荐,也可以是mv
     * @param songType 歌曲类型（歌曲或者mv）
     * @param request
     * @param response
     */
    @RequestMapping("/singleRecommend")
    public void singlegRecommend(HttpServletRequest request, Page<SongCustom>page, HttpServletResponse response, Long songType){
        //如果用户以登录，根据用户来推荐。没有登录是综合推荐
        User currentUser=WebUtils.getCurrentUser(request);
        if(currentUser!=null){
            page=songService.findSongCustomsUserLike(page,currentUser.getId(),songType);
        }else{
            page=songService.findSongCustomsRecommend(page,songType);
        }
        MyResultUtil.sendPage(response,page);

    }


    /**
     * 播放歌曲
     * @param request
     * @param response
     * @param songId
     */
    @RequestMapping("/playSong")
    public void playSong(HttpServletRequest request,HttpServletResponse response,Long songId){
        UserDto currentUser= WebUtils.getCurrentUser(request);
        if(currentUser!=null){
            songService.playSong(currentUser.getId(),songId);
        }else{
            songService.playSong(null,songId);
        }
        MyResultUtil.sendOperationSuccess(response);

    }


    /**
     * 歌曲搜索,分页
     * @param request
     * @param response
     * @param searchText
     * @param page
     */
    @RequestMapping("/findSong")
    public void findSong(HttpServletRequest request,HttpServletResponse response,Page<SongCustom>page,Integer type,String searchText){
        WebUtils.setPage(page,request);
        page=songService.findSong(page,type,searchText);
        MyResultUtil.sendPage(response,page);
    }



    /**
     * 歌曲，mv下载
     * @param request
     * @param response
     * @param songId
     * @throws Exception
     */
    @RequestMapping("/download/{songId}")
    public void download(HttpServletRequest request,HttpServletResponse response,@PathVariable Long songId) throws Exception {
       songService.download(request,response,songId);

    }




    /**
     * 歌曲分类查找,分页
     * @param response
     * @param page
     */
    @RequestMapping("/findSongCustomsBySongType")
    public void findSongCustomsBySongType(HttpServletResponse response, Page<SongCustom>page, @RequestParam(value = "typeId[]",required = false)Long[] typeId){
        //数组转为集合

        List<Long> typeIds=null;
        if(typeId!=null){
            typeIds= Arrays.asList(typeId);
        }
        page=songTypeService.findSongCustomsBySongType(page,typeIds,SysCode.SONG_TYPE.SONG);
        MyResultUtil.sendPage(response,page);
    }








}
