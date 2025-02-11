package com.agora.entfulldemo.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.agora.entfulldemo.base.BaseDialog;
import com.agora.entfulldemo.databinding.DialogUserAgreementBinding;
import com.agora.entfulldemo.utils.KTVUtil;

public class UserAgreementDialog extends BaseDialog<DialogUserAgreementBinding> {
    public UserAgreementDialog(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected DialogUserAgreementBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogUserAgreementBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        getBinding().btnDisagree.setOnClickListener(view -> {
            getOnButtonClickListener().onLeftButtonClick();
        });
        getBinding().btnAgree.setOnClickListener(view -> {
            getOnButtonClickListener().onRightButtonClick();
        });
    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                KTVUtil.dp2px(280),
                KTVUtil.dp2px(350)
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }
}
