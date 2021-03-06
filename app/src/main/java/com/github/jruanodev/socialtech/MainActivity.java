package com.github.jruanodev.socialtech;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jruanodev.socialtech.dao.Business;
import com.github.jruanodev.socialtech.dao.Contact;
import com.github.jruanodev.socialtech.dao.DatabaseManager;
import com.github.jruanodev.socialtech.dao.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CreateUserFragment.CloseListener,
    DatabaseManager.onTaskCompleteListener {

    public static MainActivity _instance;

    @BindView(R.id.title) TextView title;
    Typeface typeface;

    private FirebaseAuth.AuthStateListener authStateListener;

    @BindView(R.id.inputUsuario) EditText inputUsuario;
    @BindView(R.id.inputPassword) EditText inputPassword;
    @BindView(R.id.btnLogin) Button btnLogin;
    @BindView(R.id.tSignUp) TextView btnCreate;
    @BindView(R.id.mainContent) View view;

    FirebaseAuth mAuth;
    List<Contact> contactList = new ArrayList<>();
    List<Business> businessList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        _instance = this;

        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(this);
        btnCreate.setOnClickListener(this);

        if(mAuth.getCurrentUser() != null) {
            Log.v("AUTENTIFICACION", "CONECTADO");
            Log.v("AUTENTIFICACIÓN", "UID " + mAuth.getCurrentUser().getUid());
        } else {
            Log.v("AUTENTIFICACION", "DESCONECTADO");
        }

        authStateListener = new FirebaseAuth.AuthStateListener() {

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                Log.v("CAMBIO DE USUARIO", "" + firebaseAuth.getCurrentUser().getEmail());
            }
        };

        AssetManager am = getApplicationContext().getAssets();
        typeface = Typeface.createFromAsset(getAssets(), "fonts/BreeSerif-Regular.ttf");

        title.setTypeface(typeface);

        inputUsuario.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                inputUsuario.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputUsuario.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                User.checkValidEmail(inputUsuario.getText().toString(), inputUsuario);
            }
        });

        inputPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                inputPassword.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                User.checkValidPassword(inputPassword.getText().toString(), inputPassword);
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLogin:
                String email = inputUsuario.getText().toString();
                String password = inputPassword.getText().toString();

                if(!email.isEmpty() || !password.isEmpty())
                    loginWithUserAndPass(email, password);
                else
                    Snackbar.make(view, "Los campos no pueden estar vacíos.", Snackbar.LENGTH_SHORT).show();

                break;

            case R.id.tSignUp:
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();

                CreateUserFragment c1 = new CreateUserFragment();
                ft.setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left);
                ft.addToBackStack("MainActivity");
                ft.replace(android.R.id.content, c1).commit();

                break;

        }
    }

    public void loginWithUserAndPass(String username, String password) {

        mAuth.signInWithEmailAndPassword(username, password).addOnCompleteListener(this,
            new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        DatabaseManager db = new DatabaseManager();
                        DatabaseManager.user = FirebaseAuth.getInstance().getCurrentUser();
                        db.getCurrentUserDatabaseKey("MainActivity");

                    } else {
                        Snackbar.make(view, "Usuario o contraseña incorrectos.", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
    }

    @Override
    public void btnCloseClick(Fragment f) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.slide_from_left, R.anim.slide_to_right);
        ft.remove(f).commit();
    }

    @Override
    public void createUserClick(User u) {
        mAuth.createUserWithEmailAndPassword(u.getEmail(), u.getPassword())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            DatabaseManager.user = FirebaseAuth.getInstance().getCurrentUser();
                            DatabaseManager dbm = new DatabaseManager();

                            dbm.createUserData();

                            Intent intent = new Intent(MainActivity.this, AuxActivity.class);
                            startActivity(intent);


                        }
                    }
                });
    }

    public void updateWithUserData() {
            Intent intent = new Intent(MainActivity.this, AuxActivity.class);
            intent.putParcelableArrayListExtra("contactList", (ArrayList<? extends Parcelable>) contactList);
            intent.putParcelableArrayListExtra("businessList", (ArrayList<? extends Parcelable>) businessList);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);

    }

    @Override
    public void isComplete(boolean check) {
        if(check) {
            DatabaseManager dc = new DatabaseManager();
            dc.getAllContacts("MainActivity");
        }

    }

    @Override
    public void isContactCreated(boolean check) {

    }

    @Override
    public void isBusinessCreated(boolean check) {

    }

    @Override
    public void isContactImportComplete(List<Contact> contactList) {
        this.contactList = contactList;
        DatabaseManager d = new DatabaseManager();
        d.getAllBusinesses("MainActivity");
    }

    @Override
    public void isBusinessImportComplete(List<Business> businessList) {
        this.businessList = businessList;
        this.updateWithUserData();
    }
}
