package com.agora.entfulldemo.models.room.live.holder;

import android.content.Context;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.agora.entfulldemo.R;
import com.agora.entfulldemo.api.model.User;
import com.agora.entfulldemo.base.BaseRecyclerViewAdapter;
import com.agora.entfulldemo.bean.MemberMusicModel;
import com.agora.entfulldemo.common.CenterCropRoundCornerTransform;
import com.agora.entfulldemo.common.GlideApp;
import com.agora.entfulldemo.databinding.KtvItemRoomSpeakerBinding;
import com.agora.entfulldemo.manager.RTCManager;
import com.agora.entfulldemo.manager.RoomManager;
import com.agora.entfulldemo.manager.UserManager;
import com.agora.data.model.AgoraMember;
import com.google.android.material.card.MaterialCardView;

import io.agora.rtc2.Constants;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;

public class RoomPeopleHolder extends BaseRecyclerViewAdapter.BaseViewHolder<KtvItemRoomSpeakerBinding, AgoraMember> {
    public RoomPeopleHolder(@NonNull KtvItemRoomSpeakerBinding mBinding) {
        super(mBinding);
    }

    @Override
    public void binding(AgoraMember member, int selectedIndex) {
        mBinding.tvUserName.setText(String.valueOf(getAdapterPosition() + 1));
        mBinding.avatarItemRoomSpeaker.setImageResource(R.mipmap.ktv_ic_seat);
        mBinding.tvZC.setVisibility(View.GONE);
        mBinding.tvRoomOwner.setVisibility(View.GONE);
        mBinding.ivMute.setVisibility(View.GONE);
        if (member == null) {
            mBinding.avatarItemRoomSpeaker.setVisibility(View.VISIBLE);
            if (mBinding.superLayout.getChildAt(0) instanceof CardView) {
                mBinding.superLayout.removeViewAt(0);
            }
            return;
        }
        if (member.isMaster && getAdapterPosition() == 0) {
            mBinding.tvRoomOwner.setVisibility(View.VISIBLE);
        }
        mBinding.tvUserName.setText(member.name);
        if (member.isSelfMuted == 1) {
            mBinding.ivMute.setVisibility(View.VISIBLE);
        } else {
            mBinding.ivMute.setVisibility(View.GONE);
        }
        GlideApp.with(itemView).load(member.headUrl).error(R.mipmap.userimage)
                .transform(new CenterCropRoundCornerTransform(100)).into(mBinding.avatarItemRoomSpeaker);
        MemberMusicModel mMusicModel = RoomManager.getInstance().getMusicModel();
        if (mMusicModel != null) {
            if (mMusicModel.userNo.equals(member.userNo)) {
                mBinding.tvZC.setText("主唱");
                mBinding.tvZC.setVisibility(View.VISIBLE);
            } else if (member.userNo.equals(mMusicModel.user1Id)) {
                mBinding.tvZC.setText("合唱");
                mBinding.tvZC.setVisibility(View.VISIBLE);
            } else {
                mBinding.tvZC.setVisibility(View.GONE);
            }
        }
        showAvatarOrCameraView(member);
    }

    private void showAvatarOrCameraView(AgoraMember member) {
        Context mContext = itemView.getContext();
        User mUser = UserManager.getInstance().getUser();
        RtcEngine engine = RTCManager.getInstance().getRtcEngine();
        if (mUser != null) {
            if (member.isVideoMuted == 0) { // 未开启摄像头 《==》 移除存在的SurfaceView，显示头像
                mBinding.avatarItemRoomSpeaker.setVisibility(View.VISIBLE);
                if (mBinding.superLayout.getChildAt(0) instanceof CardView) {
                    mBinding.superLayout.removeViewAt(0);
                }
            } else { // 开启了摄像头
                mBinding.avatarItemRoomSpeaker.setVisibility(View.INVISIBLE);
                if (mBinding.superLayout.getChildAt(0) instanceof CardView) { // SurfaceView 已存在 《==》 No-OP
                    ((CardView) mBinding.superLayout.getChildAt(0)).removeAllViews();
                    mBinding.superLayout.removeViewAt(0);
                }
//                } else {
                SurfaceView surfaceView = loadRenderView(mContext);
                if (member.userNo.equals(UserManager.getInstance().getUser().userNo)) { // 是本人
                    engine.startPreview();
                    engine.setupLocalVideo(new VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, 0));
                } else {
                    int id = member.getStreamId().intValue();
                    RTCManager.getInstance().getRtcEngine().setupRemoteVideo(new VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, id));
                }
//            }
            }
        }

    }

    @NonNull
    private SurfaceView loadRenderView(@NonNull Context mContext) {
        MaterialCardView cardView = new MaterialCardView(mContext, null, R.attr.materialCardViewStyle);
        cardView.setCardElevation(0);
        cardView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> cardView.setRadius((right - left) / 2f));

        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 0);
        lp.dimensionRatio = "1:1";
        cardView.setLayoutParams(lp);

        SurfaceView surfaceView = new SurfaceView(mContext);
        surfaceView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        cardView.addView(surfaceView);

        mBinding.superLayout.addView(cardView, 0);
        return surfaceView;
    }

}