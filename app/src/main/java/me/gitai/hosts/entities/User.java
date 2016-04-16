package me.gitai.hosts.entities;

import android.content.SharedPreferences;

import java.util.Date;

import me.gitai.library.util.SharedPreferencesUtil;
import me.gitai.library.util.StringUtils;

/**
 * Created by dphdjy on 15-11-5.
 */
public class User {
    private final static String KeyName = "user";

    String objectId;
    String username;
    String email;
    String avatar;
    int maxAge;
    long createTime;

    public User(){
        SharedPreferences user = SharedPreferencesUtil.getInstence(KeyName);
        setAvatar(user.getString("avatar", "http://7xlal5.com1.z0.glb.clouddn.com/hosts/site/logo.png"));
        setMaxAge(user.getInt("maxAge", 0));
        createTime = user.getLong("createTime",0);

        if (isLogin()){
            setObjectId(user.getString("objectId", null));
            setUsername(user.getString("username", "[Username]"));
            setEmail(user.getString("email", "[Email]"));
        }else{
            setObjectId(null);
            setUsername("[Username]");
            setEmail("[Email]");
        }

    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isLogin(){
        if (System.currentTimeMillis() > createTime + maxAge){
            return false;
        }else{
            return true;
        }
    }

    public void save(){
        SharedPreferencesUtil.getEditor(KeyName)
            .putString("objectId", objectId)
            .putString("avatar", avatar)
            .putString("email", email)
            .putString("username", username)
            .putLong("createTime", System.currentTimeMillis())
            .putInt("maxAge", maxAge)
            .commit();
    }
}
