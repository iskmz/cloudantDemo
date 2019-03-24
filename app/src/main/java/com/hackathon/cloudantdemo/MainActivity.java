package com.hackathon.cloudantdemo;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudant.sync.documentstore.AttachmentException;
import com.cloudant.sync.documentstore.ConflictException;
import com.cloudant.sync.documentstore.DocumentBody;
import com.cloudant.sync.documentstore.DocumentBodyFactory;
import com.cloudant.sync.documentstore.DocumentNotFoundException;
import com.cloudant.sync.documentstore.DocumentRevision;
import com.cloudant.sync.documentstore.DocumentStore;
import com.cloudant.sync.documentstore.DocumentStoreException;
import com.cloudant.sync.documentstore.DocumentStoreNotOpenedException;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button btnDown, btnUp,btnUpdate;
    TextView txtIn;
    EditText txtOut,txtUpdate;

    File DS_path;
    DocumentStore dsUpload, dsDownload;


// docID to check update on
    // 2bf58e51a82f4378a81b456decd7944f

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setPointers();
        setListeners();
    }

    private void setListeners() {

        btnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createLocalDataStore();
                uploadLocalToRemote();

            }
        });

        btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readDataFromRemote();
                showDataOnView();
            }
        });

    }

    private void showDataOnView() {

        DocumentRevision retrieved = null;
        try {
            retrieved = dsDownload.database().read("899eb45c581477cffc66cce801025439"); // document id
        } catch (DocumentNotFoundException e) {
            e.printStackTrace();
        } catch (DocumentStoreException e) {
            e.printStackTrace();
        }

        if(retrieved!=null) {
            DocumentBody body = retrieved.getBody();
            HashMap<String, Object> map = (HashMap<String, Object>) body.asMap();

            List<HashMap<String,Object>> mapObjectsList =  (List<HashMap<String, Object>>) map.get("data");

            StringBuilder tmpStr=new StringBuilder(); // to collect output

            for(HashMap<String,Object> mapObject : mapObjectsList) {
                String tmpPath = getFilePath(mapObject);
                tmpStr.append(tmpPath
                        .substring(tmpPath.lastIndexOf("/")+1, tmpPath.lastIndexOf(".mp4"))
                        .replace("_"," "))
                        .append("\n\n");
            }

            txtIn.setText(tmpStr);
        }
        else{
            msgFailed("failed to retrieve data!");
        }

    }

    private String getFilePath(HashMap<String, Object> mapObject) {
        return (String)mapObject.get("filePath");
    }

    private void msgFailed(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void readDataFromRemote() {


        URI uri = null;
        try {
            uri = new URI(CloudantDefaults.URL + "/" + CloudantDefaults.DB_NAME);
            dsDownload = DocumentStore.getInstance(new File(DS_path,CloudantDefaults.DB_NAME));
        }
        catch (URISyntaxException use){
            use.printStackTrace();
        }
        catch (DocumentStoreNotOpenedException dsnoe){
            dsnoe.printStackTrace();
        }

        if(uri == null || dsDownload == null)
        {
            msgFailed("ERROR!");
            return;
        }

        Replicator pullReplicator = ReplicatorBuilder.pull().from(uri).to(dsDownload).build();
        pullReplicator.start();

    }

    private void createLocalDataStore() {

        try{
            dsUpload = DocumentStore.getInstance(new File(DS_path,CloudantDefaults.DB_NAME));

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("new DATA", txtOut.getText().toString().isEmpty()?"N/A":
                    txtOut.getText().toString());

            DocumentRevision revision = new DocumentRevision();
            revision.setBody(DocumentBodyFactory.create(body));
            
            DocumentRevision saved = dsUpload.database().create(revision);
        }
        catch (DocumentStoreException dse)
        {
            System.err.println("Problem opening or accessing DocumentStore: "+dse);
        } catch (AttachmentException e) {
            e.printStackTrace();
        } catch (ConflictException e) {
            e.printStackTrace();
        }

    }

    private void uploadLocalToRemote() {

        URI uri = null;
        try {
            uri = new URI(CloudantDefaults.URL + "/" + CloudantDefaults.DB_NAME);
            dsUpload = DocumentStore.getInstance(new File(DS_path,CloudantDefaults.DB_NAME));
        }
        catch (URISyntaxException use){
            use.printStackTrace();
        }
        catch (DocumentStoreNotOpenedException dsnoe){
            dsnoe.printStackTrace();
        }

        // Replicate from the local to remote database
        Replicator replicator = ReplicatorBuilder.push().from(dsUpload).to(uri).build();

        // Fire-and-forget (there are easy ways to monitor the state too)
        replicator.start();
    }

    private void setPointers() {

        btnDown = findViewById(R.id.btnDataIn);
        btnUp = findViewById(R.id.btnDataOut);
        btnUpdate = findViewById(R.id.btnDataUpdate);

        txtIn = findViewById(R.id.txtDataIn);
        txtIn.setMovementMethod(new ScrollingMovementMethod());
        txtOut = findViewById(R.id.txtDataOut);
        txtOut.setMovementMethod(new ScrollingMovementMethod());
        txtUpdate = findViewById(R.id.txtDataUpdate);
        txtUpdate.setMovementMethod(new ScrollingMovementMethod());

        DS_path = getApplicationContext().getDir(CloudantDefaults.LOCAL_DS_PATH_STRING
                , Context.MODE_PRIVATE);
    }
}