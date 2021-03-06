package com.uet.android.mouspad.Activity.Authorize;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.uet.android.mouspad.Activity.HomeActivity;
import com.uet.android.mouspad.R;
import com.uet.android.mouspad.Utils.ConnectionUtils;
import com.uet.android.mouspad.Utils.Constants;
import com.uet.android.mouspad.Utils.WidgetsUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.uet.android.mouspad.R.*;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText mEditUser, mEditPassword, mEditConfirmPassword;
    private Button mButtonRegister, mButtonLogin;
    private SignInButton mButtonLoginGoogle;
    private LoginButton mButtonLoginFacebook;
    private ProgressBar mProgressBar;
    private ImageView mButtonGoggleCustom, mButtonFacebookCustom;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseFirestore mFirebaseFirestore;
    private StorageReference mStorageReference;
    private DatabaseReference mDatabaseReference ;

    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;


    private String mCurrentUserId;

    public  static int RC_SIGN_IN = 123;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_register);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseFirestore = FirebaseFirestore.getInstance();
        mStorageReference = FirebaseStorage.getInstance().getReference();
        //google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        //facebook
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onError(FacebookException exception) {
                    }
                });

        MappingWidgets();
        initData();
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mFirebaseAuth.getCurrentUser();
        if(currentUser != null ){
            sendToHome();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = null;
            try {
                account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }

    }


    private void MappingWidgets(){
        mEditUser =findViewById(id.editUserRegister);
        mEditPassword = findViewById(id.editPasswordRegister);
        mEditConfirmPassword = findViewById(id.editConfirmPasswordRegister);
        mButtonRegister = findViewById(id.buttonRegister);
        mButtonLogin = findViewById(id.buttonLoginRegister);
        mProgressBar = findViewById(id.progressBarRegister);
        mButtonLoginGoogle = findViewById(id.login_google);
        mButtonLoginFacebook = findViewById(id.login_facebook);
        mButtonGoggleCustom = findViewById(id.btn_google_custom_login);
        mButtonFacebookCustom = findViewById(id.btn_facebook_custom_login);
    }

    private void initData() {

    }

    private void initView() {
        mEditUser.addTextChangedListener(new TextWatcherSignIn());
        mEditPassword.addTextChangedListener(new TextWatcherSignIn());
        mEditConfirmPassword.addTextChangedListener(new TextWatcherSignIn());

        mButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String userInfo = mEditUser.getText().toString();
                String password = mEditPassword.getText().toString();
                String passwordConfirm = mEditConfirmPassword.getText().toString();
                if(!TextUtils.isEmpty(userInfo) || !TextUtils.isEmpty(password) || !TextUtils.isEmpty(passwordConfirm)){
                    mProgressBar.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                        }
                    }, 2000);
                    if(password.equals(passwordConfirm)){
                        mFirebaseAuth.createUserWithEmailAndPassword(userInfo, password)
                                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if(task.isSuccessful()){
                                            firebaseAuthComplete();
                                        } else {
                                            mProgressBar.setVisibility(View.GONE);
                                            String e = task.getException().getMessage();
                                            Toast.makeText(RegisterActivity.this, e, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "All fields can't not be null!", Toast.LENGTH_SHORT).show();
                }
            }
        });



        mButtonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

        //google
        mButtonLoginGoogle.setSize(SignInButton.SIZE_STANDARD);
        mButtonLoginGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
        mButtonGoggleCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
                mButtonLoginGoogle.performClick();
            }
        });

        //facebook
        mButtonLoginFacebook.setReadPermissions(Arrays.asList(Constants.EMAIL));
        mButtonLoginFacebook.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                String imgUrl = "https://graph.facebook.com/"
                        + loginResult.getAccessToken().getUserId()
                        + "/picture?return_ss1_resources=1";
                Log.d("Facebook", imgUrl);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });
        mButtonFacebookCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mButtonLoginFacebook.performClick();
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            firebaseAuthComplete();
                        } else {
                        }

                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            firebaseAuthComplete();
                        } else {
                            Toast.makeText(getApplicationContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void firebaseAuthComplete(){
        mCurrentUserId = mFirebaseAuth.getCurrentUser().getUid();
        createDatabase();
        updateUiWithUser("username");

        FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
        assert firebaseUser != null;
        String userid = firebaseUser.getUid();

        mDatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(userid);

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("user_id", userid);
        hashMap.put("status", "offline");
        hashMap.put("search", "username");

        mDatabaseReference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(RegisterActivity.this, "Database", Toast.LENGTH_SHORT).show();
                }
            }
        });
        HashMap<Object, Object> map = new HashMap<>();
        map.put("comment", true);
        map.put("inbox", true);
        map.put("library", true);
        map.put("new_follwer", true);
        map.put("updates_from_following", true);
        map.put("message_board", true);
        map.put("vote", true);
        FirebaseFirestore.getInstance().collection("notification_setups").document(userid).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(RegisterActivity.this, "Notification Setup!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendToHome(){
        SharedPreferences pref = getSharedPreferences("introPrefs",MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("isIntroOpened",true);
        editor.apply();

        SharedPreferences loginPres =getSharedPreferences(Constants.LOGIN_STATE, MODE_PRIVATE);
        SharedPreferences.Editor loginEditor = loginPres.edit();
        editor.putBoolean("isLogin", true);
        loginEditor.apply();

        ConnectionUtils.isLoginValid = true;

        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        intent.putExtra(Constants.LOGIN_STATE, true);
        startActivity(intent);
        finish();
    }

    private boolean validate() {
        boolean valid = true;
        String email = mEditUser.getText().toString();
        String password = mEditPassword.getText().toString();
        String passwordConfirm = mEditConfirmPassword.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEditUser.setError("enter a valid email address");
            valid = false;
        } else {
            mEditUser.setError(null);
        }
        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            mEditPassword.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            mEditPassword.setError(null);
        }

        if (passwordConfirm.isEmpty() || passwordConfirm.length() < 4 || passwordConfirm.length() > 10) {
            mEditConfirmPassword.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            mEditConfirmPassword.setError(null);
        }

        return valid;
    }

    private void updateUiWithUser(String username) {
        String welcome = getString(R.string.welcome) + username;
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    private void createDatabase(){
        String randomName1 = UUID.randomUUID().toString();
        String randomName2 = UUID.randomUUID().toString();
        final StorageReference image_avatars = mStorageReference.child("image_avatars" ).child(randomName1);
        final StorageReference image_backgrounds = mStorageReference.child("image_backgrounds").child(randomName2);
        Uri avatarUri = WidgetsUtils.getUriToResource(getApplicationContext(), R.drawable.default_avatar);
        final Uri backgroundUri = WidgetsUtils.getUriToResource(getApplicationContext(), drawable.item_add_media_background );

        image_avatars.putFile(avatarUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                image_avatars.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(final Uri avatar) {
                        image_backgrounds.putFile(backgroundUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                                image_backgrounds.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri background) {
                                        FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
                                        String user_id = mFirebaseAuth.getCurrentUser().getUid();
                                        if(!mEditUser.getText().toString().isEmpty()){
                                            String userInfo = mEditUser.getText().toString();
                                            int endIndex = userInfo.indexOf("@");
                                            String fullname = userInfo.substring(0, endIndex);
                                            storeUserInformation(taskSnapshot, avatar, background, user_id, fullname);
                                        } else {
                                            String userInfo = firebaseUser.getEmail();
                                            int endIndex = userInfo.indexOf("@");
                                            String fullname = userInfo.substring(0, endIndex);
                                            storeUserInformation(taskSnapshot, avatar, background,user_id, userInfo);
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private String ALGOLIA_APPLICATION_API = "WFKPO6G4ZS";
    private String ALGOLIA_ADMIN_API = "92326d76f5c191740a6741af6aebfc7e";

    private void storeUserInformation(UploadTask.TaskSnapshot taskSnapshot, Uri uriAvatar, Uri uriBackground, String user_id, String username){
        String fullname = username;
        String account = fullname;
        String description = "New Description!";
        String avatar = "";
        String background = "";

        Map<String, Object> user = new HashMap<>();
        user.put("user_id", user_id);
        user.put("fullname", fullname);
        user.put("account", account);
        user.put("description", description);
        user.put("gender", "Do not want to tell");
        user.put("email", mEditUser.getText().toString());
        user.put("birthday", System.currentTimeMillis());

        if(taskSnapshot != null){
            avatar = uriAvatar.toString();
            user.put("avatar", avatar);
            background = uriBackground.toString();
            user.put("background", background);
        }

        mFirebaseFirestore.collection("users")
                .document(user_id)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        sendToHome();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String mes = e.getMessage();
                        Log.d("Exception Firestore", mes);
                    }
                });


        //save agolia
        Client client = new Client(ALGOLIA_APPLICATION_API, ALGOLIA_ADMIN_API);
        final Index indexUser = client.getIndex("firebase_user");
        try {
            JSONObject jsonObjects = new JSONObject().put("userInfo", fullname + " " + account).
                    put("userAccount", account)
                    .put("userFullname", fullname)
                    .put("instanceId", user_id)
                    .put("userAvatar", avatar)
                    .put("userBackground", background)
                    .put("userDes", description)
                    .put("userMail",mEditUser.getText().toString())
                    .put("userGender","Do not want to tell")
                    .put("userBirthday",System.currentTimeMillis());
            indexUser.addObjectAsync(jsonObjects, user_id, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private class TextWatcherSignIn implements TextWatcher{
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            validate();
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    }
}