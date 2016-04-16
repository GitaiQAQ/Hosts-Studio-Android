package me.gitai.hosts.entities;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.gitai.hosts.utils.InetAddresses;

public class Host {

    public static final String STR_COMMENT = "#";
    private static final String STR_SEPARATOR = " ";
    private static final String HOST_PATTERN_STR = "^\\s*(" + STR_COMMENT + "?)\\s*(\\S*)\\s*([^" + STR_COMMENT + "]*)" + STR_COMMENT + "?(.*)$";
    private static final Pattern HOST_PATTERN = Pattern.compile(HOST_PATTERN_STR);

    private String mRaw;
    private String mIp;
    private String mHostName;
    private String mComment; // When host entry has a comment at the end, eg: "::1 localhost #myhome"
    private boolean mIsCommented = false; // When host entry starts with #, eg: "#::1 localhost"
    private boolean mIsValid; // When host entry has a valid IP + hostname
    private boolean preBuild = true;

    public Host(String line) {
        mRaw = line;
        mIp = line;
        if (line.startsWith(Host.STR_COMMENT)){
            mIsCommented = true;
        }
        mIsValid = true;
        preBuild = false;
    }

    public Host(String ip, String hostName, String comment) {
        updata(ip,hostName,comment);
    }

    public Host(String line, String ip, String hostName, String comment,
			boolean isCommented, boolean isValid) {
        mRaw = line;
    	mIp = ip;
        mHostName = hostName;
        mComment = comment;
        mIsCommented = isCommented;
        mIsValid = isValid;
	}

    public void updata(String ip, String hostName, String comment){
        mIp = ip;
        mHostName = hostName;
        mComment = comment;
        mIsCommented = false;
        mIsValid = !TextUtils.isEmpty(mIp) && !TextUtils.isEmpty(mHostName)
                && InetAddresses.isInetAddress(mIp);;
        mRaw = toString();
    }

    public void merge(Host src) {
        mIp = src.getIp();
        mHostName = src.getHostName();
        mComment = src.getComment();
        mIsCommented = src.isCommented();
        mIsValid = src.isValid();
    }

    public void build(){
        if (preBuild){
            return;
        }
        Matcher matcher = HOST_PATTERN.matcher(mIp);

        if (matcher.find()) {
            mIsCommented = !TextUtils.isEmpty(matcher.group(1));
            mIp = matcher.group(2);
            mHostName = matcher.group(3).trim();
            mComment = matcher.group(4).trim();
            if (TextUtils.isEmpty(mComment)) {
                mComment = null;
            }

            mIsValid = !TextUtils.isEmpty(mIp) && !TextUtils.isEmpty(mHostName)
                    && InetAddresses.isInetAddress(mIp);
        }
        preBuild = true;
    }

    public boolean isBuild(){
        return preBuild;
    }

    public String getIp() {
        return mIp;
    }
    
    public void setIp(String mIp) {
		this.mIp = mIp;
	}

    public String getHostName() {
        return mHostName;
    }

    public void setHostName(String mHostName) {
        this.mHostName = mHostName;
    }

    public String getComment() {
        return mComment;
    }
    
    public void setComment(String mComment) {
		this.mComment = mComment;
	}

    public boolean isValid() {
        return mIsValid;
    }

    public boolean isCommented() {
        return mIsCommented;
    }

    public void toggleComment() {
        mIsCommented = !mIsCommented;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (isBuild()){
            if (mIsCommented) {
                sb.append(STR_COMMENT);
            }
            if (mIp != null) {
                sb.append(mIp).append(STR_SEPARATOR);
            }
            if (mHostName != null) {
                sb.append(mHostName);
            }
            if (!TextUtils.isEmpty(mComment)) {
                sb.append(STR_SEPARATOR).append(STR_COMMENT).append(mComment);
            }
            return sb.toString();
        }else{
            return mRaw;
        }
    }

    // TODO: 预解析
    public static Host fromString(String line,boolean preBuild) {
        if (preBuild){
            return fromString(line);
        }
        return new Host(line);
    }

    public static Host fromString(String line) {
        Matcher matcher = HOST_PATTERN.matcher(line);
        String ip = null;
        String hostname = null;
        String comment = null;
        boolean isCommented = false;
        boolean isValid = false;

        if (matcher.find()) {
            isCommented = !TextUtils.isEmpty(matcher.group(1));
            ip = matcher.group(2);
            hostname = matcher.group(3).trim();
            comment = matcher.group(4).trim();
            if (TextUtils.isEmpty(comment)) {
                comment = null;
            }

            isValid = !TextUtils.isEmpty(ip) && !TextUtils.isEmpty(hostname)
                    && InetAddresses.isInetAddress(ip);
        }
        return new Host(line, ip, hostname, comment, isCommented, isValid);
    }

    public boolean contains(String raw){
        return mRaw.contains(raw);
    }

    public boolean matcher(String reger){
        Matcher matcher = Pattern.compile(reger).matcher(mRaw);
        return matcher.find();
    }

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mComment == null) ? 0 : mComment.hashCode());
        result = prime * result + ((mHostName == null) ? 0 : mHostName.hashCode());
        result = prime * result + ((mIp == null) ? 0 : mIp.hashCode());
        result = prime * result + (mIsCommented ? 1231 : 1237);
        result = prime * result + (mIsValid ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Host other = (Host) obj;
        if (mComment == null) {
            if (other.mComment != null) {
                return false;
            }
        } else if (!mComment.equals(other.mComment)) {
            return false;
        }
        if (mHostName == null) {
            if (other.mHostName != null) {
                return false;
            }
        } else if (!mHostName.equals(other.mHostName)) {
            return false;
        }
        if (mIp == null) {
            if (other.mIp != null) {
                return false;
            }
        } else if (!mIp.equals(other.mIp)) {
            return false;
        }
        if (mIsCommented != other.mIsCommented) {
            return false;
        }
        if (mIsValid != other.mIsValid) {
            return false;
        }
        return true;
    }
}
