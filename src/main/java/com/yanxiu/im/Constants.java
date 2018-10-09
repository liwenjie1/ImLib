package com.yanxiu.im;

import com.test.yanxiu.common_base.utils.SystemUtil;
import com.yanxiu.lib.yx_basic_library.util.YXSystemUtil;

/**
 * Created by cailei on 02/03/2018.
 */

public class Constants {
//    public static final long imId = 9;
//    public static final String imToken = "fb1a05461324976e55786c2c519a8ccc";

    public static String token;

    // start *** im 用户信息，module对接用，因为已经写入所有code中，所以这样替换最为便捷
    public static long imId = -1;
    public static String imToken;
    public static String imAvatar;
    // end *** im 用户信息

    //客户端类型 学员端还是管理端
    //学员端
    public static final int APP_TYPE_STUDENT = 0X01;
    //管理端 这里需要角色扩展  班主任 区域管理员  项目管理员等
    public static final int APP_TYPE_ADMIN = 0X02;
    //未定义
    public static final int APP_TYPE_UNDEFINE = -1;

    public static int APP_TYPE = APP_TYPE_UNDEFINE;


    //字段  是否显示通讯录功能
    public static boolean showContacts = true;
    //字段 是否显示设置功能  （设置 消息免打扰  ）
    public static boolean showTopicSetting = true;
    //字段 是否显示设置功能  （设置 topic 禁言功能  ）
    public static boolean showTopicSilent = true;

    public static final String kShareTopic = "Share Topic";
    public static final String kCreateTopicMemberId = "Create Topic Member Id";
    public static final String kCreateTopicMemberName = "Create Topic Member Name";
    public static final String kFromTopicId = "From Topic Id";

    public static final int IM_REQUEST_CODE_BASE = 800;
    public static final int IM_REQUEST_CODE_MSGLIST = IM_REQUEST_CODE_BASE + 1;
    public static final int IM_REQUEST_CODE_CONTACT = IM_REQUEST_CODE_BASE + 2;




    /*应付 */
    public static final String OS = "android";
    public static final String osType = "0";
    public static final String pcode = "010110000";
    public static final String BRAND = SystemUtil.getBrandName();
    public static final String OPERTYPE = "app.upload.log";
    public static String deviceId = "-";
    public static String version = String.valueOf(YXSystemUtil.getVersionCode());
    public static String versionName = YXSystemUtil.getVersionName();
}
