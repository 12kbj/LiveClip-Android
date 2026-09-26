package com.example.liveclip;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MediaParser {
    private static final Set<String> HOSTS = Set.of("v.douyin.com", "www.douyin.com", "douyin.com", "iesdouyin.com");
    private MediaParser() {}
    public static String shareUrl(String text) throws Exception {
        Matcher matcher = Pattern.compile("https://[^\\s<>\"'，。]+", Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) { String url = matcher.group().replaceAll("[）)】]+$", ""); if (allowed(url)) return url; }
        throw new Exception("请粘贴抖音作品分享链接");
    }
    public static boolean allowed(String raw) {
        try { URL url = new URL(raw); return "https".equals(url.getProtocol()) && HOSTS.contains(url.getHost().toLowerCase()) && url.toURI().getUserInfo() == null; } catch (Exception e) { return false; }
    }
    public static JSONObject extractJson(String html) throws Exception {
        for (String marker : new String[]{"window._ROUTER_DATA =", "window._ROUTER_DATA="}) {
            int at = html.indexOf(marker); if (at < 0) continue;
            int start = html.indexOf('{', at + marker.length()); if (start < 0) continue;
            int depth = 0; boolean quoted = false, escaped = false;
            for (int i = start; i < html.length(); i++) { char c = html.charAt(i);
                if (quoted) { if (escaped) escaped = false; else if (c == '\\') escaped = true; else if (c == '"') quoted = false; }
                else if (c == '"') quoted = true; else if (c == '{') depth++; else if (c == '}' && --depth == 0) return new JSONObject(html.substring(start, i + 1));
            }
        }
        Matcher m = Pattern.compile("<script[^>]+id=[\"']RENDER_DATA[\"'][^>]*>([^<]+)</script>").matcher(html);
        if (m.find()) return new JSONObject(URLDecoder.decode(m.group(1), StandardCharsets.UTF_8));
        throw new Exception("分享页没有作品数据，可能遇到页面改版或访问限制");
    }
    private static JSONObject findItem(Object root, Set<Object> visited, int[] budget) {
        if (root == null || budget[0]-- <= 0 || visited.contains(root)) return null;
        visited.add(root);
        if (root instanceof JSONObject) { JSONObject o = (JSONObject) root;
            if ((o.has("aweme_id") || o.has("awemeId") || o.has("item_id")) && (o.has("images") || o.has("image_list") || o.has("video"))) return o;
            java.util.Iterator<String> keys = o.keys(); while (keys.hasNext()) { String k = keys.next(); JSONObject found = findItem(o.opt(k), visited, budget); if (found != null) return found; }
        } else if (root instanceof JSONArray) { JSONArray a = (JSONArray) root; for (int i = 0; i < a.length(); i++) { JSONObject found = findItem(a.opt(i), visited, budget); if (found != null) return found; } }
        return null;
    }
    private static String url(Object value) { if (value instanceof String) return ((String)value).startsWith("https://") ? (String)value : "";
        if (value instanceof JSONArray) { JSONArray a=(JSONArray)value; for(int i=0;i<a.length();i++){String found=url(a.opt(i));if(!found.isEmpty())return found;} }
        if (value instanceof JSONObject) { JSONObject o=(JSONObject)value;for(String k:new String[]{"url_list","urlList","url","uri","src"}){String found=url(o.opt(k));if(!found.isEmpty())return found;} }
        return "";
    }
    private static String choose(JSONObject o, String... keys) { if(o==null)return "";for(String k:keys){String found=url(o.opt(k));if(!found.isEmpty())return found;}return ""; }
    public static Result parse(JSONObject root) throws Exception {
        JSONObject item=findItem(root,new HashSet<>(),new int[]{20000}); if(item==null)throw new Exception("未找到视频或图文作品数据");
        Result result=new Result();result.title=item.optString("desc",item.optString("title","抖音作品"));result.author=item.optJSONObject("author") == null ? "" : item.optJSONObject("author").optString("nickname", "");
        JSONArray images=item.optJSONArray("images");if(images==null)images=item.optJSONArray("image_list");
        if(images!=null)for(int i=0;i<images.length();i++){JSONObject image=images.optJSONObject(i);if(image==null)continue;String still=choose(image,"url_list","urlList","download_url","display_image","origin_image");String live=choose(image,"video","video_url","live_photo");if(!still.isEmpty())result.images.add(new Asset(still,live));}
        if(!result.images.isEmpty()){result.type=result.images.stream().anyMatch(a->!a.live.isEmpty())?"实况图文":"图文";return result;}
        JSONObject video=item.optJSONObject("video"); result.video=choose(video,"play_addr","playAddr","play_addr_h264");if(result.video.isEmpty())result.video=choose(item,"video_play_addr");result.video=result.video.replaceAll("playwm(?=[/?])","play");
        if(result.video.isEmpty())throw new Exception("作品没有可用媒体地址");result.type="视频";return result;
    }
    public static final class Asset { public final String image,live; Asset(String image,String live){this.image=image;this.live=live;} }
    public static final class Result { public String title,author,type,video=""; public final ArrayList<Asset> images=new ArrayList<>(); }
}
