package com.app.garbagedetector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.io.Serializable;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

  Button btnAdmin, btnUser;
  TextView forgotPass;

  public FirebaseAuth mauth;
  FirebaseDatabase firebaseDatabase;
  DatabaseReference admins;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    mauth = FirebaseAuth.getInstance();
    firebaseDatabase = FirebaseDatabase.getInstance();
    admins = firebaseDatabase.getReference("admins");

    forgotPass = findViewById(R.id.forgotPass);
    btnAdmin = findViewById(R.id.btnAdmin);
    btnUser = findViewById(R.id.btnUser);

    forgotPass.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showForgotPasswordDialog();
      }
    });

    btnAdmin.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showLoginDialogForAdmin();
      }
    });

    btnUser.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startActivity(new Intent(MainActivity.this, user_home.class));
      }
    });
  }

  private void showForgotPasswordDialog() {
    final android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(this);
    LayoutInflater inflater = LayoutInflater.from(this);
    View login_layout = inflater.inflate(R.layout.forgot_password, null);

    final MaterialEditText etEmailForgot = login_layout.findViewById(R.id.etEmailForgot);

    dialog.setView(login_layout);

    dialog.setPositiveButton("PROCEED", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {

        //check validation of user
        if (TextUtils.isEmpty(etEmailForgot.getText().toString())) {
          Snackbar.make(findViewById(R.id.mainActivity), "Please enter email address", Snackbar.LENGTH_LONG).show();
          return;
        }

        dialogInterface.dismiss();

        final AlertDialog waitingDialog = new SpotsDialog(MainActivity.this);
        waitingDialog.show();

        FirebaseAuth.getInstance().sendPasswordResetEmail(etEmailForgot.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                  @Override
                  public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                      Snackbar.make(findViewById(R.id.mainActivity), "Password reset email sent", Snackbar.LENGTH_LONG).show();
                    }
                  }
                })
                .addOnFailureListener(new OnFailureListener() {
                  @Override
                  public void onFailure(@NonNull Exception e) {
                    Snackbar.make(findViewById(R.id.mainActivity), e.getMessage(), Snackbar.LENGTH_LONG).show();
                  }
                });

        waitingDialog.dismiss();
      }
    })

            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
              }
            });
    dialog.show();
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (mauth.getCurrentUser() == null) {
      return;
    } else {
      startActivity(new Intent(this, admin_home.class));
      finish();
    }
  }

  private void showLoginDialogForAdmin() {
    final android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(this);
    dialog.setTitle("SIGN IN ");

    LayoutInflater inflater = LayoutInflater.from(this);
    View login_layout = inflater.inflate(R.layout.user_login, null);

    final MaterialEditText etEmailLogin = login_layout.findViewById(R.id.etEmailLogin);
    final MaterialEditText etPasswordLogin = login_layout.findViewById(R.id.etPasswordLogin);

    dialog.setView(login_layout);

    dialog.setPositiveButton("SIGN IN", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {

        //check validation of user
        if (TextUtils.isEmpty(etEmailLogin.getText().toString())) {
          Snackbar.make(findViewById(R.id.mainActivity), "Please enter email address", Snackbar.LENGTH_LONG).show();
          return;
        }

        if (TextUtils.isEmpty(etPasswordLogin.getText().toString())) {
          Snackbar.make(findViewById(R.id.mainActivity), "Please enter password", Snackbar.LENGTH_LONG).show();
          return;
        }

        dialogInterface.dismiss();

        final AlertDialog waitingDialog = new SpotsDialog(MainActivity.this);
        waitingDialog.show();


        mauth.signInWithEmailAndPassword(etEmailLogin.getText().toString(), etPasswordLogin.getText().toString())
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                  @Override
                  public void onSuccess(AuthResult authResult) {
                    waitingDialog.dismiss();
                    Intent intent=new Intent(MainActivity.this, admin_home.class);
                    startActivity(intent);
                    MainActivity.this.finish();
                  }
                })
                .addOnFailureListener(new OnFailureListener() {
                  @Override
                  public void onFailure(@NonNull Exception e) {
                    waitingDialog.dismiss();
                    Snackbar.make(findViewById(R.id.mainActivity), "FAILED " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    findViewById(R.id.mainActivity).setEnabled(true);
                  }
                });
      }
    })

            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
              }
            });
    dialog.show();
  }
}