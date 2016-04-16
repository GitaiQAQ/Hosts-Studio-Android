package me.gitai.hosts.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.client.HttpRequest;

import java.io.File;

import me.gitai.hosts.utils.XutilsHttpClient;
import me.gitai.hosts.Constant;

public class UploadTask extends AsyncTask<Void, Void, String> {
	private Context ctx;
	private String title,des;
	private File file;
	private boolean lock;
	private String uid;
	private long time;
	public UploadTask(Context c,String t, File f, boolean l, String d,String u,long t1){
		this.ctx = c;
		this.title = t;
		this.file = f;
		this.des=d;
		this.lock=l;
		this.uid = u;
		this.time = t1;
	}

	@Override
	protected String doInBackground(Void... voids) {
		String result = null;
		try {
			result = XutilsHttpClient.getInstence(ctx).sendSync(HttpRequest.HttpMethod.GET, Constant.API_QINIU_TOKEN).readString();
			if (result.isEmpty()) {
				throw new Exception();
			}
			RequestParams params = new RequestParams();
			params.addBodyParameter("token",result);
			params.addBodyParameter("key",String.format("/hosts/files/%s/%s",uid,time));
			params.addBodyParameter("file",file);
			result = XutilsHttpClient.getInstence(ctx).sendSync(HttpRequest.HttpMethod.POST, Constant.QINIU_UPLOAD, params).readString();
			if (result.isEmpty()) {
				throw new Exception();
			}
			RequestParams params1 = new RequestParams();
			params1.addBodyParameter("title",title);
			params1.addBodyParameter("url","http://7xlal5.com1.z0.glb.clouddn.com"+String.format("/hosts/files/%s/%s",uid,time));

			if (lock){
				params1.addBodyParameter("lockStr", "lock");
			}

			params1.addBodyParameter("description",des);
			result = XutilsHttpClient.getInstence(ctx).sendSync(HttpRequest.HttpMethod.POST, Constant.API_HOSTS, params1).readString();
			if (result.isEmpty()) {
				throw new Exception();
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return e.getLocalizedMessage();
		}
	}
}
