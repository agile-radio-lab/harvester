package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.app.Activity
import androidx.room.Room
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.view.*
import android.widget.*
import androidx.preference.PreferenceManager
import com.android.volley.AuthFailureError
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.jetbrains.anko.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SessionAdapter(private val context: Context,
                    val dataSource: ArrayList<Session>) : BaseAdapter() {
    var toExport: ArrayList<Session> = ArrayList()
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = inflater.inflate(R.layout.session_item, parent, false)
        val titleTextView = rowView.findViewById(R.id.lvSessionID) as TextView
        val cbSelect = rowView.findViewById(R.id.cbSelect) as CheckBox
        val ivSaved = rowView.findViewById(R.id.ivSaved) as ImageView
        val ivUploaded = rowView.findViewById(R.id.ivUploaded) as ImageView
        val ivSync = rowView.findViewById(R.id.ivSync) as ImageView
        val sess = getItem(position) as Session

        titleTextView.text = sess.sessionID
        cbSelect.isChecked = toExport.contains(sess)
        if (sess.exportedStatus!!.and(1) == 1) {
            ivSaved.visibility = View.VISIBLE
        }
        if (sess.exportedStatus!!.and(2) == 2) {
            ivUploaded.visibility = View.VISIBLE
        }
        if (sess.exportedStatus!!.and(4) == 4) {
            ivSync.visibility = View.VISIBLE
        }
        titleTextView.setOnClickListener {
            val selectedSession = dataSource[position]
            val detailIntent = MapPopupActivity.newIntent(context, selectedSession, toExport)
            (context as Activity).startActivityForResult(detailIntent, 1)
        }
        cbSelect.setOnClickListener {
            if (cbSelect.isChecked) {
                toExport.add(sess)
            } else {
                toExport.remove(sess)
            }
        }
        return rowView
    }
}

class SecondActivity : AppCompatActivity() {
    var sessionList: ArrayList<Session> = ArrayList()
    private lateinit var listView: ListView
    private lateinit var db: AppDatabase
    private lateinit var adapter: SessionAdapter
    private var savedExportedSessions: ArrayList<Session> = ArrayList()
    var context: Context? = null
    var savePath: String = ""
    var savePathPing: String = ""
    var errorCode: Int? = null
    var uploadResponse: String? = null
    private var uploadUrl: String? = null
    private var apiToken: String? = null
    private var requestQueue: RequestQueue? = null
    private var uploadFileId: String? = null
    private var uploadAfterSave = false

    internal inner class GetSessionsFromDB : Runnable {
        override fun run() {
            val listSessionList = db.measurementPointDao().getAllSessions()
            sessionList = ArrayList(listSessionList)
        }
    }

    internal inner class RemoveSessionsFromDB : Runnable {
        override fun run() {
            val sessionIDs = Array(adapter.toExport.size) {
                adapter.toExport[it].sessionID!!
            }
            db.measurementPointDao().deleteBySessionID(sessionIDs)
        }
    }

    internal inner class UploadFile : Runnable {
        override fun run() {
            val sessionIDs = Array(adapter.toExport.size) {
                adapter.toExport[it].sessionID!!
            }

            val url = URL("$uploadUrl/files/$uploadFileId/fs")
            val conn = Multipart(url, apiToken!!)

            val file = File(savePath)
            conn.addFilePart("file", file, file.name, "text/csv")
            conn.upload(object : Multipart.OnFileUploadedListener{
                override fun onFileUploadingSuccess(response: String) {
                    uploadResponse = response
                    db.measurementPointDao().orExportStatusBySessionID(2, sessionIDs)
                }
                override fun onFileUploadingFailed(responseCode: Int) {
                    errorCode = responseCode
                }
            })

            val filePing = File(savePath)
            conn.addFilePart("file", filePing, filePing.name, "text/csv")
            conn.upload(object : Multipart.OnFileUploadedListener{
                override fun onFileUploadingSuccess(response: String) {
                    uploadResponse = response
                    db.pingResultDao().orExportStatusBySessionID(2, sessionIDs)
                }
                override fun onFileUploadingFailed(responseCode: Int) {
                    errorCode = responseCode
                }
            })
        }
    }

    internal inner class SaveMeasurementsFromDB : Runnable {
        override fun run() {
            Log.i("Measurementsaver", "STARTED")
            val sessionIDs = Array(adapter.toExport.size) {
                adapter.toExport[it].sessionID!!
            }

            val exportDir = File(context?.getExternalFilesDir(null)?.absolutePath.toString(), "")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val formatter = SimpleDateFormat("YYYY-MM-dd_HH-mm-ss", Locale.US)
            val saveFilename = "hfapp2_${formatter.format(Date())}.csv"

            val exportDir = context!!.getExternalFilesDir(null)
            val file = File(exportDir, saveFilename)

            if (!file.exists()) {
                file.createNewFile()
            }

            savePath = file.absolutePath
            val fileWriter = FileWriter(file.absoluteFile)
            fileWriter.append(CSV_HEADER)
            fileWriter.append('\n')
            val res = db.measurementPointDao().getAllBySessionID(sessionIDs)
            for (r in res) {
                fileWriter.append(r.toCSVRow())
                fileWriter.append('\n')
            }
            db.measurementPointDao().orExportStatusBySessionID(1, sessionIDs)
            Log.i("Measurement saver", "Saved to ${file.absolutePath}")
            fileWriter.flush()
            fileWriter.close()


            val saveFilenamePing = "hfapp2_${formatter.format(Date())}_ping.csv"
            val resPing = db.pingResultDao().getAllBySessionID(sessionIDs)
            if (resPing.count() > 0) {
                val filePing = File(exportDir, saveFilenamePing)
                if (!filePing.exists()) {
                    filePing.createNewFile()
                }
                savePathPing = filePing.absolutePath
                val fileWriterPing = FileWriter(filePing.absoluteFile)
                fileWriterPing.append(CSV_HEADER_PING)
                fileWriterPing.append('\n')
                for (r in resPing) {
                    fileWriterPing.append(r.toCSVRow())
                    fileWriterPing.append('\n')
                }
                db.pingResultDao().orExportStatusBySessionID(1, sessionIDs)
                Log.i("Ping results saver", "Saved to ${filePing.absolutePath}")
                fileWriterPing.flush()
                fileWriterPing.close()
            }
        }
    }

    companion object {
        const val EXPORTED_SESSIONS = "exportedSessions"

        fun newIntent(context: Context, exportedSessions: ArrayList<Session>): Intent {
            val detailIntent = Intent(context, SecondActivity::class.java)
            detailIntent.putExtra(EXPORTED_SESSIONS, exportedSessions)
            return detailIntent
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_WRITE_EXTERNAL -> {
                Toast.makeText(this, "Try to save again", Toast.LENGTH_LONG).show()
                return
            }
            else -> { }
        }
    }

    private fun getSessions() {
        val t = Thread(GetSessionsFromDB())
        t.start()
        t.join()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        requestQueue = Volley.newRequestQueue(this)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "measurements"
        ).build()
        listView = findViewById(R.id.lvSessions)
        context = this
    }

    override fun onResume() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        uploadUrl = prefs.getString("file_server_url", getString(R.string.upload_url))
        apiToken = prefs.getString("api_token", "test")

        getSessions()
        val listItems = arrayOfNulls<String>(sessionList.size)
        for (i in 0 until sessionList.size) {
            val recipe = sessionList[i]
            listItems[i] = recipe.sessionID
        }
        adapter = SessionAdapter(this, sessionList)
        adapter.toExport = savedExportedSessions
        if (intent.extras != null) {
            val exportedSessions  = intent.extras?.getParcelableArrayList<Session>(EXPORTED_SESSIONS)
            if (exportedSessions != null) {
                adapter.toExport = exportedSessions
            }
        }
        listView.adapter = adapter
        super.onResume()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when {
            item.itemId == R.id.save -> {
                if (adapter.toExport.size == 0) {
                    Toast.makeText(this, "Nothing to save", Toast.LENGTH_LONG).show()
                    return false
                }
                uploadAfterSave = false
                saveMeasurement()
            }
            item.itemId == R.id.upload -> {
                if (adapter.toExport.size == 0) {
                    Toast.makeText(this, "Nothing to upload", Toast.LENGTH_LONG).show()
                    return false
                }
                uploadAfterSave = true
                saveMeasurement()
            }
            item.itemId == R.id.delete -> {
                if (adapter.toExport.size == 0) {
                    Toast.makeText(this, "Nothing to remove", Toast.LENGTH_LONG).show()
                    return false
                }
                val builder: AlertDialog.Builder? = this.let {
                    AlertDialog.Builder(this)
                }
                builder?.setMessage(R.string.dialog_message)?.setTitle(R.string.dialog_title)

                builder?.apply {
                    setPositiveButton("Yes"
                    ) { _, _ ->
                        deleteMeasurement()
                    }
                    setNegativeButton("No"
                    ) { _, _ ->
                    }
                }
                val dialog: AlertDialog? = builder?.create()
                dialog?.show()
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                savedExportedSessions = data.getParcelableArrayListExtra<Session>(EXPORTED_SESSIONS) as ArrayList<Session>
            }
        }
    }

    private fun updateView(newStatus: Int = 0) {
        for (e in adapter.toExport) {
            if (newStatus > 0) {
                val currentStatus = adapter.dataSource[adapter.dataSource.indexOf(e)].exportedStatus
                adapter.dataSource[adapter.dataSource.indexOf(e)].exportedStatus = currentStatus?.or(newStatus)
            } else if (newStatus == -1) {
                adapter.dataSource.remove(e)
            }
        }
        if (newStatus == -1) {
            adapter.toExport = ArrayList()
        }
        adapter.notifyDataSetChanged()
    }

    private fun deleteMeasurement() {
        val t = Thread(RemoveSessionsFromDB())
        t.start()
        t.join()

        Toast.makeText(context, "Removed", Toast.LENGTH_LONG).show()
        updateView(-1)
    }

    private fun saveMeasurement() {
        Log.i("Measurement saver","GO into fun")
        if (!hasPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            getPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_WRITE_EXTERNAL)
            return
        }
        doAsync {
            savePath = ""
            savePathPing = ""
            val t = Thread(SaveMeasurementsFromDB())
            t.start()
            t.join()

            uiThread {
                Toast.makeText(context, "Saved to $savePath", Toast.LENGTH_LONG).show()
                updateView(1)
                if (uploadAfterSave) {
                    uploadMeasurement()
                }
            }
        }

    }

    private fun createUploadObject(filePath: String): JSONObject {
        val file = File(savePath)
        val rootObject= JSONObject()
        rootObject.put("name",file.name)
        return rootObject
    }

    private fun uploadMeasurement() {
        if (savePath == "") {
            Toast.makeText(context, "File to upload was not saved", Toast.LENGTH_LONG).show()
            return
        }

        val arr = JSONArray()
        arr.put(createUploadObject(savePath))
        if (savePathPing != "") {
            arr.put(createUploadObject(savePathPing))
        }

        val jsonArrayRequest: JsonArrayRequest = object : JsonArrayRequest(
            Method.POST, "$uploadUrl/files", arr,
            Response.Listener  { response ->
                uploadFileId = response.getJSONObject(0).getString("id")
                errorCode = null
                uploadResponse = null

                doAsync {
                    val t = Thread(UploadFile())
                    t.start()
                    t.join()
                    uiThread {
                        if (errorCode != null) {
                            Toast.makeText(context, "File upload failure $errorCode", Toast.LENGTH_LONG).show()
                        }
                        if (uploadResponse != null) {
                            Toast.makeText(context, "File uploaded $savePath", Toast.LENGTH_LONG).show()
                            updateView(2)
                        }
                    }
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show()
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["X-API-Key"] =  apiToken!!
                return params
            }
        }
        requestQueue?.add(jsonArrayRequest)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.map_menu, menu)
        return true
    }
//    override fun onStop() {
//        super.onStop()
//        db.close()
//    }
}
