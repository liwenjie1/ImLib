package com.yanxiu.im.business.interfaces;

import com.yanxiu.im.bean.TopicItemBean;

/**
 * Created by 朱晓龙 on 2018/5/29 12:24.
 */

public interface RecyclerViewItemLongClickListener {

    boolean onItemLongClicked(int position,TopicItemBean bean);
}
