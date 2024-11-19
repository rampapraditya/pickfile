package pramedia.pickfileandcapture;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import androidx.core.content.FileProvider;

import pramedia.pickfileandcapture.utils.FileUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private ImageView image;
    private TextView namaFile, pathFile;
    private Button btnPickImage, btnCamera, btnPickAudio, btnPickVideo, btnPickFile;
    private final FileUtils fu = new FileUtils();
    private Uri photoUri; // URI untuk file gambar
    private File photoFile; // Tambahkan ini sebagai variabel kelas

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        image.setImageURI(fileUri);

                        String filePath = fu.getFilePathFromUri(MainActivity.this, fileUri);
                        String fileName = fu.getFileNameFromUri(MainActivity.this, fileUri);

                        namaFile.setText(String.format("Nama File : %s", fileName));
                        pathFile.setText(String.format("Path File : %s", filePath));
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) Objects.requireNonNull(result.getData().getExtras()).get("data");
                    if (photo != null) {
                        image.setImageBitmap(photo);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncherWithFile = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    scanFile(photoFile.getAbsolutePath());
                    image.setImageURI(photoUri);

                    String filePath = fu.getFilePathFromUri(MainActivity.this, photoUri);
                    String fileName = fu.getFileNameFromUri(MainActivity.this, photoUri);

                    namaFile.setText(String.format("Nama File : %s", fileName));
                    pathFile.setText(String.format("Path File : %s", filePath));

                    Toast.makeText(this, "Gambar disimpan di: " + photoUri.getPath(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        image = findViewById(R.id.image);
        namaFile = findViewById(R.id.namaFile);
        pathFile = findViewById(R.id.pathFile);

        btnPickImage = findViewById(R.id.btnPickImage);
        btnPickImage.setOnClickListener(this);

        btnCamera = findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(this);

        btnPickAudio = findViewById(R.id.btnPickAudio);
        btnPickAudio.setOnClickListener(this);

        btnPickVideo = findViewById(R.id.btnPickVideo);
        btnPickVideo.setOnClickListener(this);

        btnPickFile = findViewById(R.id.btnPickFile);
        btnPickFile.setOnClickListener(this);

        checkPermissions();
    }

    @Override
    public void onClick(View v) {
        if (v == btnPickImage) {
            pickFile("image/*");
        } else if (v == btnPickAudio) {
            pickFile("audio/*");
        } else if (v == btnPickVideo) {
            pickFile("video/*");
        } else if (v == btnPickFile) {
            pickFile("*/*");
        } else if (v == btnCamera) {
            takePhoto();
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO,
                                Manifest.permission.CAMERA},
                        REQUEST_CODE_PERMISSIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA},
                        REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    private void pickFile(String type) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(type);
        filePickerLauncher.launch(intent);
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void takePhoto() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // kalau tanpa simpan file
//        cameraLauncher.launch(cameraIntent);

        // kalau dengan simpan file
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                cameraLauncherWithFile.launch(cameraIntent);
            }
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        try {
            photoFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return photoFile;
    }

    private void scanFile(String path) {
        Log.d("INFO", path);
        MediaScannerConnection.scanFile(this, new String[]{path}, null,
                (scannedPath, uri) -> {
                    runOnUiThread(() -> {
                        // Menampilkan gambar di ImageView setelah pemindaian selesai
                        image.setImageURI(Uri.parse(scannedPath));
                    });
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Izin diperlukan untuk melanjutkan", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }
}