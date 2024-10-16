package org.noear.wood;

import org.noear.wood.utils.StringUtils;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TmlAnalyzer {
    private static final ReentrantLock SYNC_LOCK = new ReentrantLock();

    private static Map<String, TmlBlock> libs = new HashMap<>();

    public static TmlBlock get(String tml, Map<String, Object> args) {
        TmlBlock block = libs.get(tml);

        if (block == null) {
            SYNC_LOCK.lock();
            try {
                block = libs.get(tml);

                if (block == null) {
                    block = build(tml, args);
                    libs.put(tml, block);
                }
            } finally {
                SYNC_LOCK.unlock();
            }
        }

        return block;
    }

    private static TmlBlock build(String tml, Map<String, Object> args) {
        TmlBlock cache = new TmlBlock();
        cache.sql = tml;
        cache.sql2 = tml;
        cache.marks = parse(tml);

        Map<String, String> tmpList = new HashMap<>();

        for (TmlMark tm : cache.marks) {
            Object val = args.get(tm.name);
            if (val instanceof Iterable) { //支持数组型参数
                tmpList.put(tm.mark, tm.mark);
            } else {
                if (tm.mark.startsWith("@")) {
                    tmpList.put(tm.mark, "?");
                } else {
                    String valStr = (val == null ? "" : val.toString());
                    tmpList.put(tm.mark, valStr);
                }
            }
        }

        //按长度倒排KEY
        List<String> keyList = new ArrayList<>(tmpList.keySet());
        Collections.sort(keyList, (o1, o2) -> {
            int len = o2.length() - o1.length();
            if (len > 0) {
                return 1;
            } else if (len < 0) {
                return -1;
            } else {
                return 0;
            }
        });

        for (String key : keyList) {
            cache.sql2 = cache.sql2.replace(key, tmpList.get(key));
        }

        return cache;
    }

    private static List<TmlMark> parse(String tml) {
        List<TmlMark> marks = new ArrayList<>();

        Pattern pattern = Pattern.compile("@(\\w+)|[@\\$]{1}\\{(\\w+)\\}");
        Matcher m = pattern.matcher(tml);

        while (m.find()) {
            String mark = m.group(0);
            String name = m.group(1);
            if (StringUtils.isEmpty(name)) {
                name = m.group(2);
            }

            marks.add(new TmlMark(mark, name));
        }

        return marks;
    }
}
