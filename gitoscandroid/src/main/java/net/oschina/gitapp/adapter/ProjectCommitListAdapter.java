package net.oschina.gitapp.adapter;

import android.content.Context;

import net.oschina.gitapp.R;
import net.oschina.gitapp.bean.Commit;
import net.oschina.gitapp.common.StringUtils;

import java.util.List;

/**
 * 项目Commit列表适配器
 * @created 2014-05-26 下午14:43
 * @author 火蚁（http://my.oschina.net/LittleDY）
 * 
 * 最后更新：
 * 更新者：
 */
public class ProjectCommitListAdapter extends CommonAdapter<Commit> {
	
	public ProjectCommitListAdapter(Context context, List<Commit> data, int resource) {
		super(context, data, resource);
	}

    @Override
    public void convert(ViewHolder vh, Commit commit) {
        // 1.加载头像

        String portraitURL = commit.getAuthor() == null ? "" : commit.getAuthor().getNew_portrait();
        if (portraitURL.endsWith("portrait.gif") || StringUtils.isEmpty(portraitURL)) {
            vh.setImageResource(R.id.projectcommit_listitem_userface, R.drawable.mini_avatar);
        } else {
            vh.setImageForUrl(R.id.projectcommit_listitem_userface, portraitURL);
        }

        // 2.显示相关信息
        String name = commit.getAuthor() == null ? commit.getAuthor_name() : commit.getAuthor().getName();
        vh.setText(R.id.projectcommit_listitem_username, name);
        vh.setText(R.id.projectcommit_listitem_content, commit.getTitle());
        vh.setText(R.id.projectcommit_listitem_date, StringUtils.friendly_time(commit.getCreatedAt()));
    }
}