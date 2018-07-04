package com.cloudkura.light;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;

public class ActivityCloudAuth extends AppCompatActivity {

    static final String TAG = "ActivityCloudAuth";
    static final int RESULT_SIGN_IN = 9001;

    FirebaseAuth mAuth;
    GoogleSignInClient mGoogleSignInClient;
    com.google.android.gms.common.SignInButton mSignInGoogle;

    @VisibleForTesting
    public ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_auth);

        // タイトル変更
        this.setTitle(R.string.webauth_title);

        // メニューに戻るボタンを表示
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        /*
        // サイトへのリンク作成
        TextView txtMoveToAboutUs = (TextView) findViewById(R.id.activity_auth_about_us);
        txtMoveToAboutUs.setMovementMethod(LinkMovementMethod.getInstance());
        txtMoveToAboutUs.setText(createUrlString(CKUtil.getMyString(R.string.about_us), CKUtil.getMyString(R.string.web_cloudkura_about)));
        */

        // ボタンイベント
        // Google認証
        mSignInGoogle = (com.google.android.gms.common.SignInButton) this.findViewById(R.id.activity_auth_google_sign_in_button);
        mSignInGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // ネットワーク接続確認
                if (CKUtil.isConnectNetwork()) {
                    signInByGoogle();
                } else {
                    CKUtil.showLongToast(CKUtil.getMyString(R.string.message_not_connect_network));
                }
            }
        });

        mAuth = FirebaseAuth.getInstance();
    }

    // メニューの戻るボタンの動作
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        hideProgressDialog();

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RESULT_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                CKUtil.showLongToast(CKUtil.getMyString(R.string.signin_failed));
                updateUI(null);
            }
        }
    }

    // ClickableSpan処理(Webブラウザ呼出し)
    private SpannableStringBuilder createUrlString(String showString, final String urlString) {

        SpannableStringBuilder builder = new SpannableStringBuilder();

        if (showString.equals("")) {
            return builder;
        }

        builder.append(showString);
        builder.setSpan(new ClickableSpan() {
                            // クリック時の処理
                            @Override
                            public void onClick(View view) {
                                // ブラウザ起動
                                Uri uri = Uri.parse(urlString);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                            }

                            @Override
                            public void updateDrawState(TextPaint ds) {
                                super.updateDrawState(ds);
                            }
                        },
                0, showString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return builder;
    }

    // Google Login
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        showProgressDialog();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // 認証成功
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                            CKUtil.showLongToast(CKUtil.getMyString(R.string.signin_success));
                            // 認証成功なら画面を閉じる
                            setResult(RESULT_OK);
                            finish();

                        } else {
                            // 認証失敗時
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            CKUtil.showLongToast(CKUtil.getMyString(R.string.signin_failed));
                            updateUI(null);
                        }

                        // 進捗ダイアログを閉じる
                        hideProgressDialog();
                    }
                });
    }

    // Google認証
    private void signInByGoogle() {
        showProgressDialog();

        // Push通知の購読開始
        FirebaseMessaging.getInstance().subscribeToTopic("mytopic");

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RESULT_SIGN_IN);
    }

    // 認証終了後
    private void updateUI(FirebaseUser user) {
        hideProgressDialog();
        // ユーザー情報を保存
        CKUtil.setUserInfo(user);
        if (user != null) {
            // 画面を閉じる
            finish();
        }
    }

    // 認証時のプログレス画面を表示
    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.in_progress));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    // 認証時のプログレス画面を隠す
    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
    }

}
