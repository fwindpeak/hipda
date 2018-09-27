package net.jejer.hipda.utils;

import android.content.Context;
import android.text.TextUtils;

import com.vdurmont.emoji.EmojiParser;

import net.jejer.hipda.bean.HiSettingsHelper;
import net.jejer.hipda.bean.ThreadBean;
import net.jejer.hipda.bean.ThreadListBean;
import net.jejer.hipda.service.NotiHelper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HiParserThreadList {

    private static long HOLD_FETCH_NOTIFY = 0;

    public static ThreadListBean parse(Context context, Document doc) {
        // Async check notify
        new parseNotifyRunnable(context, doc).run();
        HiSettingsHelper.updateMobileNetworkStatus(context);

        ThreadListBean threads = new ThreadListBean();

        //parse uid and re-set username if necessary
        if (TextUtils.isEmpty(HiSettingsHelper.getInstance().getUid())) {
            Elements spaceES = doc.select("#um avt a");
            if (spaceES.size() == 1) {
                String spaceUrl = spaceES.first().attr("href");
                if (!TextUtils.isEmpty(spaceUrl)) {
                    String uid = Utils.getMiddleString(spaceUrl, "home.php?mode=space&uid=", "&");
                    String username = Utils.nullToText(spaceES.first().text()).trim();
                    if (!TextUtils.isEmpty(uid)
                            && TextUtils.isDigitsOnly(uid)
                            && !HiSettingsHelper.getInstance().getUid().equals(uid)) {
                        //re-set username if it is not exactly SAME to user inputted with case sensitive
                        if (!HiSettingsHelper.getInstance().getUsername().equals(username)
                                && HiSettingsHelper.getInstance().getUsername().equalsIgnoreCase(username))
                            HiSettingsHelper.getInstance().setUsername(username);
                        //uid will be setted later
                        threads.setUid(uid);
                    }
                }
            }
        }

        Elements tbodyES = doc.select("tbody[id]");
//        Logger.e("tbodyES.size:"+tbodyES.size());
        for (int i = 0; i < tbodyES.size(); ++i) {

            threads.setParsed(true);

            Element tbodyE = tbodyES.get(i);
            ThreadBean thread = new ThreadBean();

			/* title and tid */
            String[] idSpil = tbodyE.attr("id").split("_");
            if (idSpil.length != 2) {
                continue;
            }
            String idType = idSpil[0];
            String idNum = idSpil[1];

//            Logger.e("idType:"+idType);

            thread.setTid(idNum);
            // is stick thread or normal thread
            Boolean isStick = idType.startsWith("stickthread");
            thread.setIsStick(isStick);

            if (isStick && !HiSettingsHelper.getInstance().isShowStickThreads()) {
                continue;
            }

            Elements titleES = tbodyE.select("tr th.common a.s");
//            Logger.e("titleES.size:"+titleES.size());
            if (titleES.size() == 0) {
                continue;
            }
            Element titleLink = titleES.first();
            String title = titleLink.text();
//            Logger.e("title:"+title);
            thread.setTitle(EmojiParser.parseToUnicode(title));

            String linkStyle = titleLink.attr("style");
            if (!TextUtils.isEmpty(linkStyle)) {
                thread.setTitleColor(Utils.getMiddleString(linkStyle, "color:", "").trim());
            }

            Elements typeES = tbodyE.select("th.subject em a");
            if (typeES.size() > 0) {
                thread.setType(typeES.text());
            }

            Elements threadIsNewES = tbodyE.select("td.icn a img");
            if (threadIsNewES.size() > 0) {
                String imgSrc = Utils.nullToText(threadIsNewES.first().attr("src"));
                thread.setNew(imgSrc.contains("new"));
            }

			/*  author, authorId and create_time  */
            Elements authorES = tbodyE.select("td.by");
            if (authorES.size() == 0) {
                continue;
            }
            Elements authorciteAES = authorES.first().select("cite a");
            if (authorciteAES.size() == 0) {
                continue;
            }
            String author = authorciteAES.first().text();
            if (!thread.setAuthor(author)) {
                //author in blacklist
                continue;
            }

            String userLink = authorciteAES.first().attr("href");
            if (userLink.length() < "home.php?mod=space&uid=".length()) {
                continue;
            }
            String authorId = Utils.getMiddleString(userLink, "uid=", "&");
            thread.setAuthorId(authorId);

            thread.setAvatarUrl(HiUtils.getAvatarUrlByUid(authorId));

            Elements threadCreateTimeES = authorES.first().select("em");
            if (threadCreateTimeES.size() == 0) {
                continue;
            }
            String threadCreateTime = threadCreateTimeES.first().text();
            thread.setTimeCreate(threadCreateTime);

            Elements threadUpdateTimeES = tbodyE.select("td.by em span");
            if (threadUpdateTimeES.size() > 0) {
                String threadUpdateTime = threadUpdateTimeES.first().text();
                thread.setTimeUpdate(threadUpdateTime);
            }

			/*  comments and views  */
            Elements nums = tbodyE.select("td.num");
            if (nums.size() == 0) {
                continue;
            }
            Elements comentsES = nums.first().select("a.xi2");
            if (comentsES.size() == 0) {
                continue;
            }
            String comments = comentsES.first().text();
            thread.setCountCmts(comments);

            Elements viewsES = nums.first().select("em");
            if (viewsES.size() == 0) {
                continue;
            }
            String views = viewsES.first().text();
            thread.setCountViews(views);

            // lastpost
            Elements lastpostciteES = tbodyE.select("td.by cite");
            if (lastpostciteES.size() == 0) {
                continue;
            }
            String lastpost = lastpostciteES.first().text();
            thread.setLastPost(lastpost);

            // attachment and picture
            Elements attachs = tbodyE.select("th.new img");
            for (int j = 0; j < attachs.size(); j++) {
                Element attach = attachs.get(j);
                String attach_img_url = attach.attr("src");
                if (attach_img_url.isEmpty()) {
                    continue;
                }
                if (attach_img_url.endsWith("image_s.gif")) {
                    thread.setHavePic(true);
                }
                if (attach_img_url.endsWith("common.gif")) {
                    thread.setHaveAttach(true);
                }
            }

            //get max page number
            Elements pages = tbodyE.select("tr th.common a.s");
            int maxPage = 1;
            if (pages.size() > 0) {
                Element pageLink = pages.get(pages.size() - 1);
                if (!TextUtils.isEmpty(pageLink.attr("href"))) {
                    String lastPage = Utils.getMiddleString(pageLink.attr("href"), "page=", "&");
                    if (!TextUtils.isEmpty(lastPage) && TextUtils.isDigitsOnly(lastPage))
                        maxPage = Utils.parseInt(lastPage);
                }
            }
            thread.setMaxPage(maxPage);

            Logger.e("add thread:"+thread.getTitle());
            threads.add(thread);
        }

        return threads;
    }

    public static class parseNotifyRunnable implements Runnable {

        private Document mDoc;
        private Context mCtx;

        public parseNotifyRunnable(Context context, Document doc) {
            mDoc = doc;
            mCtx = context;
        }

        @Override
        public void run() {
            if (System.currentTimeMillis() > HOLD_FETCH_NOTIFY + 10 * 1000) {
                NotiHelper.fetchNotification(mDoc);
                NotiHelper.showNotification(mCtx);
            }
        }
    }

    public static void holdFetchNotify() {
        HOLD_FETCH_NOTIFY = System.currentTimeMillis();
    }

}
