package com.example.greenstep;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

public class UploadHowTo extends Fragment {
    private EditText inputTitle,inputSourceRef;
    private ImageButton uploadImage;
    private Button submitBtn;
    private ProgressBar progressBar;
    private Uri imageUri;
    final private StorageReference storageReference = FirebaseStorage.getInstance().getReference();
    FirebaseFirestore firestoreDB = FirebaseFirestore.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @NonNull Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.edu_howto_upload, container, false);
        return rootView;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        inputTitle = view.findViewById(R.id.editTitle);
        inputSourceRef = view.findViewById(R.id.editSourceRef);
        uploadImage = view.findViewById(R.id.uploadImage);
        submitBtn = view.findViewById(R.id.btnComplete);
        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        // In the gallery page
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null && data.getData() != null) {
                                imageUri = data.getData();
                                uploadImage.setImageURI(imageUri);
                            }
                        } else {
                            Toast.makeText(requireContext(), "No image selected.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPicker = new Intent();
                photoPicker.setAction(Intent.ACTION_GET_CONTENT);
                photoPicker.setType("image/*");
                activityResultLauncher.launch(photoPicker);
            }
        });


        submitBtn.setOnClickListener(new View.OnClickListener() {
            boolean title, srcRef, img = false;
            @Override
            public void onClick(View v) {
                String titleText = inputTitle.getText().toString().trim();
                String srcRefText= inputSourceRef.getText().toString().trim();
                if(!titleText.isEmpty()){
                    title = true;
                } else{
                    Toast.makeText(requireContext(), "Please enter title.", Toast.LENGTH_SHORT).show();
                }
                if(!srcRefText.isEmpty()){
                    srcRef = true;
                } else{
                    Toast.makeText(requireContext(), "Please enter source/ reference link.", Toast.LENGTH_SHORT).show();
                }
                if (imageUri != null) {
                    img = true;

                } else {
                    Toast.makeText(requireContext(), "Please select image.", Toast.LENGTH_SHORT).show();
                }
                if(title && srcRef && img){
                    uploadToFirebase(imageUri);
//                    uploadToFirebase(imageUri, new UploadCallback() {
//                        @Override
//                        public void onSuccess() {
//                            Navigation.findNavController(view).popBackStack();
//                        }
//
//                        @Override
//                        public void onFailure() {
//                            // Handle failure if needed
//                        }
//                    });
                }
            }
        });

    }

//    public interface UploadCallback {
//        void onSuccess();
//        void onFailure();
//    }

    // Upload image to Storage AND upload details to Firestore DB
    private void uploadToFirebase(Uri uri) {
        StorageReference imageReference = storageReference.child("eduHowToPictures").child(String.valueOf(System.currentTimeMillis()));
        imageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                String sourceRef = inputSourceRef.getText().toString();
                String title = inputTitle.getText().toString();
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    // Create a new document in "Edu Content Posts" collection
                    Map<String, Object> data = new HashMap<>();
                    data.put("Title", title);
                    data.put("Source/ Reference",sourceRef);
                    data.put("ImageUrl", imageUrl);
                    firestoreDB.collection("Edu Content Posts")
                            .add(data)
                            .addOnSuccessListener(documentReference -> {
                                progressBar.setVisibility(View.VISIBLE);
                                Toast.makeText(requireContext(), "Uploaded", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(requireContext(), MainActivity.class);
                                startActivity(intent);
                                // Navigate back to the previous activity
//                                callback.onSuccess();
//                                requireActivity().onBackPressed();
//                                Navigation.findNavController(view).popBackStack();
//                                finish();
                            }).addOnFailureListener(e -> {
                                progressBar.setVisibility(View.INVISIBLE);
                                Toast.makeText(requireContext(), "Failed to upload", Toast.LENGTH_SHORT).show();
                            });
                }).addOnFailureListener(exception -> {
                    Log.e("FirebaseStorage","Error getting download URL",exception);
                });

            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

//    private String getFileExtension(Uri fileUri){
//        ContentResolver contentResolver = getContentResolver();
//        MimeTypeMap mime = MimeTypeMap.getSingleton();
//        return mime.getExtensionFromMimeType(contentResolver.getType(fileUri));
//    }

}

