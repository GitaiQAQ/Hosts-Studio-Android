package me.gitai.library.widget;

import android.widget.EditText;
import android.text.Layout;
import android.text.TextUtils;
import android.widget.ScrollView;
import android.util.AttributeSet; 

import android.content.Context;

public class CodeEditor extends EditText{
	public CodeEditor(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	private void init(Context context){

	}

	public String getCode(){
		return getText().toString();
	}
}
