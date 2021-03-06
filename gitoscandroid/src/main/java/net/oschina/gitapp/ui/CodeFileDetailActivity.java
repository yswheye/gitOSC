package net.oschina.gitapp.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import com.kymjs.rxvolley.client.HttpCallback;

import net.oschina.gitapp.AppConfig;
import net.oschina.gitapp.AppContext;
import net.oschina.gitapp.R;
import net.oschina.gitapp.api.GitOSCApi;
import net.oschina.gitapp.bean.CodeFile;
import net.oschina.gitapp.bean.CodeTree;
import net.oschina.gitapp.bean.Project;
import net.oschina.gitapp.common.Contanst;
import net.oschina.gitapp.common.FileUtils;
import net.oschina.gitapp.common.UIHelper;
import net.oschina.gitapp.ui.baseactivity.BaseActivity;
import net.oschina.gitapp.utils.DialogHelp;
import net.oschina.gitapp.utils.JsonUtils;
import net.oschina.gitapp.utils.MarkdownUtils;
import net.oschina.gitapp.utils.SourceEditor;
import net.oschina.gitapp.utils.T;
import net.oschina.gitapp.widget.TipInfoLayout;

import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * 代码文件详情
 *
 * @author 火蚁
 * @created 2014-06-04
 */
@SuppressWarnings("deprecation")
public class CodeFileDetailActivity extends BaseActivity {

    @InjectView(R.id.webview)
    WebView webview;
    @InjectView(R.id.tip_info)
    TipInfoLayout tipInfo;
    private AppContext mContext;

    private Menu optionsMenu;

    private SourceEditor editor;

    private CodeFile mCodeFile;

    private Project mProject;

    private String mFileName;

    private String mPath;

    private String mRef;

    private String url_link = null;

    private void downloadFile() {
        final String path = AppConfig.DEFAULT_SAVE_FILE_PATH;
        boolean res = FileUtils.writeFile(mCodeFile.getContent().getBytes(),
                path, mFileName);
        if (res) {
            DialogHelp.getOpenFileDialog(this, "文件已经保存在" + AppConfig.DEFAULT_SAVE_FILE_PATH, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    UIHelper.showOpenFileActivity(CodeFileDetailActivity.this, path + "/" + mFileName, CodeTree.getMIME(mFileName));
                }
            }).show();
        } else {
            T.showToastShort(mContext, "保存文件失败");
        }
    }

    private void showEditCodeFileActivity() {
        Intent intent = new Intent(CodeFileDetailActivity.this,
                CodeFileEditActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Contanst.CODE_FILE, mCodeFile);
        bundle.putSerializable(Contanst.PROJECT, mProject);
        bundle.putString(Contanst.BRANCH, mRef);
        bundle.putString(Contanst.PATH, mPath);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置actionbar加载动态
        if (!AppContext.getInstance().isOpenSensor())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_code_file_view);
        ButterKnife.inject(this);
        mContext = AppContext.getInstance();
        Intent intent = getIntent();
        mProject = (Project) intent.getSerializableExtra(Contanst.PROJECT);
        mFileName = intent.getStringExtra("fileName");
        mPath = intent.getStringExtra("path");
        mRef = intent.getStringExtra("ref");
        init();
        loadCode(mProject.getId(), mPath, mRef);

        url_link = GitOSCApi.NO_API_BASE_URL + mProject.getOwner().getUsername()
                + "/" + mProject.getPath() + "/"
                + "blob" + "/" + mRef + "/" + mPath;

        tipInfo.setOnClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadCode(mProject.getId(), mPath, mRef);
            }
        });
    }

    private void init() {
        mTitle = mFileName;
        mActionBar.setTitle(mFileName);
        mSubTitle = mRef;
        mActionBar.setSubtitle(mSubTitle);
        editor = new SourceEditor(webview);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.projet_code_detail_menu, menu);
        optionsMenu = menu;
        updateMenuState();
        return true;
    }

    private void updateMenuState() {
        if (optionsMenu == null) return;
        if (mCodeFile == null) {
            optionsMenu.findItem(R.id.open_browser).setVisible(false);
            optionsMenu.findItem(R.id.copy).setVisible(false);
            optionsMenu.findItem(R.id.download).setVisible(false);
        } else {
            optionsMenu.findItem(R.id.open_browser).setVisible(true);
            optionsMenu.findItem(R.id.copy).setVisible(true);
            optionsMenu.findItem(R.id.download).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.refresh:
                loadCode(mProject.getId(), mPath, mRef);
                break;
            case R.id.copy:
                ClipboardManager cbm = (ClipboardManager) getSystemService(Context
                        .CLIPBOARD_SERVICE);
                cbm.setText(url_link);
                UIHelper.toastMessage(this, "复制成功");
                break;
            case R.id.open_browser:
                if (!mProject.isPublic()) {
                    if (!mContext.isLogin()) {
                        UIHelper.showLoginActivity(CodeFileDetailActivity.this);
                        return false;
                    }
                    url_link = url_link + "?private_token="
                            + AppContext.getToken();
                }
                UIHelper.openBrowser(CodeFileDetailActivity.this, url_link);
                break;
            case R.id.download:
                downloadFile();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadCode(final String projectId, final String path,
                          final String ref_name) {
        GitOSCApi.getCodeFileDetail(projectId, path, ref_name, new HttpCallback() {
            @Override
            public void onSuccess(Map<String, String> headers, byte[] t) {
                super.onSuccess(headers, t);
                webview.setVisibility(View.VISIBLE);
                CodeFile codeFile = JsonUtils.toBean(CodeFile.class, t);
                mCodeFile = codeFile;
                editor.setMarkdown(MarkdownUtils.isMarkdown(mPath));
                editor.setSource(mPath, mCodeFile);

                updateMenuState();
            }

            @Override
            public void onFailure(int errorNo, String strMsg) {
                super.onFailure(errorNo, strMsg);
                webview.setVisibility(View.GONE);
                tipInfo.setLoadError();
            }

            @Override
            public void onPreStart() {
                super.onPreStart();
                tipInfo.setLoading();
                webview.setVisibility(View.GONE);
            }

            @Override
            public void onFinish() {
                super.onFinish();
                tipInfo.setHiden();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ActionBar bar = getSupportActionBar();
            if (bar != null)
                bar.hide();
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            ActionBar bar = getSupportActionBar();
            if (bar != null)
                bar.show();
        }
    }
}
