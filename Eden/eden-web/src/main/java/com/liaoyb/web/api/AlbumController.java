package com.liaoyb.web.api;

import com.liaoyb.base.domain.Page;
import com.liaoyb.persistence.domain.dto.SongDto;
import com.liaoyb.persistence.domain.vo.base.Album;
import com.liaoyb.persistence.domain.vo.base.User;
import com.liaoyb.persistence.domain.vo.custom.SongCustom;
import com.liaoyb.persistence.service.AlbumService;
import com.liaoyb.support.utils.MyResultUtil;
import com.liaoyb.support.utils.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 专辑Controller
 * @author ybliao2
 */
@Controller
@RequestMapping("/api/album")
public class AlbumController {

    @Autowired
    private AlbumService albumService;

    //专辑详细
    @RequestMapping("/albumInfo")
    public void albumInfo(HttpServletResponse response,Long albumId){
        Album album=albumService.findAlbumById(albumId);
        MyResultUtil.sendObject(response,album);
    }

    //专辑中的所有歌曲，不使用分页
    @RequestMapping("/findSongCustomInAlbum")
    public void findSongCustomInAlbum(HttpServletRequest request, HttpServletResponse response, Long albumId){
        List<SongCustom>songCustoms=albumService.findSongCustomInAlbum(albumId);
        MyResultUtil.sendList(response,songCustoms);
    }

    //专辑中的歌曲列表,分页
    @RequestMapping("/findSongInAlbum")
    public void findSongInAlbum(HttpServletRequest request, HttpServletResponse response, Page<SongDto> page, Long albumId){
        WebUtils.setPage(page,request);
        //当前用户
        User currentUser= WebUtils.getCurrentUser(request);
        page=albumService.findSongDtoInAlbum(page,albumId,currentUser!=null?currentUser.getId():null);
        MyResultUtil.sendPage(response,page);
    }


    /**
     * 专辑搜索
     * @param request
     * @param response
     * @param page
     * @param searchText
     */
    @RequestMapping("/findAlbum")
    public void findAlbum(HttpServletRequest request, HttpServletResponse response, Page<Album> page,String searchText){
        WebUtils.setPage(page,request);
        page=albumService.findAlbum(page,searchText);
        MyResultUtil.sendPage(response,page);
    }

}
