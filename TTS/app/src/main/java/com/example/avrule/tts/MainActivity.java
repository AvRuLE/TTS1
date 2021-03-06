package com.example.avrule.tts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.net.URI;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
//import java.util.List;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Locale;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
//import android.content.Intent;
import android.content.pm.PackageManager;
//import android.content.pm.ResolveInfo;
//import android.graphics.Bitmap;
//import android.graphics.Camera;
//import android.net.Uri;
//import android.nfc.Tag;
import android.os.Environment;
//import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
//import android.support.v4.content.FileProvider;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
//import android.widget.ImageView;
//import android.widget.TextView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;
import okhttp3.OkHttpClient;

import java.io.File;
import java.util.List;


//import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
//import static android.widget.Toast.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    //static final int REQUEST_IMAGE_CAPTURE = 1;
    private TextToSpeech tts;
    private Button btnSpeak;
    private EditText txtText;
    HashMap<String,Integer> map;
    HashMap<Integer, String> rmap;
    int [] carr;
    private android.hardware.Camera mCamera;
    private CameraPreview mPreview;
    File pictureFile;
    @Nullable
    private ClarifaiClient client;
    private android.hardware.Camera.PictureCallback mPicture = new android.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
            Toast.makeText(MainActivity.this,"Inside OnPictureTaken",Toast.LENGTH_SHORT).show();
            pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

            if(pictureFile == null){
                Log.d("pictureFile","Error creating media file, check storage permissions:");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e){
                Log.d("FNFE","filenotfound: "+e.getMessage());
            }catch (IOException e){
                Log.d("IOE","IOE: "+e.getMessage());
            }
            String msg = "Releasing camera";
            //Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show();
            //releaseCamera();
            msg = "Starting next preview";
            Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show();
            camera.stopPreview();
            Thread network = new Thread(new Runnable() {
                @Override
                public void run() {
                    String ResultString = "";
                    String TestString = "";
                    final List<ClarifaiOutput<Concept>> predictionResults = client.getDefaultModels().generalModel().predict().
                            withInputs(ClarifaiInput.forImage(new File(pictureFile.getAbsolutePath())))
                            .executeSync()
                            .get();
                    System.out.println("Number of predictions is: "+ predictionResults.size());
                    if(predictionResults!=null && predictionResults.size()>0){
                        ListIterator<ClarifaiOutput<Concept>> itr= predictionResults.listIterator();
                        int k=1;
                        while(itr.hasNext()){
                            System.out.println("In iterator number: "+(k));
                            ClarifaiOutput<Concept> output = itr.next();
                            //System.out.println("Model Name "+output.model().name()
                            //+"\nlist "+(k++)+" size: "+output.data().size()
                            //);
                            List<Concept> concepts = output.data();
                            if(concepts != null && concepts.size() > 0){
                                for (int j = 0; j < concepts.size(); j++) {
                                    Concept concept = concepts.get(j);
                                    float cval=0;
                                    //System.out.println("Name: "+concept.name()+" Value: "+concept.value());
                                    try {
                                        cval = concept.value();
                                        System.out.println("value of concept: "+cval);
                                    } catch (NullPointerException e)
                                    {
                                        //System.out.println("No value of concept");
                                    }

                                    if(cval > 0.9){
                                        try {

                                            ResultString += " "+ProcessText(concept.name());
                                            TestString += " "+concept.name();
                                            //System.out.println("detected: "+concept.name());
                                        }catch (NullPointerException e){
                                            //System.out.println("No value of Name");
                                        }

                                    }
                                }
                            }
                        }
                    }
                    System.out.println("TestString:: "+TestString);
                    System.out.println("ResultString:: "+ResultString);
                    Arrays.fill(carr,0);
                    speak(ResultString);
                }
            });
            network.start();
            try {
                network.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //predict(pictureFile.getAbsolutePath());
            camera.startPreview();
        }
    };

    private String ProcessText(String string){
        if(map.containsKey(string)){
            int val = map.get(string);
            if(carr[val-1]==0)
            {
                carr[val-1]++;
                return rmap.get(val);
            }
        }
        return "";
    }


    /*public void predict(String ImageUrl) {
        final List<ClarifaiOutput<Concept>> predictionResults = client.getDefaultModels().generalModel().predict().
                            withInputs(ClarifaiInput.forImage(new File(ImageUrl)))
                            .executeSync()
                            .get();
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Example of a call to a native method
        /*TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());*/

        //create an instance of Camera
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            int requestCode = 100;
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA}, requestCode);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            int requestCode = 100;
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            int requestCode = 100;
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_DENIED) {
            int requestCode = 100;
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.INTERNET}, requestCode);
        }
        setContentView(R.layout.activity_main);
        map = new HashMap<String, Integer>();
        addItemstoStiHash();
        rmap = new HashMap<Integer, String>();
        addItemstoItsHash();
        carr = new int[rmap.size()];
        Arrays.fill(carr,0);
        //client = new ClarifaiBuilder(getString(R.string.CLIENT_ID),getString(R.string.CLIENT_SECRET))
        client = new ClarifaiBuilder(getString(R.string.CLARIFAI_API_KEY))
                .client(new OkHttpClient())
                .buildSync();
        Toast.makeText(MainActivity.this,"Before ButtionClick",Toast.LENGTH_SHORT).show();
        //onButtonclicklistener();
        startPreview();
    }

    private void addItemstoStiHash()
    {
        //For person and similar queries 1;
        map.put("person",1);
        map.put("people",1);
        map.put("man",1);
        map.put("male",1);
        map.put("female",1);
        map.put("women",1);
        map.put("men",1);
        map.put("woman",1);
        map.put("boy",1);
        map.put("girl",1);
        map.put("child",1);
        map.put("adult",1);

        map.put("dog",2);
        map.put("canine",2);
        map.put("pet",2);
        map.put("animal",2);
        map.put("puppy",2);
        map.put("hound",2);
        map.put("wolf",2);
        map.put("fox",2);

        map.put("car",3);
        map.put("vehicle",3);
        map.put("transportation system",3);
        map.put("automotive",3);
        map.put("drive",3);
        map.put("sedan",3);
        map.put("traffic",3);
        map.put("truck",3);
        map.put("asphalt",3);
        map.put("bus",3);

        map.put("road",4);
        map.put("expressway",4);
        map.put("lane",4);
        map.put("highway",4);

        map.put("cattle",5);
        map.put("mammal",5);
        map.put("cow",5);
        map.put("bull",5);
        map.put("livestock",5);
        map.put("horn",5);
        map.put("beef cattle",5);
        map.put("calf",5);

        map.put("step",6);
        map.put("handrail",6);
        map.put("ladder",6);
        map.put("upstairs",6);
        map.put("steps",6);

        map.put("engine",7);
        map.put("locomotive",7);
        map.put("train",7);
        map.put("railway",7);
        map.put("wagon",7);

        map.put("railroad track",8);
        map.put("track",8);


    }

    private void addItemstoItsHash()
    {
        rmap.put(1,"Human ahead");
        rmap.put(2,"Dog infront");
        rmap.put(3,"vehicle ahead");
        rmap.put(4,"road be careful");
        rmap.put(5,"Bull infront");
        rmap.put(6,"Staircase infront careful");
        rmap.put(7,"Train ahead");
        rmap.put(8,"Rail track");

    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

    /*
    USED FOR THUMBNAIL

    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data){
        ImageView mImageView = (ImageView) findViewById(R.id.image1);
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);
        }
    }
    */
    /*
    //FOR FULL SIZE IMAGE
    static final int REQUEST_TAKE_PHOTO = 1;
    String mCurrentPhotoPath;

    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager())!=null){
            File photoFile = null;
            try{
                photoFile = createImageFile();
            } catch (IOException ex) {
                //Error occured while creating the File
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,"com.example.android.fileprovider",photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                Toast.makeText(getApplicationContext(),"it happened", LENGTH_LONG).show();
            }
        }
    }


    private  File createImageFile() throws Exception
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_"+timeStamp+"_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,".jpg",storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    */

    //Check if device has camera
    private  boolean checkCameraHardware(Context context){
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            //this device has camera
            return true;
        }else {
            //no camera on this device
            return false;
        }
    }

    public  static android.hardware.Camera getCameraInstance(){
        android.hardware.Camera c = null;
        try {
            c = android.hardware.Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            //Camera is not available
        }
        return c;
    }


    private void startPreview(){
        System.out.println("before getcamerainstance");
        mCamera = getCameraInstance();

        //Create our Preview view and set it as the content of our activity
        mPreview = null;
        System.out.println("before CameraPreview");
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = findViewById(R.id.camera_preview);
        System.out.println("before addView");
        preview.addView(mPreview);
        System.out.println("after addView");
        tts = new TextToSpeech(this, this);
        Thread thread = new Thread(){
            @Override
            public void run(){
                super.run();
                while (true){
                    try {

                        mCamera.takePicture(null,null,mPicture);
                        sleep(5000);
                    }
                    catch (Exception e){

                    }
                }
            }
        };
        thread.start();
    }

    private void releaseCamera(){
        if(mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

    private  static File getOutputMediaFile(int type){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "TTS");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("TTS", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
            //Do something
            tts = new TextToSpeech(this, this);
            mCamera.takePicture(null,null,mPicture);
        }
        return true;
    }

    /*public void onButtonclicklistener(){
        tts = new TextToSpeech(this, this);
        btnSpeak = (Button) findViewById(R.id.button);
        txtText = (EditText) findViewById(R.id.edit);
        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //speak();
                mCamera.takePicture(null,null,mPicture);
                //dispatchTakePictureIntent();
            }
        });
    }*/

    @Override
    public void onDestroy(){
        if (tts!=null){
            tts.stop();
            tts.shutdown();
        }
        releaseCamera();
        super.onDestroy();
    }

    @Override
    public void onInit(int status){
        if(status == TextToSpeech.SUCCESS){
            int result = tts.setLanguage(Locale.ENGLISH);
            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Log.e("TTS","This is not supported");
            }
            else{
                //btnSpeak.setEnabled(true);
                //speak();
                Toast.makeText(MainActivity.this,"TTS Started successfully", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Log.e("TTS","Initialization failed");
        }
    }

    private void speak(String string){
        //CharSequence text = txtText.getText().toString();
        CharSequence text = (CharSequence)string;
        //tts.setSpeechRate(2);
        tts.speak(text ,TextToSpeech.QUEUE_FLUSH,null, null);
    }

    public native String stringFromJNI();
}
