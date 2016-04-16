package me.gitai.hosts;

/**
 * Created by dp on 15-8-20.
 */
public class Constant {
    public static String BASE_URL = /*BuildConfig.DEBUG?"http://192.168.1.5:3000/":*/"https://hostseditor.avosapps.com/";
    public static String API_BASE_URL = BASE_URL + "api/";

    public static String API_UPDATA = API_BASE_URL + "updata/android/";
    public static String API_HOSTS = API_BASE_URL + "hosts/";
    public static String API_PROJECT = API_BASE_URL + "project/";
    public static String API_PROJECT_PUBLIC = API_PROJECT + "public/";
    public static String API_PROJECT_PRIVATE = API_PROJECT + "private/";
    public static String API_LOGIN = API_BASE_URL + "login/";
    public static String API_LOGOUT = API_BASE_URL + "logout/";
    public static String API_REGISTER = API_BASE_URL + "register/";

    public static String API_QINIU_TOKEN = API_BASE_URL + "upload/token";

    public static String QINIU_UPLOAD = "http://upload.qiniu.com";

    public static String TAG_FAILURE = "hostseditor_failure";
    public static String TAG_NOTHING = "hostseditor_no_data";

    public static String DATA_FOLDER_NAME = "/data/data/" + BuildConfig.APPLICATION_ID + "/";
    public static String FILE_FOLDER_NAME = DATA_FOLDER_NAME + "files/";
    public static String LOGS_FOLDER_NAME = DATA_FOLDER_NAME + "logs/";

    public static String PREFERENCE_KEY_MODEL_QUICKLOAD = "model_quickload";
    public static String PREFERENCE_KEY_PROJECT_DIY = "project_diy";
    public static String PREFERENCE_KEY_PROJECT_PUBLIC = "project_public";
    public static String PREFERENCE_KEY_PROJECT_PRIVATE = "project_private";
    public static String PREFERENCE_KEY_SYSTEM_DEFAULT = "system_default";
    public static String PREFERENCE_KEY_WHILELIST = "while_list";
    public static String PREFERENCE_KEY_RESET_SETTINGS = "reset_settings";
    public static String PREFERENCE_KEY_CLEAR_CACHES = "clear_caches";
    public static String PREFERENCE_KEY_ABOUT_VERSION = "about_version";
}
