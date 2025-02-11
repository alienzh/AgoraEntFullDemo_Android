package com.agora.entfulldemo.models.room.live;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.agora.entfulldemo.R;
import com.agora.entfulldemo.base.BaseRecyclerViewAdapter;
import com.agora.entfulldemo.base.BaseViewBindingActivity;
import com.agora.entfulldemo.base.OnItemClickListener;
import com.agora.entfulldemo.bean.MemberMusicModel;
import com.agora.entfulldemo.bean.room.RTMMessageBean;
import com.agora.entfulldemo.common.KtvConstant;
import com.agora.entfulldemo.databinding.ActivityRoomLivingBinding;
import com.agora.entfulldemo.databinding.KtvItemRoomSpeakerBinding;
import com.agora.entfulldemo.dialog.CommonDialog;
import com.agora.entfulldemo.dialog.MoreDialog;
import com.agora.entfulldemo.dialog.MusicSettingDialog;
import com.agora.entfulldemo.dialog.RoomChooseSongDialog;
import com.agora.entfulldemo.dialog.UserLeaveSeatMenuDialog;
import com.agora.entfulldemo.listener.OnButtonClickListener;
import com.agora.entfulldemo.manager.PagePathConstant;
import com.agora.entfulldemo.manager.RTMManager;
import com.agora.entfulldemo.manager.RoomManager;
import com.agora.entfulldemo.manager.UserManager;
import com.agora.entfulldemo.models.room.live.fragment.dialog.MVFragment;
import com.agora.entfulldemo.models.room.live.holder.RoomPeopleHolder;
import com.agora.entfulldemo.utils.ThreadManager;
import com.agora.entfulldemo.utils.WifiUtils;
import com.agora.entfulldemo.widget.DividerDecoration;
import com.agora.entfulldemo.widget.LrcControlView;
import com.agora.data.model.AgoraMember;
import com.alibaba.android.arouter.facade.annotation.Route;

import java.util.Arrays;
import java.util.List;

import io.agora.lrcview.PitchView;
import io.agora.lrcview.bean.LrcData;

/**
 * 房间主页
 */
@Route(path = PagePathConstant.pageRoomLiving)
public class RoomLivingActivity extends BaseViewBindingActivity<ActivityRoomLivingBinding>

            private MoreDialog moreDialog;
    private MusicSettingDialog musicSettingDialog;
    private BaseRecyclerViewAdapter<KtvItemRoomSpeakerBinding, AgoraMember, RoomPeopleHolder> mRoomSpeakerAdapter;
    private boolean isInitList = false;
    private CommonDialog creatorExitDialog;

    private CommonDialog exitDialog;
    private UserLeaveSeatMenuDialog mUserLeaveSeatMenuDialog;
    private AgoraMember mAgoraMember = null;

    @Override
    protected ActivityRoomLivingBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityRoomLivingBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        roomLivingViewModel = new ViewModelProvider(this).get(RoomLivingViewModel.class);
        roomLivingViewModel.setLifecycleOwner(this);
        roomLivingViewModel.initData();
        mRoomSpeakerAdapter = new BaseRecyclerViewAdapter<>(Arrays.asList(new AgoraMember[8]),
                this, RoomPeopleHolder.class);
        getBinding().rvUserMember.addItemDecoration(new DividerDecoration(4, 24, 8));
        getBinding().rvUserMember.setAdapter(mRoomSpeakerAdapter);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            getBinding().rvUserMember.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
        getBinding().lrcControlView.setRole(LrcControlView.Role.Listener);
        getBinding().lrcControlView.post(() -> {
            roomLivingViewModel.joinRoom();
            roomLivingViewModel.requestRTMToken();
        });
    }

    @Override
    public void requestData() {
        getWifiStatus();
    }

    private void getWifiStatus() {
        int wifiRssi = WifiUtils.getWifiStatus();
        if (wifiRssi < 50) {
            getBinding().ivNetStatus.setImageResource(R.drawable.bg_round_green);
            getBinding().tvNetStatus.setText(R.string.net_status_good);
        } else {
            getBinding().ivNetStatus.setImageResource(R.drawable.bg_round_yellow);
            getBinding().tvNetStatus.setText(R.string.net_status_low);
        }
    }

    @Override
    public void onItemClick(@NonNull AgoraMember agoraMember, View view, int position, long viewType) {
        mAgoraMember = agoraMember;
        if (!TextUtils.isEmpty(agoraMember.userNo)) {
            if (RoomManager.mMine.isMaster) {
                if (!mRoomSpeakerAdapter.dataList.get(position).userNo.equals(RoomManager.mMine.userNo)) {
                    showUserLeaveSeatMenuDialog();
                }
            } else if (mRoomSpeakerAdapter.dataList.get(position).userNo.equals(RoomManager.mMine.userNo)) {
                showUserLeaveSeatMenuDialog();
            }
        }
    }

    /**
     * 下麦提示
     */
    private void showUserLeaveSeatMenuDialog() {
        if (mUserLeaveSeatMenuDialog == null) {
            mUserLeaveSeatMenuDialog = new UserLeaveSeatMenuDialog(this);
            mUserLeaveSeatMenuDialog.setOnButtonClickListener(new OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                    roomLivingViewModel.leaveSeat(mAgoraMember);
                }
            });
        }
        mUserLeaveSeatMenuDialog.setAgoraMember(mAgoraMember);
        mUserLeaveSeatMenuDialog.show();
    }

    @Override
    public void onItemClick(View view, int position, long viewType) {
        if (position == -1) return;
        // 击坐位 上麦克
        AgoraMember agoraMember = mRoomSpeakerAdapter.dataList.get(position);
        if (agoraMember == null) {
            if (RoomManager.getInstance().getMine().role == AgoraMember.Role.Listener) {
                roomLivingViewModel.haveSeat(position);
            }
        }
    }

    @Override
    public void initListener() {
        getBinding().ivExit.setOnClickListener(view -> {
            showExitDialog();
        });
        getBinding().cbMic.setOnCheckedChangeListener((compoundButton, b) -> {
            roomLivingViewModel.toggleMic(b ? 0 : 1);
            mRoomSpeakerAdapter.getItemData(RoomManager.mMine.onSeat).isSelfMuted = b ? 0 : 1;
            mRoomSpeakerAdapter.notifyItemChanged(RoomManager.mMine.onSeat);
        });
        getBinding().iBtnChorus.setOnClickListener(this::showChooseSongDialog);
        getBinding().iBtnChooseSong.setOnClickListener(this::showChooseSongDialog);
        getBinding().btnMenu.setOnClickListener(this::showMoreDialog);
        getBinding().btnOK.setOnClickListener(view -> {
            getBinding().groupResult.setVisibility(View.GONE);
        });
        roomLivingViewModel.setOnLrcActionListener(getBinding().lrcControlView);
        getBinding().lrcControlView.setPitchViewOnActionListener(new PitchView.OnActionListener() {
            @Override
            public void onOriginalPitch(float pitch, int totalCount) {
            }

            @Override
            public void onScore(double score, double cumulativeScore, double totalScore) {
                Log.d("cwtsw", "得分 score = " + score + " cumulativeScore = " + cumulativeScore + " totalScore = "
                        + totalScore);
                getBinding().lrcControlView.updateScore(score);
            }
        });
        getBinding().cbVideo.setOnCheckedChangeListener((compoundButton, b) -> toggleSelfVideo(b));
        roomLivingViewModel.setISingleCallback((type, o) -> {
            ThreadManager.getMainHandler().post(() -> {
                if (isFinishing() || getBinding() == null) {
                    return;
                }
                hideLoadingView();
                if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_LOCAL_PITCH) {
                    getBinding().lrcControlView.getPitchView().updateLocalPitch((float) o);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_BG_CHANGE) {
                    // 修改背景
                    setPlayerBgFromMsg(Integer.parseInt((String) o));
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_MEMBER_COUNT_UPDATE) {
                    getBinding().tvRoomMCount.setText(
                            getString(R.string.room_count, String.valueOf(RTMManager.getInstance().getMemberCount())));
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_JOIN_SUCCESS) {
                    if (!TextUtils.isEmpty(roomLivingViewModel.agoraRoom.bgOption)) {
                        setPlayerBgFromMsg(Integer.parseInt(roomLivingViewModel.agoraRoom.bgOption));
                    } else {
                        setPlayerBgFromMsg(0);
                    }
                    getBinding().tvRoomName.setText(roomLivingViewModel.agoraRoom.name);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_EXIT) {
                    finish();
                } else if (type == KtvConstant.TYPE_CONTROL_VIEW_STATUS_ON_CREATOR_EXIT) {
                    showCreatorExitDialog();
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_

    // 修改背景   getBiding(.lcControlView.setLrcViewBackground(R.mipmap.portrait02;se if (type == KtvConsta   finis();
    se if (
    onMemberLeave((AgoraMember) o);}else if(type==KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_COTgetBindin}else if(type==KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_MEMBER_JOIN) onMemberJoin((AgoraMember) );}else if(type==KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_PLAY_COMPLETED) {if (RoomManager.getInstance().mMusicModel.userNo          .eqal(UserManager.getInstance().getUser().userNo)        || UserManager.getnst                  .eqal(RoomManager.getInstance().mMusicModel.user1Id)) {
    
            int score = (int) getBinding().lrcControlVie
                inding().tvResultScore.setText(String.valueOf(score
                score >= 90) {
                        ng().ivResultLevel.setImageResource(R.mipmap.ic_s);
            } else if (score >= 80) {
                getBinding().ivResultLevel.setImageResource(R.mipmap.i
            } else if (score >
                getBinding().ivResultLevel.setImageResource(R.mipmap.ic_b);
            } else {
                getBinding().ivResultLevel.setImageResource(R.mipmap.ic_c);
            }
                inding().groupResult.setVisibility(View.VISIBLE);
            
                ype == KtvConstant.CALLBACK_TYPE_ROOM_SEAT_CHANGE) {
            m
            f (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_S
        g
    }else if(type==KtvConstant.TYPE_CONTROL_VIEW_STATUS_ON_VID
    O
        RTMMessageBean bean = (RTMMessageBean) o;
       // 像打摄像头
    
        for (int i = 0; i < mRoomSpeakerAdapter.dataList.size();
           i (mRomS
    eakerAdapter.dataList.get(i) != null
                                && mRoomS
    e
                mRoomSpeakerAdapter.dataList.get(

                mRoomSpeakerAdapter.notifyItemChanged(i);
            }
                    
                ype == KtvConstant.TYPE_C

                geBean bean = (RTMMessageBean) o;
                  = 0;  < mRompeakerAdapter.dataList.

    
            i
         
              mRomSeakerAdapter.dataList.get(i).isSelfMuted = bean.i
    SelfMuted;
                            // mRoomSpeakerAdapter.dataList.get(i).setStreamId(bean.streamId.intValue());
                            mRoomSpeakerAdapter.notifyItemChanged(i);
                        }
                    }
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_MIC_STATUS) {
                    getBinding().cbMic.setChecked((boolean) o);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_VIDEO_STATUS_CHANGED) {
                    onVideoStatusChange((AgoraMember) o);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_MUSIC_DEL) {
                    // onMusicDelete((MemberMusicModel) o);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_MUSIC_CHANGED) {
                    onMusicChanged((MemberMusicModel) o);
                    getBinding().lrcControlView.setScoreControlView();
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_CONTROL_VIEW_PITCH_LRC_DATA) {
                    getBinding().lrcControlView.getLrcView().setLrcData((LrcData) o);
                    getBinding().lrcControlView.getPitchView().setLrcData((LrcData) o);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_CONTROL_VIEW_TOTAL_DURATION) {
                    getBinding().lrcControlView.getLrcView().setTotalDuration((Long) o);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_CONTROL_VIEW_UPDATE_TIME) {
                    getBinding().lrcControlView.getLrcView().updateTime((Long) o);
                    getBinding().lrcControlView.getPitchView().updateTime((Long) o);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_COUNT_DOWN) {
                    getBinding().lrcControlView.setCountDown((Integer) o);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_JOINED_CHORUS) {
                    getBinding().lrcControlView.onMemberJoinedChorus();
                    mRoomSpeakerAdapter.notifyDataSetChanged();
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_SHOW_MEMBER_STATUS) {
                    if (!isInitList) {
                        for (AgoraMember member : (List<AgoraMember>) o) {
                            onMemberJoin(member);
                        }
                        isInitList = true;
                    }
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_ON_SEAT) {
                    RTMMessageBean msg = (RTMMessageBean) o;
                    AgoraMember member = new AgoraMember();
                    member.onSeat = msg.onSeat;
                    member.userNo = msg.userNo;
                    member.headUrl = msg.headUrl;
                    member.name = msg.name;
                    member.id = msg.id;
                    member.isSelfMuted = 0;
                    member.isVideoMuted = 0;
                    onMemberJoin(member);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LEAVE_SEAT) {
                    RTMMessageBean msg = (RTMMessageBean) o;
                    AgoraMember member = new AgoraMember();
                    member.onSeat = msg.onSeat;
                    member.userNo = msg.userNo;
                    member.headUrl = msg.headUrl;
                    member.name = msg.name;
                    member.isSelfMuted = 0;
                    member.isVideoMuted = 0;
                    onMemberLeave(member);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_MUSICEMPTY) {
                    getBinding().lrcControlView.setRole(LrcControlView.Role.Listener);
                    getBinding().lrcControlView.onIdleStatus();
                    mRoomSpeakerAdapter.notifyDataSetChanged();
                } else if (type == KtvConstant.CALLBACK_TYPE_SHOW_MUSIC_MENU_DIALOG) {
                    showMusicSettingDialog();
               

    } else if (type == KtvConstant.CALLBACK_TYPE_SHOW_CHANGE_MUSIC_DIALOG) {
                    showChangeMusicDialog();
                } else if (type == KtvConstant.CALLBACK_TYPE_TOGGLE_MIC) {
                    getBinding().cbMic.setEnabled((Boolean) o);
                } else if (type == KtvConstant.CALLBACK_TYPE_ROOM_LIVING_ON_CONTROL_VIEW_STATUS) {
                    if ((int) o == KtvConstant.TYPE_CONTROL_VIEW_STATUS_ON_PREPARE) {
                        getBinding().lrcControlView.onPrepareStatus();
                    } else if ((int) o == KtvConstant.TYPE_CONTROL_VIEW_STATUS_ON_WAIT_CHORUS) {
                        getBinding().lrcControlView.onWaitChorusStatus();
                    } else if ((int) o == KtvConstant.TYPE_CONTROL_VIEW_STATUS_ON_PLAY_STATUS) {
                        getBinding().lrcControlView.onPlayStatus();
                    } else if ((int) o == KtvConstant.TYPE_CONTROL_VIEW_STATUS_ON_PAUSE_STATUS) {
                        getBinding().lrcControlView.onPauseStatus();
                    } else if ((int) o == KtvConstant.TYPE_CONTROL_VIEW_STATUS_ON_LRC_RESET) {
                        getBinding().lrcControlView.getLrcView().reset();
                    }
                }
            });
        });

    }

    private voi
    i   exitDialog = new CommonDialog(this);

    
        exitDialog.setDialogTitle("退出房间");
            if (RoomManager.mMine.isMaster) {
         
     
        } else {
            exitDialog.setDescText("确认要关闭房间么？");
            }
            exitDialog.setDialogBtnText("取消", "确定");
            exitDialog.setOnButtonClickListener(new OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                    roomLivingViewModel.exitRoom();
                }
            });
        }
        exitDialog.show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void onMusicChanged(@NonNull MemberMusicModel music) {
        getBinding().lrcControlView.setMusic(music);
     

            getBinding().lrcCo

        } else {
            // RoomManager.mMine.role = AgoraMember.Role.Listener;
            getBinding().lrcControlView.setRole(LrcControlView.Role.Listener);
        }
        roomLivingViewModel.onMusicStaticChanged(this, music);
        mRoomSpeakerAdapter.notifyDataSetChanged();
    }

    private void onVideoStatusChange(AgoraMember member) {
        for (int i = 0; i < mRoomSpeakerAdapter.dataList.size(); i++) {
            AgoraMember currentMember = mRoomSpeakerAdapter.dataList.get(i);
            if (currentMember != null && currentMember.userNo.equals(member.userNo)) {
                mRoomSpeakerAdapter.dataList.set(i, member);
                mRoomSpeakerAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    public void closeMenuDialog() {
        moreDialog.dismiss();
    }

    private void showChooseSongDialog(View view) {
        boolean isChorus = false;
        if (view.getId() == R.id.iBtnChorus) {
            isChorus = true;
        }
        new RoomChooseSongDialog(isChorus)
                .show(getSupportFragmentManager(), RoomChooseSongDialog.TAG);
    }

    private void showMoreDialog(View view) {
        if (moreDialog == null) {
            moreDialog = new MoreDialog(roomLivingViewModel.mSetting);
        }
        moreDialog.show(getSupportFragmentManager(), MoreDialog.TAG);
    }

    private void showMusicSettingDialog() {
        if (musicSettingDialog == null) {
            musicSettingDialog = new MusicSettingDialog(roomLivingViewModel.mSetting);
        }
        musicSettingDialog.show(getSupportFragmentManager(), MusicSettingDialog.TAG);
    }

    private CommonDialog changeMusicDialog;

    private void showChangeMusicDialog() {
        if (changeMusicDialog == null) {
            changeMusicDialog = new CommonDialog(this);
            changeMusicDialog.setDialogTitle(getString(R.string.ktv_room_change_music_title));
            changeMusicDialog.setDescText(getString(R.string.ktv_room_change_music_msg));
            changeMusicDialog.setDialogBtnText(getString(R.string.ktv_cancel), getString(R.string.ktv_confirm));
            changeMusicDialog.setOnButtonClickListener(new OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {

                }

                @Override
                public void onRightButtonClick() {
                    roomLivingViewModel.changeMusic();
                }
            });
        }
        changeMusicDialog.show();
    }

    public void setPlayerBgFromMsg(int position) {
        getBinding().lrcControlView.setLrcViewBackground(MVFragment.exampleBackgrounds.get(position));
    }

    public void setPlayerBg(int position) {
        roomLivingViewModel.setMV_BG(position);
        getBinding().lrcControlView.setLrcViewBackground(MVFragment.exampleBackgrounds.get(position));
    }

    @Override
    protected void onStart() {
        super.onStart();
        roomLivingViewModel.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        roomLivingViewModel.onStop();
    }

    @Override
    public boolean isBlackDarkStatus() {

            
    // 开启 关

    摄像头
    private void toggleSelfVideo(boolean isOpen) {
        if (isOpen) {
            RoomManager.mMine.isVideoMuted = 1;
        } else {
            RoomManager.mMine.isVideoMuted = 0;
        }
        mRoomSpeakerAdapter.getItemData(RoomManager.mMine.onSeat).isVideoMuted = RoomManager.mMine.isVideoMuted;
        mRoomSpeakerAdapter.notifyItemChanged(RoomManager.mMine.onSeat);
        roomLivingViewModel.toggleSelfVideo(RoomManager.mMine.isVideoMuted);
    }

    private void onMemberLeave(@NonNull AgoraMember member) {
        if (member.userNo.equals(RoomManager.mMine.userNo)) {
            getBinding().groupBottomView.setVisibility(View.GONE);
            RoomManager.mMine.role = AgoraMember.Role.Listener;
        }
        AgoraMember temp = mRoomSpeakerAdapter.getItemData(member.onSeat);
        if (temp != null) {
            mRoomSpeakerAdapter.dataList.set(member.onSeat, null);
            mRoomSpeakerAdapter.notifyItemChanged(member.onSeat);
        }
        RoomManager.getInstance().onMemberLeave(member);
    }

    private void onMemberJoin(@NonNull AgoraMember member) {
        if (mRoomSpeakerAdapter.getItemData(member.onSeat) == null) {
            mRoomSpeakerAdapter.dataList.set(member.onSeat, member);
            if (member.userNo.equals(RoomManager.getInstance().getMine().userNo)) {
                RoomManager.getInstance().getMine().onSeat = member.onSeat;
                if (member.isMaster) {
                    RoomManager.getInstance().getMine().isMaster = true;
                    RoomManager.getInstance().getMine().role = AgoraMember.Role.Owner;
                } else {
                    RoomManager.getInstance().getMine().role = AgoraMember.Role.Speaker;
                }
                getBinding().groupBottomView.setVisibility(View.VISIBLE);
            }
            mRoomSpeakerAdapter.notifyItemChanged(member.onSeat);
            RoomManager.getInstance().onMemberJoin(member);
        }
    }

    private void showCreatorExitDialog() {
        if (creatorExitDialog == null) {
            creatorExitDialog = new CommonDialog(this);
            creatorExitDialog.setCanceledOnTouchOutside(false);
            creatorExitDialog.setDialogTitle("房间关闭");
            creatorExitDialog.setDescText("房主退出，并关闭了房间");
            creatorExitDialog.setDialogBtnText(getString(R.string.exit), null);
            creatorExitDialog.setOnButtonClickListener(new OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                    finish();
                }

                @Override
                public void onRightButtonClick() {

                }
            });
        }
        creatorExitDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        roomLivingViewModel.release();
        RoomManager.getInstance().leaveRoom();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showExitDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
