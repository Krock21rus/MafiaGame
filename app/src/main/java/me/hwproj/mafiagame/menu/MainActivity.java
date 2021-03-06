package me.hwproj.mafiagame.menu;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import me.hwproj.mafiagame.gameinterface.GameActivity;
import me.hwproj.mafiagame.R;
import me.hwproj.mafiagame.networking.NetworkData;
import me.hwproj.mafiagame.persistence.AppDatabaseInteractor;

import static me.hwproj.mafiagame.networking.NetworkData.*;

/**
 * Main menu activity.
 * User signs in google play games here.
 */
public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MafiaGame";
    private static AppDatabaseInteractor databaseInteractor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseInteractor = new AppDatabaseInteractor(getApplicationContext());

        Button createGame = findViewById(R.id.createGame);
        createGame.setOnClickListener(v -> {
            if (NetworkData.getGoogleSignInAccount() != null) {
                Intent intent = new Intent(this, GameActivity.class);
                startActivity(intent);
            } else {
                new AlertDialog.Builder(this).setMessage("Should be signed in at first")
                        .setNeutralButton(android.R.string.ok, null).show();
            }
        });

        Button signIn = findViewById(R.id.signIn);
        signIn.setOnClickListener(v -> startSignInIntent(
                new GoogleSignInOptions
                        .Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                        .requestEmail().build(),
                RC_GAMES_SIGN_IN));

        Button signOut = findViewById(R.id.signOut);
        signOut.setOnClickListener(v -> signOut());

        Button openSettings = findViewById(R.id.open_settings);
        openSettings.setOnClickListener(v ->  {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        TextView hello = findViewById(R.id.hello_text);
        hello.setText(String.format(getString(R.string.hello_with_name), getDatabaseInteractor().loadName()));

        signInSilently(new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                        .requestEmail().build(),
                RC_GAMES_SIGN_IN);


    }

    private void signInSilently(GoogleSignInOptions signInOptions, @SuppressWarnings("SameParameterValue") int requestCode) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (GoogleSignIn.hasPermissions(account, signInOptions.getScopeArray())) {
            // Already signed in.
            // The signed in account is stored in the 'account' variable.
            setGoogleSignInAccount(account);
        } else {
            // Haven't been signed-in before. Try the silent sign-in first.
            GoogleSignInClient signInClient = GoogleSignIn.getClient(this, signInOptions);
            signInClient
                    .silentSignIn()
                    .addOnCompleteListener(
                            this,
                            task -> {
                                if (task.isSuccessful()) {
                                    // The signed in account is stored in the task's result.
                                    setGoogleSignInAccount(task.getResult());
                                } else {
                                    // Player will need to sign-in explicitly using via UI.
                                    // See [sign-in best practices](http://developers.google.com/games/services/checklist) for guidance on how and when to implement Interactive Sign-in,
                                    // and [Performing Interactive Sign-in](http://developers.google.com/games/services/android/signin#performing_interactive_sign-in) for details on how to implement
                                    // Interactive Sign-in.
                                    // signing in
                                    startSignInIntent(signInOptions, requestCode);
                                }
                            });
        }
    }

    private void startSignInIntent(GoogleSignInOptions signInOptions, int requestCode) {
        GoogleSignInClient signInClient = GoogleSignIn.getClient(this,
                signInOptions);
        Intent intent = signInClient.getSignInIntent();
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GAMES_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // The signed in account is stored in the result.
                Log.d("MafiaGame", "SigningIn SUCCESS");

                setGoogleSignInAccount(result.getSignInAccount());
            } else {
                String message = result.getStatus().toString();
                if (message.isEmpty()) {
                    message = getString(R.string.signin_other_error);
                }
                new AlertDialog.Builder(this).setMessage(message)
                        .setNeutralButton(android.R.string.ok, null).show();
            }
        }
    }

    private void signOut() {
        GoogleSignInClient signInClient = GoogleSignIn.getClient(this,
                GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
        setGoogleSignInAccount(null);
        signInClient.signOut().addOnCompleteListener(this,
                task -> {
                    // at this point, the user is signed out.
                    new AlertDialog.Builder(MainActivity.this).setMessage("You has signed out")
                            .setNeutralButton(android.R.string.ok, null).show();
                });
    }

    private static AppDatabaseInteractor getDatabaseInteractor() {
        return databaseInteractor;
    }
}
