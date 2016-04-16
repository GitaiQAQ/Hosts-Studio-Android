package me.gitai.hosts.widget;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lidroid.xutils.BitmapUtils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.cache.MD5FileNameGenerator;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

import org.json.JSONObject;
import org.json.JSONTokener;

import me.gitai.hosts.Constant;
import me.gitai.hosts.R;
import me.gitai.hosts.entities.User;
import me.gitai.hosts.utils.ProgressDialogUtil;
import me.gitai.hosts.utils.XutilsHttpClient;
import me.gitai.library.util.SharedPreferencesUtil;
import me.gitai.library.util.StringUtils;
import me.gitai.library.util.ToastUtil;
import me.gitai.library.widget.MaterialDialog;

/**
 * Created by dphdjy on 15-11-5.
 */
public class AccessView extends RelativeLayout implements View.OnClickListener {
    private User user;

    private ImageView mAvatar;
    private TextView mUsername;
    private TextView mEmail;
    private BitmapUtils bitmapUtils;

    private View dialog_view;
    private EditText et_username;
    private EditText et_password;

    private ProgressDialogUtil mProgressDialog;

    private OnLoginStateChange loginStateChange;

    public AccessView(Context context) {
        super(context);
        initLayout(context);
    }

    private void initLayout(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.view_access, this, true);

        bitmapUtils = new BitmapUtils(context);

        mAvatar = (ImageView)view.findViewById(R.id.iv_avatar);
        mUsername = (TextView)view.findViewById(R.id.tv_username);
        mEmail = (TextView)view.findViewById(R.id.tv_email);

        view.setOnClickListener(this);
    }

    public void setOnLoginStateChange(OnLoginStateChange listen){
        loginStateChange = listen;
    }

    public void refesh(){
        user = new User();
        setAvatar(user.getAvatar());
        setUsername(user.getUsername());
        setEmail(user.getEmail());
        if (loginStateChange!=null){
            loginStateChange.OnChange(isLogin());
        }
    }

    public void setAvatar(String url){
        bitmapUtils.display(mAvatar, url);
    }

    public void setUsername(String username) {
        this.mUsername.setText(username);
    }

    public void setEmail(String email) {
        this.mEmail.setText(email);
    }

    public boolean isLogin(){
        return user.isLogin();
    }

    private MaterialDialog.OnClickListener onPositive = new MaterialDialog.OnClickListener() {

        @Override
        public boolean onClick(View v, View MaterialDialog) {
            String username = et_username.getText().toString();
            String password = et_password.getText().toString();

            if (StringUtils.hasText(username)&&StringUtils.hasText(password)){
                mProgressDialog = new ProgressDialogUtil(getContext()).show();

                RequestParams params = new RequestParams();
                params.addBodyParameter("username", username);
                params.addBodyParameter("password", password);

                HttpUtils http = XutilsHttpClient.getInstence(getContext());
                http.send(HttpRequest.HttpMethod.POST, Constant.API_LOGIN, params, new RequestCallBack<String>() {
                    @Override
                    public void onSuccess(ResponseInfo<String> responseInfo) {
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                        JSONTokener jsonParser;
                        jsonParser = new JSONTokener(responseInfo.result);
                        try {
                            JSONObject obj = (JSONObject) jsonParser.nextValue();
                            try {
                                if (obj.getInt("code") > 0) {
                                    ToastUtil.show(obj.getInt("message"));
                                    return;
                                }
                            } catch (Exception e) {
                                String avatar = "https://cdn.v2ex.com/gravatar/"
                                        + new MD5FileNameGenerator().generate(obj.getString("email"))
                                        + "?size=64&d=retro";

                                user.setObjectId(obj.getString("objectId"));
                                user.setAvatar(avatar);
                                user.setEmail(obj.getString("email"));
                                user.setUsername(obj.getString("username"));
                                user.setMaxAge(obj.getInt("maxAge"));

                                user.save();
                            }
                        } catch (Exception e) {
                            ToastUtil.show(e.getLocalizedMessage());
                        }
                        refesh();
                    }

                    @Override
                    public void onFailure(HttpException e, String s) {
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                        ToastUtil.show(s);
                    }
                });
                return true;
            }else{
                ToastUtil.showId(R.string.toast_username_or_password_is_empty);
                return false;
            }
        }
    };

    @Override
    public void onClick(View view) {
        new MaterialDialog(getContext())
                .setTitle(getContext().getString(R.string.action_login))
                .setContentView(R.layout.dialog_login, new MaterialDialog.OnViewInflateListener() {
                    @Override
                    public boolean onInflate(View v) {
                        et_username = (EditText) v.findViewById(R.id.et_username);
                        et_password = (EditText) v.findViewById(R.id.et_password);
                        return false;
                    }
                })
                .setPositiveButton(android.R.string.ok, onPositive)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public interface OnLoginStateChange{
         void OnChange(boolean login);
    }
}
