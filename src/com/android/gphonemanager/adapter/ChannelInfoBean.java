package com.android.gphonemanager.adapter;

public class ChannelInfoBean {
	
	private int id;
	private int iconRes;
	private String title;
    private String describtion;  
    private int type;  
    private onGridViewItemClickListener onClickListener;  
	
	public ChannelInfoBean(int id, int iconRes, String title) {
		this.id = id;
		this.iconRes = iconRes;
		this.title = title;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public void setIconRes(int iconRes) {
		this.iconRes = iconRes;
	}
	
	public int getIconRes() {
		return iconRes;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
    public onGridViewItemClickListener getOnClickListener() {  
        return onClickListener;  
    }  
    public void setOnClickListener(onGridViewItemClickListener onClickListener) {  
        this.onClickListener = onClickListener;  
    }  
	
    public interface onGridViewItemClickListener  
    {  
        public abstract void ongvItemClickListener(int position);  
    }  
	
}
