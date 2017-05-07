package com.zoomweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by XuPeng on 2017/5/7.
 */

public class Now {
    @SerializedName("tmp")
    public String temperature;
    @SerializedName("cond")
    public More more;

    public class More{
        @SerializedName("txt")
        public String info;
    }
}
