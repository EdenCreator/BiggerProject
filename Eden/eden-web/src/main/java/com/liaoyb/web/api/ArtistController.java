package com.liaoyb.web.api;

import com.liaoyb.base.domain.Page;
import com.liaoyb.persistence.domain.dto.ArtistDto;
import com.liaoyb.persistence.domain.vo.base.Album;
import com.liaoyb.persistence.domain.vo.base.Artist;
import com.liaoyb.persistence.domain.vo.custom.ObjTest;
import com.liaoyb.persistence.domain.vo.custom.SongCustom;
import com.liaoyb.persistence.service.ArtistService;
import com.liaoyb.support.utils.MyResultUtil;
import com.liaoyb.support.utils.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 歌手Controller
 * @author ybliao2
 */
@Controller
@RequestMapping("/api/artist")
public class ArtistController {

    @Autowired
    private ArtistService artistService;


    //歌手详情ArtistDto
    @RequestMapping("/artistDtoInfo")
    public void artistDtoInfo(HttpServletResponse response,Long artistId){
        ArtistDto artistDto=artistService.findArtistDtoById(artistId);
        MyResultUtil.sendObject(response,artistDto);
    }


    //歌手的专辑
    @RequestMapping("/albumOfArtist")
    public void albumOfArtist(HttpServletResponse response, Page<Album>page, Long artistId){

        page=artistService.findAlbumsBelongToArtist(page,artistId);
        MyResultUtil.sendPage(response,page);
    }


    //歌手的mv
    @RequestMapping("/mvOfArtist")
    public void mvOfArtist(HttpServletResponse response, Page<SongCustom>page, Long artistId){
        page=artistService.findMvBelongToArtist(page,artistId);
        MyResultUtil.sendPage(response,page);
    }

    //// TODO: 2016/3/23  
    //相似歌手)



    //歌手搜索(此方法已失效)
    @RequestMapping("/searchArtistFail")
    @Deprecated
    public void searchArtistFail(HttpServletResponse response,@RequestBody ObjTest hh){

//        System.out.println("obj"+hh);
        Page<Artist>page=new Page<>();
        Artist artist=new Artist();
        page=artistService.artistsQuery(page,artist);
        MyResultUtil.sendPage(response,page);
    }


    /**
     * 歌手条件搜索
     * @param response
     * @param page
     * @param artist
     */
    @RequestMapping("/searchArtist")
    public void searchArtist(HttpServletResponse response, Page<Artist>page, Artist artist){

        page=artistService.artistsQuery(page,artist);
        MyResultUtil.sendPage(response,page);
    }


    /**
     * 总搜索页面
     * 歌手搜索
     * @param response
     * @param page
     * @param searchText
     */
    @RequestMapping("/findArtist")
    public void findArtist(HttpServletRequest request, HttpServletResponse response, Page<ArtistDto>page, String searchText){
        WebUtils.setPage(page,request);
        page=artistService.findArtist(page,searchText);
        MyResultUtil.sendPage(response,page);
    }



   

}
