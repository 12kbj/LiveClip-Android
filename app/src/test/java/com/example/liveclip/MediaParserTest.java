package com.example.liveclip;

import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;

public class MediaParserTest {
    @Test public void acceptsDouyinRedirectHosts() {
        assertTrue(MediaParser.allowed("https://v.douyin.com/UWNFnJbW9gY/"));
        assertTrue(MediaParser.allowed("https://www.iesdouyin.com/share/video/7689802751656039835/"));
        assertTrue(MediaParser.allowed("https://m.douyin.com/share/video/7689802751656039835/"));
    }

    @Test public void rejectsLookalikeOrUnsafeRedirects() {
        assertFalse(MediaParser.allowed("https://iesdouyin.com.evil.example/video/1"));
        assertFalse(MediaParser.allowed("https://notdouyin.com/video/1"));
        assertFalse(MediaParser.allowed("http://www.iesdouyin.com/video/1"));
        assertFalse(MediaParser.allowed("https://user@www.iesdouyin.com/video/1"));
    }
    @Test public void extractsIdsFromVideoNoteAndSlidesLinks() {
        assertEquals("7689467523242316539",MediaParser.shareItemId("https://www.iesdouyin.com/share/video/7689467523242316539/?region=CN"));
        assertEquals("7688596112890995087",MediaParser.shareItemId("https://www.iesdouyin.com/share/note/7688596112890995087/"));
        assertEquals("7688953027634924977",MediaParser.shareItemId("https://www.iesdouyin.com/share/slides/7688953027634924977/"));
        assertEquals("7688953027634924977",MediaParser.shareItemId("https://www.douyin.com/?modal_id=7688953027634924977"));
    }
    @Test public void extractsShareTextOnlyFromTrustedHosts() throws Exception {
        assertEquals("https://v.douyin.com/Abc123/",MediaParser.shareUrl("复制打开抖音 https://v.douyin.com/Abc123/ 立即观看"));
        try {MediaParser.shareUrl("https://v.douyin.com.evil.example/Abc123/");fail("lookalike accepted");}
        catch(Exception expected){assertTrue(expected.getMessage().contains("正确"));}
    }
    @Test public void parsesImageMotionAndVideoCover() throws Exception {
        JSONObject live=new JSONObject("{\"aweme_id\":\"7688596112890995087\",\"image_post_info\":{\"images\":[{\"url_list\":[\"https://example.org/still.jpg\"],\"video_play_addr\":{\"url_list\":[\"https://example.org/motion.mp4\"]}}]}}");
        MediaParser.Result images=MediaParser.parse(live);
        assertEquals("实况图文",images.type);
        assertEquals("https://example.org/motion.mp4",images.images.get(0).live);
        JSONObject video=new JSONObject("{\"aweme_id\":\"7689467523242316539\",\"video\":{\"play_addr\":{\"url_list\":[\"https://example.org/play.mp4\"]},\"cover\":{\"url_list\":[\"https://example.org/cover.jpg\"]}}}");
        assertEquals("https://example.org/cover.jpg",MediaParser.parse(video).videoCover);
        JSONObject uriOnly=new JSONObject("{\"aweme_id\":\"7688596112890995087\",\"images\":[{\"url_list\":[\"https://example.org/still.jpg\"],\"video\":{\"play_addr\":{\"uri\":\"v0200fg10000examplevideoid\"}}}]}");
        assertEquals("https://www.iesdouyin.com/aweme/v1/play/?video_id=v0200fg10000examplevideoid&ratio=1080p",MediaParser.parse(uriOnly).images.get(0).live);
        JSONObject webDetail=new JSONObject("{\"aweme_detail\":{\"aweme_id\":\"7688596112890995087\",\"image_post_info\":{\"images\":[{\"url_list\":[\"https://example.org/still.jpg\"],\"video\":{\"play_addr\":{\"url_list\":[\"https://example.org/motion.mp4\"]}}}]}}}");
        assertEquals("https://example.org/motion.mp4",MediaParser.parse(webDetail).images.get(0).live);
    }

    private String push(String text){return "<script>self.__pace_f.push([1,"+JSONObject.quote(text)+"])</script>";}
    private String image(String still,String motion){
        return "{\"urlList\":[{\"src\":\"https://example.org/"+still+".jpg\"}],\"video\":{\"playAddr\":[{\"src\":\"https://example.org/"+motion+".mp4\"}]}}";
    }
    @Test public void readsDesktopRscSplitAcrossPushes() throws Exception {
        String record="a:[\"$\",{\"awemeId\":\"7689277421266849637\",\"images\":["+image("still","live")+"]}]\n";
        int split=record.length()/2;
        MediaParser.Result result=MediaParser.parsePage(push(record.substring(0,split))+push(record.substring(split)),"7689277421266849637");
        assertEquals("https://example.org/live.mp4",result.images.get(0).live);
    }
    @Test public void upgradesStaticRouterDataFromRsc() throws Exception {
        String html="<script>window._ROUTER_DATA={\"aweme_id\":\"123456\",\"images\":[{\"url_list\":[\"https://example.org/original.jpg\"]}]};</script>";
        html+=push("f:{\"awemeId\":\"123456\",\"images\":["+image("thumb","live")+"]}\n");
        MediaParser.Result r=MediaParser.parsePage(html,"123456");
        assertEquals("https://example.org/original.jpg",r.images.get(0).image);
        assertEquals("https://example.org/live.mp4",r.images.get(0).live);
    }
    @Test public void rejectsRecommendedWorkMotion() throws Exception {
        String html=push("1:{\"awemeId\":\"999999\",\"images\":["+image("still","wrong")+"]}\n");
        try{MediaParser.parsePage(html,"123456");fail("accepted another work");}catch(Exception expected){}
    }
    @Test public void mergeKeepsOriginalSlideIndices() throws Exception {
        MediaParser.Result base=MediaParser.parse(new JSONObject("{\"aweme_id\":\"123456\",\"images\":[{}, {\"url_list\":[\"https://example.org/second.jpg\"]}]}"));
        MediaParser.Result full=MediaParser.parse(new JSONObject("{\"awemeId\":\"123456\",\"images\":["+image("first","first-motion")+","+image("second","second-motion")+"]}"));
        MediaParser.Result r=MediaParser.mergeMotion(base,full);
        assertEquals(2,r.images.size());
        assertEquals("https://example.org/second-motion.mp4",r.images.get(1).live);
        full.id="999999";
        assertSame(base,MediaParser.mergeMotion(base,full));
    }
}
