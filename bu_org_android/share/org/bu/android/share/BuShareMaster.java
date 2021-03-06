package org.bu.android.share;

import java.io.File;
import java.util.HashMap;

import org.bu.android.R;
import org.bu.android.acty.BuActivity;
import org.bu.android.app.BuUILogic;
import org.bu.android.misc.BuFileHolder;
import org.bu.android.misc.BuStringUtils;
import org.bu.android.share.misc.ShareImageUtils;
import org.bu.android.share.misc.ShareOptionHolder;
import org.bu.android.share.misc.ShareSmsUtils;
import org.bu.android.share.misc.ShareUtils;
import org.bu.android.widget.BuMenu;
import org.bu.android.widget.BuMenuMaster.BuMenuListener;
import org.bu.android.wxapi.BuWXMaster;
import org.bu.android.yxapi.BuYXMaster;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;

/**
 * 需要在Assets下放置 bu_share_config.xml配置文件
 * 
 * @author jxs
 * @date 2015-3-30 下午12:01:19
 */
public interface BuShareMaster {

	class BuShareViewHoler {
		BuMenu weiMiMenu;
	}

	public abstract class BuShareListener implements PlatformActionListener, BuShareWidget.OnTargetSelectedListener {
		public abstract View getRootView();

		private Context mContext;
		private BuMenu menu;

		public BuShareListener(Context mContext) {
			super();
			this.mContext = mContext;
		}

		@SuppressLint("InflateParams")
		public View getMenus(BuMenu menu) {
			this.menu = menu;
			View view = LayoutInflater.from(mContext).inflate(R.layout.wm_share_widget, null);
			TextView notes = (TextView) view.findViewById(R.id.notes);
			BuShareWidget bu_share_widget = (BuShareWidget) view.findViewById(R.id.bu_share_widget);
			onLayoutInflater(view, notes, bu_share_widget);
			bu_share_widget.setOnTargetSelectedListener(this);
			return view;
		}

		public void onLayoutInflater(View view, TextView notes, BuShareWidget bu_share_widget) {
			notes.setText("分享到以下社交平台");
		}

		@Override
		public void onTargetSelected(BuShareAppInfo targetAppInfo) {
			this.menu.dismiss();
		}

		@Override
		public void onCancel(Platform arg0, int arg1) {

		}

		@Override
		public void onComplete(Platform arg0, int arg1, HashMap<String, Object> arg2) {

		}

		@Override
		public void onError(Platform arg0, int arg1, Throwable arg2) {

		}

	}

	public class BuShareLogic extends BuUILogic<BuActivity, BuShareViewHoler> implements BuWXMaster, BuYXMaster {

		private BuWXLogic weixinLogic;
		private BuYXLogic yixinLogic;
		private BuShareListener shareListener;
		private BuMenuListener buMenuListener;
		private BuShareInfo shareInfo = new BuShareInfo();

		public BuShareLogic(BuActivity t, BuShareListener _listener) {
			super(t, new BuShareViewHoler());

			mViewHolder.weiMiMenu = new BuMenu(mActivity);

			this.shareListener = _listener;
			weixinLogic = new BuWXLogic(mActivity);
			yixinLogic = new BuYXLogic(mActivity);

			buMenuListener = new BuMenuListener() {

				@Override
				public View getMenus(BuMenu menu) {
					return shareListener.getMenus(menu);
				}
			};
		}

		private void shareSDK_share(BuShareAppInfo appInfo, String title, String content, String comment, String imgPath, String url) {

			if (appInfo.getTargetAppDefine().id == BuShareAppDefine.NULL.id) {
				return;
			}
			ShareOptionHolder mOptionHolder = new ShareOptionHolder(mActivity, shareListener);
			mOptionHolder.disableSSOWhenAuthorize();
			// 分享时Notification的图标和文字
			mOptionHolder.setNotification(R.drawable.ic_launcher, mActivity.getString(R.string.app_name));
			// title标题，印象笔记、邮箱、信息、微信、人人网和QQ空间使用
			mOptionHolder.setTitle(title);
			// titleUrl是标题的网络链接，仅在人人网和QQ空间使用
			mOptionHolder.setTitleUrl(url);
			// text是分享文本，所有平台都需要这个字段
			mOptionHolder.setText(content);
			// imagePath是图片的本地路径，Linked-In以外的平台都支持此参数
			mOptionHolder.setImagePath(imgPath);
			// url仅在微信（包括好友和朋友圈）中使用
			mOptionHolder.setUrl(url);
			// comment是我对这条分享的评论，仅在人人网和QQ空间使用
			mOptionHolder.setComment(comment);
			// site是分享此内容的网站名称，仅在QQ空间使用
			mOptionHolder.setSite(mActivity.getString(R.string.app_name));
			// siteUrl是分享此内容的网站地址，仅在QQ空间使用
			mOptionHolder.setSiteUrl(url);
			mOptionHolder.share(appInfo);
		}

		protected Bitmap thumbZip(String imagePath) {
			Bitmap thumb = ShareUtils.decodeFile(new File(imagePath));
			thumb = ShareImageUtils.compressBySize(thumb, 80, 80);
			thumb = ShareImageUtils.compressByQuality(thumb, 32);
			thumb = ShareImageUtils.compressBySize(thumb, 32);
			return thumb;
		}

		public void shareText(BuShareInfo shareInfo) {
			share(shareInfo, true);
		}

		public void shareWebpage(BuShareInfo shareInfo) {
			share(shareInfo, false);
		}

		private void share(BuShareInfo shareInfo, boolean isText) {
			this.shareInfo = shareInfo;
			this.shareInfo.setShareText(isText);
			mActivity.dismissLoading();
			mViewHolder.weiMiMenu.show(shareListener.getRootView(), buMenuListener);// 显示分享到
		}

		public void onTargetSelected(BuShareAppInfo targetAppInfo) {
			share(targetAppInfo, shareInfo);
		}

		private void share(BuShareAppInfo targetAppInfo, BuShareInfo info) {

			Bitmap thumb = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.ic_launcher);
			File file = new File(BuFileHolder.RandomFileName.getPicFileName());
			if (!BuStringUtils.isEmpety(info.getImagePath())) {
				file = new File(info.getImagePath());
			}
			if (BuStringUtils.isEmpety(info.getImagePath()) || file.exists() || file.length() == 0) {
				thumb = BuFileHolder.compressImage(thumb);
				BuFileHolder.savePic(thumb, file);
			}

			String title = info.getTitle();
			String content = info.getContent();

			if (targetAppInfo.getTargetAppDefine().id == BuShareAppDefine.SMS.id) {
				ShareSmsUtils.sendtoMessage(mActivity, info.getNumber(), title + "\n" + content + " \n" + info.getUrl());
			} else if (targetAppInfo.getTargetAppDefine().id == BuShareAppDefine.WECHAT.id) {
				if (info.isShareText()) {
					weixinLogic.sendTextMsg(content, false);
				} else {
					weixinLogic.sendWebPage(info.getUrl(), title, content, file.getAbsolutePath(), false);
				}
			} else if (targetAppInfo.getTargetAppDefine().id == BuShareAppDefine.WECHAT_CMTS.id) {
				if (info.isShareText()) {
					weixinLogic.sendTextMsg(content, true);
				} else {
					weixinLogic.sendWebPage(info.getUrl(), title, content, file.getAbsolutePath(), true);
				}
			} else if (targetAppInfo.getTargetAppDefine().id == BuShareAppDefine.YIXIN.id) {
				if (info.isShareText()) {
					yixinLogic.sendTextMsg(content, false);
				} else {
					yixinLogic.sendWebPage(info.getUrl(), title, content, file.getAbsolutePath(), false);
				}
			} else if (targetAppInfo.getTargetAppDefine().id == BuShareAppDefine.YIXIN_CMTS.id) {
				if (info.isShareText()) {
					yixinLogic.sendTextMsg(content, true);
				} else {
					yixinLogic.sendWebPage(info.getUrl(), title, content, file.getAbsolutePath(), true);
				}
			} else {
				shareSDK_share(targetAppInfo, title, content, "", file.getPath(), info.getUrl());
			}
		}

	}

}
