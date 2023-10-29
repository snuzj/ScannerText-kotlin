@file:Suppress("DEPRECATION")

package com.snuzj.scannerapp

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snuzj.scannerapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private companion object {
        private const val CAMERA_REQUEST_CODE = 101
        private const val STORAGE_REQUEST_CODE = 102
    }
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>
    private lateinit var progressDialog: ProgressDialog
    private lateinit var textRecognizer: TextRecognizer

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Loading")
        progressDialog.setCanceledOnTouchOutside(false)

        cameraPermission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        binding.captureBtn.setOnClickListener {
            showInputImageDialog()
        }

        binding.recognizeTextBtn.setOnClickListener {
            if (imageUri == null){
                Toast.makeText(this,"Pick Image First...",Toast.LENGTH_SHORT).show()
            }
            else{
                recognizeTextFromImage()
            }
        }
    }

    private fun recognizeTextFromImage() {
        progressDialog.setMessage("Preparing Image...")
        progressDialog.show()

        try {
            val inputImage = InputImage.fromFilePath(this,imageUri!!)
            progressDialog.setMessage("Recognizing Text...")

            textRecognizer.process(inputImage)
                .addOnSuccessListener {text->
                    progressDialog.dismiss()

                    val recognizedText = text.text

                    binding.recognizeTextEt.setText(recognizedText)
                }
                .addOnFailureListener {e->
                    progressDialog.dismiss()
                    Toast.makeText(this,"Failed to recognize text ${e.message}",Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception){
            progressDialog.dismiss()
            Toast.makeText(this,"Failed to prepare image ${e.message}",Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInputImageDialog() {
        val popupMenu = PopupMenu(this,binding.captureBtn)

        popupMenu.menu.add(Menu.NONE,1,1,"CAMERA")
        popupMenu.menu.add(Menu.NONE,2,2,"GALLERY")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val id = menuItem.itemId
            if (id == 1){
                if(checkCameraPermission()){
                    pickImageCamera()
                }
                else{
                    requestCameraPermission()
                }
            } else if (id == 2){
                if(checkStoragePermission()){
                    pickImageGallery()
                }
                else{
                    requestStoragePermission()
                }
            }

            return@setOnMenuItemClickListener true
        }

    }

    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if (result.resultCode == Activity.RESULT_OK){
            val data = result.data
            imageUri = data!!.data
            binding.imageIv.setImageURI(imageUri)
        } else{
            Toast.makeText(this,"Cancelled...",Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkStoragePermission(): Boolean{
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storagePermission, STORAGE_REQUEST_CODE)
    }

    private fun pickImageCamera(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Image Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            binding.imageIv.setImageURI(imageUri)
        }
    }


    private fun checkCameraPermission(): Boolean{
        val cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        return cameraResult && storageResult
    }

    private fun requestCameraPermission(){
        ActivityCompat.requestPermissions(this,cameraPermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_REQUEST_CODE->{
                if (grantResults.isNotEmpty()){
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if (cameraAccepted && storageAccepted){
                        pickImageCamera()
                    } else{
                        Toast.makeText(this,"Camera and Storage permission are required",Toast.LENGTH_SHORT).show()
                    }
                }
            }
            STORAGE_REQUEST_CODE->{
                if (grantResults.isNotEmpty()){
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (storageAccepted){
                        pickImageGallery()
                    }
                    else{
                        Toast.makeText(this,"Storage permission is required",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


}