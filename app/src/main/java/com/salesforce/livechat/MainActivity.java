package com.salesforce.livechat;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.salesforce.livechat.databinding.ActivityMainBinding;
import com.salesforce.android.chat.core.AgentAvailabilityClient;
import com.salesforce.android.chat.core.ChatConfiguration;
import com.salesforce.android.chat.core.ChatCore;
import com.salesforce.android.chat.core.model.AvailabilityState;
import com.salesforce.android.chat.core.model.ChatEntity;
import com.salesforce.android.chat.core.model.ChatEntityField;
import com.salesforce.android.chat.core.model.ChatUserData;
import com.salesforce.android.chat.ui.ChatUI;
import com.salesforce.android.chat.ui.ChatUIClient;
import com.salesforce.android.chat.ui.ChatUIConfiguration;
import com.salesforce.android.chat.ui.model.PreChatTextInputField;
import com.salesforce.android.service.common.utilities.control.Async;

import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration appBarConfiguration;

    private List<ChatUserData> chatUserData;
    private List<ChatEntity> chatEntities;

    private static final String ORG_ID = "Your organization Id";
    private static final String BUTTON_ID = "Your button Id";
    private static final String DEPLOYMENT_ID = "Your deployment Id";
    private static final String LIVE_AGENT_POD = "Your live agent pod";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.salesforce.livechat.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchChat();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /**
     * Launches Chat.
     */
    private void launchChat() {
        initializePreChatUserData();

        // Build a configuration object
        ChatConfiguration chatConfiguration =
                new ChatConfiguration.Builder(ORG_ID, BUTTON_ID, DEPLOYMENT_ID, LIVE_AGENT_POD)
                        .chatUserData(chatUserData)
                        .chatEntities(chatEntities)
                        .build();

        // Create an agent availability client
        AgentAvailabilityClient client = ChatCore.configureAgentAvailability(chatConfiguration, false);

        // Check agent availability
        client.check().onResult(new Async.ResultHandler<AvailabilityState>() {
            @Override
            public void handleResult(Async<?> async, @NonNull AvailabilityState availabilityState) {
                switch (availabilityState.getStatus()) {
                    case AgentsAvailable: {
                        ChatUI.configure(ChatUIConfiguration.create(chatConfiguration))
                                .createClient(getApplicationContext())
                                .onResult(new Async.ResultHandler<ChatUIClient>() {
                                    @Override public void handleResult (Async<?> operation,
                                                                        @NonNull ChatUIClient chatUIClient) {
                                        chatUIClient.startChatSession(MainActivity.this);
                                    }
                                });

                        break;
                    }
                    case NoAgentsAvailable: {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("No available agents")
                                .setMessage("There are no active agents at the moment. Please contact us later.")
                                .setNegativeButton(android.R.string.no, null)
                                .show();
                        break;
                    }
                    case Unknown: {
                        break;
                    }
                }
            }
        });
    }

    /**
     * Configures pre-chat fields
     */
    private void initializePreChatUserData() {
        chatUserData = new ArrayList<>();

        PreChatTextInputField firstName = new PreChatTextInputField.Builder()
                .required(true)
                .build("Please enter your first name", "First Name");
        PreChatTextInputField lastName = new PreChatTextInputField.Builder()
                .required(true)
                .build("Please enter your last name", "Last Name");

        PreChatTextInputField email = new PreChatTextInputField.Builder()
                .required(true)
                .inputType(EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .mapToChatTranscriptFieldName("Email__c")
                .build("Please enter your email", "Email Address");

        ChatUserData subject = new ChatUserData(
                "Hidden Subject Field",
                "Chat Conversation",
                false
        );
        ChatUserData origin = new ChatUserData(
                "Hidden Origin Field",
                "011",
                false
        );

        chatUserData.add(firstName);
        chatUserData.add(lastName);
        chatUserData.add(email);
        chatUserData.add(subject);
        chatUserData.add(origin);

        chatEntities = new ArrayList<>();

        ChatEntity caseEntity = new ChatEntity.Builder()
                .showOnCreate(true)
                .linkToTranscriptField("Case")
                .addChatEntityField(new ChatEntityField.Builder()
                        .doFind(false)
                        .isExactMatch(false)
                        .doCreate(true)
                        .build("Subject", subject))
                .addChatEntityField(new ChatEntityField.Builder()
                        .doFind(false)
                        .isExactMatch(false)
                        .doCreate(true)
                        .build("Origin", origin))
                .build("Case");

        ChatEntity contactEntity = new ChatEntity.Builder()
                .showOnCreate(true)
                .linkToTranscriptField("Contact")
                .linkToAnotherSalesforceObject(caseEntity, "ContactId")
                .addChatEntityField(new ChatEntityField.Builder()
                        .doFind(true)
                        .isExactMatch(true)
                        .doCreate(false)
                        .build("FirstName", firstName))
                .addChatEntityField(new ChatEntityField.Builder()
                        .doFind(true)
                        .isExactMatch(true)
                        .doCreate(false)
                        .build("LastName", lastName))
                .addChatEntityField(new ChatEntityField.Builder()
                        .doFind(true)
                        .isExactMatch(true)
                        .doCreate(false)
                        .build("Email", email))
                .build("Contact");

        ChatEntity accountEntity = new ChatEntity.Builder()
                .showOnCreate(true)
                .linkToTranscriptField("Account")
                .addChatEntityField(new ChatEntityField.Builder()
                        .doFind(true)
                        .isExactMatch(true)
                        .doCreate(false)
                        .build("FirstName", firstName))
                .addChatEntityField(new ChatEntityField.Builder()
                        .doFind(true)
                        .isExactMatch(true)
                        .doCreate(false)
                        .build("LastName", lastName))
                .addChatEntityField(new ChatEntityField.Builder()
                        .doFind(true)
                        .isExactMatch(true)
                        .doCreate(false)
                        .build("PersonEmail", email))
                .build("Account");

        chatEntities.add(caseEntity);
        chatEntities.add(contactEntity);
        chatEntities.add(accountEntity);
    }
}