package com.hackathon.cloudantdemo;

import android.content.Context;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cloudant.sync.documentstore.AttachmentException;
import com.cloudant.sync.documentstore.ConflictException;
import com.cloudant.sync.documentstore.DocumentBodyFactory;
import com.cloudant.sync.documentstore.DocumentRevision;
import com.cloudant.sync.documentstore.DocumentStore;
import com.cloudant.sync.documentstore.DocumentStoreException;
import com.cloudant.sync.documentstore.DocumentStoreNotOpenedException;
import com.cloudant.sync.internal.mazha.BulkGetResponse;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button btnDown, btnUp;
    TextView txtIn;
    EditText txtOut;


    File DS_path;
    DocumentStore ds;



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
    }

    private void readDataFromRemote() {


    }

    private void createLocalDataStore() {

        try{
            ds = DocumentStore.getInstance(new File(DS_path,CloudantDefaults.DB_NAME));

            DocumentRevision revision = new DocumentRevision();
            Map<String, Object> body = new HashMap<String, Object>();
            body.put("new DATA", txtOut.getText().toString().isEmpty()?"N/A":
                    txtOut.getText().toString());
            revision.setBody(DocumentBodyFactory.create(body));
            DocumentRevision saved = ds.database().create(revision);

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
        DocumentStore ds = null;
        try {
            uri = new URI(CloudantDefaults.URL + "/" + CloudantDefaults.DB_NAME);
            ds = DocumentStore.getInstance(new File(DS_path,CloudantDefaults.DB_NAME));
        }
        catch (URISyntaxException use){
            use.printStackTrace();
        }
        catch (DocumentStoreNotOpenedException dsnoe){
            dsnoe.printStackTrace();
        }

        // Replicate from the local to remote database
        Replicator replicator = ReplicatorBuilder.push().from(ds).to(uri).build();

        // Fire-and-forget (there are easy ways to monitor the state too)
        replicator.start();
    }

    private void setPointers() {

        btnDown = findViewById(R.id.btnDataIn);
        btnUp = findViewById(R.id.btnDataOut);
        txtIn = findViewById(R.id.txtDataIn);
        txtOut = findViewById(R.id.txtDataOut);

        DS_path = getApplicationContext().getDir(CloudantDefaults.LOCAL_DS_PATH_STRING
                , Context.MODE_PRIVATE);



    }

    /*

    // Obtain storage path on Android
File path = getApplicationContext().getDir("documentstores", Context.MODE_PRIVATE);

try {
    // Obtain reference to DocumentStore instance, creating it if doesn't exist
    DocumentStore ds = DocumentStore.getInstance(new File(path, "my_document_store"));

    // Create a document
    DocumentRevision revision = new DocumentRevision();
    Map<String, Object> body = new HashMap<String, Object>();
    body.put("animal", "cat");
    revision.setBody(DocumentBodyFactory.create(body));
    DocumentRevision saved = ds.database().create(revision);

    // Add an attachment -- binary data like a JPEG
    UnsavedFileAttachment att1 =
            new UnsavedFileAttachment(new File("/path/to/image.jpg"), "image/jpeg");
    saved.getAttachments().put("image.jpg", att1);
    DocumentRevision updated = ds.database().update(saved);

    // Read a document
    DocumentRevision aRevision = ds.database().read(updated.getId());
} catch (DocumentStoreException dse) {
    System.err.println("Problem opening or accessing DocumentStore: "+dse);
}


     */
}
