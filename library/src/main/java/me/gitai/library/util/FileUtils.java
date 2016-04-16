package me.gitai.library.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;

/**
 * Created by dphdjy on 15-11-6.
 */
public class FileUtils {
    public static String getPath(Context ctx,Uri uri){
        if ("content".equalsIgnoreCase(uri.getScheme())){
            String[] projection = {"_data"};
            Cursor cursor = null;
            try{
                cursor = ctx.getContentResolver().query(uri,projection,null,null,null);
                int colemn_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()){
                    return cursor.getString(colemn_index);
                }
            }catch (Exception ex){

            }
        }else if("file".equalsIgnoreCase(uri.getScheme())){
            return uri.getPath();
        }
        return null;
    };
}
