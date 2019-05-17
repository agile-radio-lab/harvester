package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.app.Activity
import android.arch.persistence.room.Room
import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.*
import android.widget.*
import java.io.File
import java.io.FileWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.security.auth.callback.Callback
import kotlin.collections.ArrayList

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
        val sess = getItem(position) as Session

        titleTextView.text = sess.sessionID
        cbSelect.isChecked = toExport.contains(sess)
        if (sess.exportedStatus!!.and(1) == 1) {
            ivSaved.visibility = View.VISIBLE
        }
        if (sess.exportedStatus!!.and(2) == 2) {
            ivUploaded.visibility = View.VISIBLE
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
    var errorCode: Int? = null
    var uploadResponse: String? = null

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
            val url = URL(getString(R.string.upload_url) + "upload")
            val conn = Multipart(url)
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
        }
    }

    internal inner class SaveMeasurementsFromDB : Runnable {
        override fun run() {
            val sessionIDs = Array(adapter.toExport.size) {
                adapter.toExport[it].sessionID!!
            }
            val exportDir =  File(Environment.getExternalStorageDirectory(), "")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val formatter = SimpleDateFormat("YYYY-MM-dd_HH-mm-ss")
            val saveFilename = "hfapp2_${formatter.format(Date())}.csv"

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

    private fun getSessions() {
        val t = Thread(GetSessionsFromDB())
        t.start()
        t.join()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "measurements"
        ).build()
        listView = findViewById(R.id.lvSessions)
        context = this
    }

    override fun onResume() {
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
                saveMeasurement()
            }
            item.itemId == R.id.upload -> {
                if (adapter.toExport.size == 0) {
                    Toast.makeText(this, "Nothing to upload", Toast.LENGTH_LONG).show()
                    return false
                }
                saveMeasurement()
                uploadMeasurement()
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
                    ) { dialog, id ->
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
                savedExportedSessions = data.getParcelableArrayListExtra<Session>(EXPORTED_SESSIONS)
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
        savePath = ""
        val t = Thread(SaveMeasurementsFromDB())
        t.start()
        t.join()

        Toast.makeText(context, "Saved to $savePath", Toast.LENGTH_LONG).show()
        updateView(1)
    }

    private fun uploadMeasurement() {
        if (savePath == "") {
            Toast.makeText(context, "File to upload was not saved", Toast.LENGTH_LONG).show()
            return
        }
        errorCode = null
        uploadResponse = null
        val t = Thread(UploadFile())
        t.start()
        t.join()

        if (errorCode != null) {
            Toast.makeText(context, "File upload failure $errorCode", Toast.LENGTH_LONG).show()
        }
        if (uploadResponse != null) {
            Toast.makeText(context, "File uploaded $savePath", Toast.LENGTH_LONG).show()
            updateView(2)
        }
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
