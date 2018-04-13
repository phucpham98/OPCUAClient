package com.lucazanrosso.opcuaclient;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.security.Security;
import java.util.Locale;

import org.opcfoundation.ua.application.Client;
import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.EndpointDescription;
import org.opcfoundation.ua.core.MessageSecurityMode;
import org.opcfoundation.ua.core.ReadResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.core.WriteValue;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.transport.security.SecurityPolicy;
import org.opcfoundation.ua.utils.EndpointUtil;

public class MainActivity extends AppCompatActivity {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.text_view);
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

    }

    public void connect(View view) {
        new  ConnectionAsyncTask().execute(null, null, null);
    }

    private class ConnectionAsyncTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {

            try {

                /////////////// CLIENT ///////////////
                // Create ApplicationDescription
                ApplicationDescription applicationDescription = new ApplicationDescription();
                applicationDescription.setApplicationName(new LocalizedText("AndroidClient", Locale.ENGLISH));
                applicationDescription.setApplicationUri("urn:localhost:AndroidClient");
                applicationDescription.setProductUri("urn:lucazanrosso:AndroidClient");
                applicationDescription.setApplicationType(ApplicationType.Client);

                // Create Client Application Instance Certificate
                KeyPair myClientApplicationInstanceCertificate = ExampleKeys.getCert(getApplicationContext(),applicationDescription);

                // Create Client
                Client myClient = Client.createClientApplication(myClientApplicationInstanceCertificate);
                System.out.println("Application URI " + myClient.getApplication().getApplicationUri());
                //////////////////////////////////////


                /////////// DISCOVER ENDPOINT ////////
                // Discover endpoints
                EndpointDescription[] endpoints = myClient.discoverEndpoints("opc.tcp://DESKTOP-EGB7B8G.homenet.telecomitalia.it:53530/OPCUA/SimulationServer");

                // Filter out all but opc.tcp protocol endpoints
                endpoints = EndpointUtil.selectByProtocol(endpoints, "opc.tcp");

                // Filter out all but Signed & Encrypted endpoints
                endpoints = EndpointUtil.selectByMessageSecurityMode(endpoints, MessageSecurityMode.SignAndEncrypt);

                // Filter out all but Basic128 cryption endpoints
                endpoints = EndpointUtil.selectBySecurityPolicy(endpoints, SecurityPolicy.BASIC256SHA256);

                // Sort endpoints by security level. The lowest level at the beginning, the highest at the end of the array
                endpoints = EndpointUtil.sortBySecurityLevel(endpoints);

                // Choose one endpoint.
                EndpointDescription endpoint = endpoints[endpoints.length - 1];

                System.out.println("Security Level " + endpoint.getSecurityPolicyUri());
                System.out.println("Security Mode " + endpoint.getSecurityMode());
                //////////////////////////////////////


                /////////////// SESSION //////////////
                // Create the session from the chosen endpoint
                SessionChannel mySession = myClient.createSessionChannel(endpoint);

                // Activate the session. Use mySession.activate() if you do not want to use user authentication
                mySession.activate("Luca", "lukesky");

                // Read a variable
                NodeId nodeId = new NodeId(5, "Counter1");
                ReadValueId readValueId = new ReadValueId(nodeId, Attributes.Value, null, null);
                ReadResponse res = mySession.Read(null, 500.0, TimestampsToReturn.Source, readValueId);

                // Show the result in a TextView
                final DataValue[] dataValue = res.getResults();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(dataValue[0].getValue().toString());
                    }
                });

                // Write a variable. In this case the same varieable read is set to 0
                WriteValue writeValue = new WriteValue(nodeId, Attributes.Value, null, new DataValue(new Variant(0)));
                mySession.Write(null, writeValue);

                // Close the session
                mySession.close();
                mySession.closeAsync();
                //////////////////////////////////////

            } catch (Exception e) {
                e.printStackTrace();
            }

            return "Connecting...";
        }


        @Override
        protected void onPostExecute(String result) {

        }


        @Override
        protected void onPreExecute() {

        }


        @Override
        protected void onProgressUpdate(String... text) {

        }
    }
}
