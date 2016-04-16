package me.gitai.hosts.tasks;

import android.os.AsyncTask;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.http.client.HttpRequest;

import org.json.JSONObject;
import org.json.JSONTokener;

import me.gitai.hosts.Constant;
import me.gitai.hosts.tasks.UpdataTask.UpdataInfo;
import me.gitai.library.util.ToastUtil;

public class UpdataTask extends AsyncTask<Void, Void, UpdataInfo> {
	@Override
	protected UpdataInfo doInBackground(Void... voids) {
		UpdataInfo updata=null;
		try {
			String result = new HttpUtils().sendSync(HttpRequest.HttpMethod.GET, Constant.API_UPDATA).readString();
			//HttpUtil.requestByHttpGet1(Constant.API_UPDATA);
			if (result.isEmpty()) {
				throw new Exception();
			}

			JSONTokener jsonParser;
			jsonParser = new JSONTokener(result);
			JSONObject obj = (JSONObject) jsonParser.nextValue();

			updata = new UpdataInfo(obj);
		} catch (Exception e) {
			e.printStackTrace();
			ToastUtil.show(e.getLocalizedMessage());
		}
		return updata;
	}

	public class UpdataInfo{
		 int versionCode;
		 String versionLabel;

		 int levelCode;
		 String levelLabel;

		 String title;
		 String msg;

		 String url;

		public UpdataInfo(JSONObject obj) {
			try{
				this.versionCode = obj.getInt("versionCode");

				this.versionLabel = obj.getString("versionLabel");

				this.levelCode = obj.getInt("levelCode");
				this.levelLabel = obj.getString("levelLabel");

				this.title = obj.getString("title");
				this.msg = obj.getString("msg");

				this.url = obj.getString("url");
			}catch (Exception e) {

			}
		}

		public int getVersionCode() {
			return versionCode;
		}

		public void setVersionCode(int versionCode) {
			this.versionCode = versionCode;
		}

		public String getVersionLabel() {
			return versionLabel;
		}

		public void setVersionLabel(String versionLabel) {
			this.versionLabel = versionLabel;
		}

		public int getLevelCode() {
			return levelCode;
		}

		public void setLevelCode(int levelCode) {
			this.levelCode = levelCode;
		}

		public String getLevelLabel() {
			return levelLabel;
		}

		public void setLevelLabel(String levelLabel) {
			this.levelLabel = levelLabel;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getMsg() {
			return msg;
		}

		public void setMsg(String msg) {
			this.msg = msg;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}
	}


}
