package me.gitai.library.util.io;

import me.gitai.library.util.StringUtils;

/**
 * User: mcxiaoke
 * Date: 14-3-25
 * Time: 15:55
 */
public class SafeFileNameGenerator implements NameGenerator {

    @Override
    public String generate(String key) {
        return StringUtils.toSafeFileName(key);
    }

}
