package stan.androiddemo.project.Mito

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget
import com.bumptech.glide.request.target.Target
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.maning.imagebrowserlibrary.MNImageBrowser
import kotlinx.android.synthetic.main.activity_image_set.*
import stan.androiddemo.Model.ResultInfo
import stan.androiddemo.R
import stan.androiddemo.project.Mito.Model.ImageSetInfo
import java.io.File
import kotlin.Exception

class ImageSetActivity : AppCompatActivity() {

    lateinit var imageSet:ImageSetInfo
    var arrImageUrl = ArrayList<String>()
    lateinit var mAdapter:BaseQuickAdapter<String,BaseViewHolder>
    lateinit var failView: View
    lateinit var loadingView: View
    var currentUrl = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_set)
        title = ""
        toolbar.setNavigationOnClickListener { onBackPressed() }
        imageSet = intent.getParcelableExtra<ImageSetInfo>("set")
        txt_toolbar_title.text = imageSet.title
        mAdapter = object:BaseQuickAdapter<String,BaseViewHolder>(R.layout.mito_image_solo_item,arrImageUrl){
            override fun convert(helper: BaseViewHolder, item: String) {
                val imgDownload = helper.getView<ImageView>(R.id.img_download)
                Glide.with(this@ImageSetActivity).load(item).crossFade().listener(object:RequestListener<String,GlideDrawable>{
                    override fun onException(e: Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                        imgDownload.visibility = View.VISIBLE
                        return false
                    }

                }).into(helper.getView(R.id.img_mito))


                imgDownload.setOnClickListener {
                    if (ContextCompat.checkSelfPermission(this@ImageSetActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(this@ImageSetActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),1)
                        currentUrl = item
                        return@setOnClickListener
                    }
                    downloadImg(item)
                }
            }
        }

        mAdapter.setOnItemClickListener { adapter, view, position ->
            MNImageBrowser.showImageBrowser(this,view,position,arrImageUrl)
        }

        loadingView = View.inflate(this,R.layout.list_loading_hint,null)
        failView = View.inflate(this,R.layout.list_empty_hint,null)
        failView.setOnClickListener {
            mAdapter.emptyView = loadingView
            getImages()
        }
        recycler_view_images.layoutManager = LinearLayoutManager(this)
        recycler_view_images.adapter = mAdapter
        getImages()
    }

    fun getImages(){
        ImageSetInfo.imageSet(imageSet,{ v: ResultInfo ->
            runOnUiThread {
                if (v.code != 0) {
                    Toast.makeText(this,v.message, Toast.LENGTH_LONG).show()
                    mAdapter.emptyView = failView
                    return@runOnUiThread
                }
                val imageSets = v.data!! as ImageSetInfo
                if (imageSets.images.size <= 0){
                    mAdapter.emptyView = failView
                    return@runOnUiThread
                }
                arrImageUrl.addAll(imageSets.images)
                mAdapter.notifyDataSetChanged()
            }
        })
    }

    fun downloadImg(url:String){
        object:AsyncTask<String,Int,File?>(){
            override fun doInBackground(vararg p0: String?): File? {
                var file:File? = null
                try {
                    val future = Glide.with(this@ImageSetActivity).load(url).downloadOnly(Target.SIZE_ORIGINAL,Target.SIZE_ORIGINAL)
                    file = future.get()
                    val pictureFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absoluteFile
                    val fileDir = File(pictureFolder,"Mito")
                    if (!fileDir.exists()){
                        fileDir.mkdir()
                    }
                    val fileName = System.currentTimeMillis().toString() + ".jpg"
                    val destFile = File(fileDir,fileName)
                    file.copyTo(destFile)
                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(File(destFile.path))))
                }
                catch (e:Exception){
                    e.printStackTrace()
                }
                return file
            }

            override fun onPreExecute() {
                Toast.makeText(this@ImageSetActivity,"保存图片成功",Toast.LENGTH_LONG).show()
            }

        }.execute()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            1->{
                if (grantResults.size> 0&&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    downloadImg(currentUrl)
                }
                else{
                    Toast.makeText(this@ImageSetActivity,"你没有允许保存文件",Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}
