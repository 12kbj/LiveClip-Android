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
}
